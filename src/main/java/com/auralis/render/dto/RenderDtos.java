package com.auralis.render.dto;

import com.auralis.render.domain.RenderJob;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public final class RenderDtos {

    private RenderDtos() {}

    public record CreateRenderRequest(
            String format,
            @Min(64) @Max(320) Integer bitrateKbps,
            @Min(8000) @Max(192000) Integer sampleRate,
            Boolean includeStems) {}

    public record RenderJobResponse(
            String id, String projectId, String editVersionId, String status,
            String format, int bitrateKbps, int sampleRate, boolean includeStems,
            int progress, int attempts,
            Double renderedSeconds, Long billedSeconds, Long outputSizeBytes,
            boolean hasOutput, boolean hasStems, String errorMessage,
            Instant startedAt, Instant finishedAt, Instant createdAt) {

        public static RenderJobResponse of(RenderJob j) {
            return new RenderJobResponse(
                    j.getId().toString(), j.getProjectId().toString(), j.getEditVersionId().toString(),
                    j.getStatus().name(), j.getFormat(), j.getBitrateKbps(), j.getSampleRate(), j.isIncludeStems(),
                    j.getProgress(), j.getAttempts(), j.getRenderedSeconds(), j.getBilledSeconds(),
                    j.getOutputSizeBytes(), j.getOutputObjectKey() != null, j.getStemsObjectKey() != null,
                    j.getErrorMessage(), j.getStartedAt(), j.getFinishedAt(), j.getCreatedAt());
        }
    }

    public record DownloadUrlResponse(String url, Instant expiresAt) {}
}
