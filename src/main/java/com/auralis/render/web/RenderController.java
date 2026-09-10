package com.auralis.render.web;

import com.auralis.auth.security.CurrentUser;
import com.auralis.common.web.PageResponse;
import com.auralis.render.RenderService;
import com.auralis.render.RenderService.RenderRequest;
import com.auralis.render.dto.RenderDtos.CreateRenderRequest;
import com.auralis.render.dto.RenderDtos.DownloadUrlResponse;
import com.auralis.render.dto.RenderDtos.RenderJobResponse;
import com.auralis.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Renders")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/renders")
@RequiredArgsConstructor
public class RenderController {

    private final RenderService renderService;
    private final CurrentUser currentUser;

    @Operation(summary = "Queue an export; snapshots the current graph and returns a QUEUED job")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public RenderJobResponse enqueue(@PathVariable UUID projectId, @Valid @RequestBody CreateRenderRequest body) {
        return RenderJobResponse.of(renderService.enqueue(currentUser.id(), projectId,
                new RenderRequest(body.format(), body.bitrateKbps(), body.sampleRate(), body.includeStems())));
    }

    @Operation(summary = "List render jobs for a project, newest first")
    @GetMapping
    public PageResponse<RenderJobResponse> list(@PathVariable UUID projectId, Pageable pageable) {
        return PageResponse.from(renderService.list(currentUser.id(), projectId, pageable), RenderJobResponse::of);
    }

    @Operation(summary = "Poll a render job's status")
    @GetMapping("/{jobId}")
    public RenderJobResponse get(@PathVariable UUID projectId, @PathVariable UUID jobId) {
        return RenderJobResponse.of(renderService.get(currentUser.id(), projectId, jobId));
    }

    @Operation(summary = "Cancel a queued or running render")
    @PostMapping("/{jobId}/cancel")
    public RenderJobResponse cancel(@PathVariable UUID projectId, @PathVariable UUID jobId) {
        return RenderJobResponse.of(renderService.cancel(currentUser.id(), projectId, jobId));
    }

    @Operation(summary = "Short-lived download URL for the finished mixdown")
    @GetMapping("/{jobId}/download-url")
    public DownloadUrlResponse downloadUrl(@PathVariable UUID projectId, @PathVariable UUID jobId) {
        StorageService.PresignedDownload d = renderService.downloadUrl(currentUser.id(), projectId, jobId, false);
        return new DownloadUrlResponse(d.url(), d.expiresAt());
    }

    @Operation(summary = "Short-lived download URL for the stems archive (if requested)")
    @GetMapping("/{jobId}/stems-url")
    public DownloadUrlResponse stemsUrl(@PathVariable UUID projectId, @PathVariable UUID jobId) {
        StorageService.PresignedDownload d = renderService.downloadUrl(currentUser.id(), projectId, jobId, true);
        return new DownloadUrlResponse(d.url(), d.expiresAt());
    }
}
