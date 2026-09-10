package com.auralis.asset;

import com.auralis.asset.domain.AssetStatus;
import com.auralis.asset.domain.AudioAsset;
import com.auralis.asset.repo.AudioAssetRepository;
import com.auralis.common.error.ApiException;
import com.auralis.storage.StorageKeys;
import com.auralis.storage.StorageService;
import com.auralis.subscription.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AudioAssetRepository assetRepository;
    private final StorageService storage;
    private final QuotaService quotaService;
    private final AssetAnalyzer analyzer;
    /** Empty unless another module contributes a checker (e.g. project clips). */
    private final List<AssetUsageChecker> usageCheckers;

    @Transactional
    public InitiateUploadResult initiateUpload(UUID userId, String filename, String contentType, long declaredSizeBytes) {
        if (contentType == null || !(contentType.startsWith("audio/") || contentType.equals("application/ogg"))) {
            throw ApiException.badRequest("contentType must be an audio/* MIME type");
        }
        if (declaredSizeBytes <= 0) {
            throw ApiException.badRequest("sizeBytes must be positive");
        }
        quotaService.assertUploadAllowed(userId, declaredSizeBytes);

        AudioAsset asset = new AudioAsset();
        asset.setOwnerUserId(userId);
        asset.setOriginalFilename(StorageKeys.sanitize(filename));
        asset.setContentType(contentType);
        asset.setSizeBytes(declaredSizeBytes);
        asset.setObjectKey(StorageKeys.sourceAsset(userId, asset.getId(), asset.getOriginalFilename()));
        asset.setStatus(AssetStatus.PENDING_UPLOAD);
        asset = assetRepository.save(asset);

        StorageService.PresignedUpload upload = storage.presignUpload(asset.getObjectKey(), contentType);
        return new InitiateUploadResult(asset, upload);
    }

    @Transactional
    public AudioAsset completeUpload(UUID userId, UUID assetId) {
        AudioAsset asset = requireOwned(userId, assetId);
        if (asset.getStatus() != AssetStatus.PENDING_UPLOAD) {
            return asset; // idempotent-ish: already progressed
        }
        long actualSize = storage.size(asset.getObjectKey());
        if (actualSize < 0) {
            throw ApiException.badRequest("No uploaded object found for this asset yet");
        }
        quotaService.assertStorageAvailable(userId, actualSize);

        asset.setSizeBytes(actualSize);
        asset.setStatus(AssetStatus.PROCESSING);
        quotaService.addStorageUsage(userId, actualSize);

        UUID id = asset.getId();
        runAfterCommit(() -> analyzer.analyze(id));
        return asset;
    }

    @Transactional(readOnly = true)
    public Page<AudioAsset> list(UUID userId, Pageable pageable) {
        return assetRepository.findByOwnerUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public AudioAsset get(UUID userId, UUID assetId) {
        return requireOwned(userId, assetId);
    }

    @Transactional(readOnly = true)
    public StorageService.PresignedDownload download(UUID userId, UUID assetId) {
        AudioAsset asset = requireOwned(userId, assetId);
        if (asset.getStatus() == AssetStatus.PENDING_UPLOAD) {
            throw ApiException.conflict("Asset upload has not completed");
        }
        return storage.presignDownload(asset.getObjectKey(), asset.getOriginalFilename());
    }

    @Transactional
    public void delete(UUID userId, UUID assetId) {
        AudioAsset asset = requireOwned(userId, assetId);
        for (AssetUsageChecker checker : usageCheckers) {
            if (checker.isAssetInUse(assetId)) {
                throw ApiException.conflict("Cannot delete asset: " + checker.describeUsage(assetId));
            }
        }
        safeDelete(asset.getObjectKey());
        if (asset.getWaveformKey() != null) safeDelete(asset.getWaveformKey());
        if (asset.getStatus() != AssetStatus.PENDING_UPLOAD) {
            quotaService.addStorageUsage(userId, -asset.getSizeBytes());
        }
        assetRepository.delete(asset);
    }

    private AudioAsset requireOwned(UUID userId, UUID assetId) {
        return assetRepository.findByIdAndOwnerUserId(assetId, userId)
                .orElseThrow(() -> ApiException.notFound("Asset"));
    }

    private void safeDelete(String key) {
        try {
            storage.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete object {}", key, e);
        }
    }

    private static void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    public record InitiateUploadResult(AudioAsset asset, StorageService.PresignedUpload upload) {}
}
