package com.auralis.render;

import com.auralis.common.error.ApiException;
import com.auralis.project.ProjectAccessService;
import com.auralis.project.VersionService;
import com.auralis.project.domain.EditVersion;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.graph.GraphAssembler;
import com.auralis.render.domain.RenderJob;
import com.auralis.render.domain.RenderStatus;
import com.auralis.render.engine.RenderEncoders;
import com.auralis.render.repo.RenderJobRepository;
import com.auralis.subscription.QuotaService;
import com.auralis.subscription.QuotaService.RenderRequestLimits;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RenderService {

    private final RenderJobRepository jobRepository;
    private final ProjectAccessService access;
    private final VersionService versionService;
    private final GraphAssembler graphAssembler;
    private final QuotaService quotaService;
    private final com.auralis.storage.StorageService storage;

    @Transactional
    public RenderJob enqueue(UUID userId, UUID projectId, RenderRequest req) {
        Project project = access.requireRole(userId, projectId, ProjectRole.VIEWER);

        String format = RenderEncoders.normalize(req.format());
        int bitrate = req.bitrateKbps() != null && req.bitrateKbps() > 0 ? req.bitrateKbps() : 256;
        int sampleRate = req.sampleRate() != null && req.sampleRate() > 0 ? req.sampleRate() : project.getSampleRate();
        boolean stems = Boolean.TRUE.equals(req.includeStems());

        EditVersion frozen = versionService.captureForRender(userId, projectId,
                "Render " + format + " @ " + Instant.now());
        double estimatedSeconds = graphAssembler.fromJson(frozen.getGraphJson()).totalDurationSeconds();
        if (estimatedSeconds <= 0) {
            throw ApiException.unprocessable("empty_project", "Nothing to render: the project has no audible clips");
        }

        quotaService.assertRenderAllowed(userId,
                new RenderRequestLimits((long) Math.ceil(estimatedSeconds), format, bitrate, stems));

        RenderJob job = new RenderJob();
        job.setProjectId(projectId);
        job.setEditVersionId(frozen.getId());
        job.setRequestedByUserId(userId);
        job.setStatus(RenderStatus.QUEUED);
        job.setFormat(format);
        job.setBitrateKbps(bitrate);
        job.setSampleRate(sampleRate);
        job.setIncludeStems(stems);
        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Page<RenderJob> list(UUID userId, UUID projectId, Pageable pageable) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return jobRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public RenderJob get(UUID userId, UUID projectId, UUID jobId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return jobRepository.findByIdAndProjectId(jobId, projectId)
                .orElseThrow(() -> ApiException.notFound("Render job"));
    }

    @Transactional
    public RenderJob cancel(UUID userId, UUID projectId, UUID jobId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        RenderJob job = jobRepository.findByIdAndProjectId(jobId, projectId)
                .orElseThrow(() -> ApiException.notFound("Render job"));
        if (job.getStatus().isTerminal()) {
            throw ApiException.conflict("Job already " + job.getStatus());
        }
        if (job.getStatus() == RenderStatus.QUEUED) {
            job.setStatus(RenderStatus.CANCELED);
            job.setFinishedAt(Instant.now());
        } else {
            job.setCancelRequested(true); // RUNNING: worker will honour it
        }
        return job;
    }

    @Transactional(readOnly = true)
    public com.auralis.storage.StorageService.PresignedDownload downloadUrl(
            UUID userId, UUID projectId, UUID jobId, boolean stems) {
        RenderJob job = get(userId, projectId, jobId);
        if (job.getStatus() != RenderStatus.SUCCEEDED) {
            throw ApiException.conflict("Render is not finished (" + job.getStatus() + ")");
        }
        String key = stems ? job.getStemsObjectKey() : job.getOutputObjectKey();
        if (key == null) {
            throw ApiException.notFound(stems ? "Stems archive" : "Render output");
        }
        String ext = RenderEncoders.extension(job.getFormat());
        String filename = stems ? "stems-" + jobId + ".zip" : "render-" + jobId + "." + ext;
        return storage.presignDownload(key, filename);
    }

    public record RenderRequest(String format, Integer bitrateKbps, Integer sampleRate, Boolean includeStems) {}
}
