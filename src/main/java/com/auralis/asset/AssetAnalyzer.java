package com.auralis.asset;

import com.auralis.asset.domain.AssetStatus;
import com.auralis.asset.domain.AudioAsset;
import com.auralis.asset.repo.AudioAssetRepository;
import com.auralis.media.MediaInfo;
import com.auralis.media.MediaProperties;
import com.auralis.media.ProbeService;
import com.auralis.storage.StorageKeys;
import com.auralis.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Background probe + waveform generation for a freshly uploaded asset. Runs on
 * the {@code mediaExecutor} pool; failures land on the asset as
 * {@link AssetStatus#FAILED} with a reason.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssetAnalyzer {

    private final AudioAssetRepository assetRepository;
    private final StorageService storage;
    private final ProbeService probeService;
    private final WaveformService waveformService;
    private final MediaProperties mediaProperties;

    @Async("mediaExecutor")
    @Transactional
    public void analyze(UUID assetId) {
        AudioAsset asset = assetRepository.findById(assetId).orElse(null);
        if (asset == null || asset.getStatus() != AssetStatus.PROCESSING) {
            return;
        }

        Path workDir = mediaProperties.workDir();
        Path localSource = null;
        Path waveformFile = null;
        try {
            Files.createDirectories(workDir);
            localSource = workDir.resolve(assetId + "-" + StorageKeys.sanitize(asset.getOriginalFilename()));
            storage.download(asset.getObjectKey(), localSource);

            MediaInfo info = probeService.probe(localSource);
            asset.setDurationSeconds(info.durationSeconds());
            asset.setSampleRate(info.sampleRate());
            asset.setChannels(info.channels());
            asset.setCodec(info.codec());
            asset.setBitRateKbps(info.bitRateKbps());

            byte[] waveformJson = waveformService.generate(localSource);
            waveformFile = Files.createTempFile(workDir, "wf-", ".json");
            Files.write(waveformFile, waveformJson);
            String waveformKey = StorageKeys.assetWaveform(assetId);
            storage.upload(waveformKey, waveformFile, "application/json");
            asset.setWaveformKey(waveformKey);

            asset.setStatus(AssetStatus.READY);
            asset.setFailureReason(null);
            log.info("Asset {} analyzed: {}s, {}Hz, {}ch, {}", assetId,
                    String.format("%.1f", info.durationSeconds()), info.sampleRate(), info.channels(), info.codec());
        } catch (Exception e) {
            log.warn("Asset {} analysis failed", assetId, e);
            asset.setStatus(AssetStatus.FAILED);
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            asset.setFailureReason(reason.length() > 500 ? reason.substring(0, 500) : reason);
        } finally {
            deleteQuietly(localSource);
            deleteQuietly(waveformFile);
        }
    }

    private static void deleteQuietly(Path p) {
        if (p == null) return;
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
            // best effort
        }
    }
}
