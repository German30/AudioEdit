package com.auralis.render;

import com.auralis.project.repo.EditVersionRepository;
import com.auralis.render.domain.RenderJob;
import com.auralis.render.domain.RenderStatus;
import com.auralis.render.repo.RenderJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * The short, transactional DB touches around a render run. Kept in a separate
 * bean from {@link RenderExecutionService} so the {@code @Transactional}
 * boundaries are honoured (no self-invocation) and never wrap the ffmpeg call.
 */
@Component
@RequiredArgsConstructor
public class RenderJobStore {

    private final RenderJobRepository jobRepository;
    private final EditVersionRepository versionRepository;
    private final RenderProperties renderProps;

    public record JobContext(UUID projectId, UUID editVersionId, UUID requestedByUserId, String format,
                             int bitrateKbps, int sampleRate, boolean includeStems, String graphJson) {}

    @Transactional
    public Optional<JobContext> loadRunnableContext(UUID jobId) {
        RenderJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null || job.getStatus() != RenderStatus.RUNNING) {
            return Optional.empty();
        }
        String graphJson = versionRepository.findById(job.getEditVersionId())
                .map(v -> v.getGraphJson()).orElse(null);
        if (graphJson == null) {
            job.setStatus(RenderStatus.FAILED);
            job.setErrorMessage("Frozen edit version no longer exists");
            job.setFinishedAt(Instant.now());
            return Optional.empty();
        }
        return Optional.of(new JobContext(job.getProjectId(), job.getEditVersionId(), job.getRequestedByUserId(),
                job.getFormat(), job.getBitrateKbps(), job.getSampleRate(), job.isIncludeStems(), graphJson));
    }

    @Transactional
    public void markSucceeded(UUID jobId, String outputKey, long outputSize, String stemsKey,
                              double renderedSeconds, long billedSeconds) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(RenderStatus.SUCCEEDED);
            job.setOutputObjectKey(outputKey);
            job.setOutputSizeBytes(outputSize);
            job.setStemsObjectKey(stemsKey);
            job.setRenderedSeconds(renderedSeconds);
            job.setBilledSeconds(billedSeconds);
            job.setProgress(100);
            job.setFinishedAt(Instant.now());
            job.setErrorMessage(null);
        });
    }

    /** Move a failed job to CANCELED, FAILED (attempts exhausted) or back to QUEUED for retry. */
    @Transactional
    public void markFailedOrRetry(UUID jobId, String rawMessage) {
        RenderJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) return;
        String msg = rawMessage == null ? "unknown error" : rawMessage;
        if (msg.length() > 1000) msg = msg.substring(0, 1000);

        if (job.isCancelRequested()) {
            job.setStatus(RenderStatus.CANCELED);
            job.setFinishedAt(Instant.now());
        } else if (job.getAttempts() >= renderProps.maxAttempts()) {
            job.setStatus(RenderStatus.FAILED);
            job.setErrorMessage(msg);
            job.setFinishedAt(Instant.now());
        } else {
            job.setStatus(RenderStatus.QUEUED);
            job.setErrorMessage(msg);
            job.setStartedAt(null);
        }
    }
}
