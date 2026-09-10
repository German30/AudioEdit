package com.auralis.render;

import com.auralis.render.domain.RenderJob;
import com.auralis.render.domain.RenderStatus;
import com.auralis.render.repo.RenderJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Polls the queue and hands jobs to the render executor. Claiming is atomic
 * (see {@link RenderJobRepository#claim}) so multiple app instances are safe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RenderWorker {

    private final RenderJobRepository jobRepository;
    private final RenderExecutionService executionService;
    private final ThreadPoolTaskExecutor renderExecutor;

    @Scheduled(fixedDelayString = "${auralis.render.poll-interval:PT5S}")
    public void poll() {
        int freeSlots = renderExecutor.getMaxPoolSize() - renderExecutor.getActiveCount();
        if (freeSlots <= 0) {
            return;
        }
        var candidates = jobRepository.findTop20ByStatusOrderByCreatedAtAsc(RenderStatus.QUEUED);
        for (RenderJob candidate : candidates) {
            if (freeSlots <= 0) break;
            if (jobRepository.claim(candidate.getId(), Instant.now()) == 1) {
                freeSlots--;
                renderExecutor.execute(() -> safeRun(candidate.getId()));
            }
        }
    }

    /** Recover jobs left RUNNING by a crashed worker. */
    @Scheduled(fixedDelay = 120_000)
    public void requeueStale() {
        int n = jobRepository.requeueStale(Instant.now().minus(Duration.ofMinutes(30)));
        if (n > 0) log.warn("Re-queued {} stale RUNNING render job(s)", n);
    }

    private void safeRun(java.util.UUID jobId) {
        try {
            executionService.execute(jobId);
        } catch (Exception e) {
            log.error("Render worker crashed on job {}", jobId, e);
        }
    }
}
