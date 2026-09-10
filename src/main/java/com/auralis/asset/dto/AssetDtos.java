package com.auralis.asset.dto;

import com.auralis.asset.AssetService.InitiateUploadResult;
import com.auralis.asset.domain.AudioAsset;
import com.auralis.storage.StorageService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.Map;

public final class AssetDtos {

    private AssetDtos() {}

    public record InitiateUploadRequest(
            @NotBlank String filename,
            @NotBlank String contentType,
            @Positive long sizeBytes) {}

    public record PresignedUploadView(String url, String method, Map<String, String> headers, Instant expiresAt) {
        static PresignedUploadView of(StorageService.PresignedUpload u) {
            return new PresignedUploadView(u.url(), u.method(), u.headers(), u.expiresAt());
        }
    }

    public record InitiateUploadResponse(String assetId, String objectKey, PresignedUploadView upload) {
        public static InitiateUploadResponse of(InitiateUploadResult r) {
            return new InitiateUploadResponse(
                    r.asset().getId().toString(),
                    r.asset().getObjectKey(),
                    PresignedUploadView.of(r.upload()));
        }
    }

    public record AssetResponse(
            String id, String originalFilename, String contentType, long sizeBytes, String status,
            Double durationSeconds, Integer sampleRate, Integer channels, String codec, Integer bitRateKbps,
            boolean hasWaveform, String failureReason, Instant createdAt) {

        public static AssetResponse of(AudioAsset a) {
            return new AssetResponse(
                    a.getId().toString(), a.getOriginalFilename(), a.getContentType(), a.getSizeBytes(),
                    a.getStatus().name(), a.getDurationSeconds(), a.getSampleRate(), a.getChannels(),
                    a.getCodec(), a.getBitRateKbps(), a.getWaveformKey() != null, a.getFailureReason(),
                    a.getCreatedAt());
        }
    }

    public record DownloadUrlResponse(String url, Instant expiresAt) {}
}
