package com.auralis.project.web;

import com.auralis.auth.security.CurrentUser;
import com.auralis.common.web.PageResponse;
import com.auralis.project.VersionService;
import com.auralis.project.dto.ProjectDtos.SnapshotRequest;
import com.auralis.project.dto.ProjectDtos.VersionResponse;
import com.auralis.project.graph.ProjectGraph;
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

@Tag(name = "Versions")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;
    private final CurrentUser currentUser;

    @Operation(summary = "Capture a snapshot of the current edit graph")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse snapshot(@PathVariable UUID projectId, @Valid @RequestBody SnapshotRequest body) {
        return VersionResponse.of(versionService.snapshot(currentUser.id(), projectId, body.label()));
    }

    @Operation(summary = "List snapshots newest first")
    @GetMapping
    public PageResponse<VersionResponse> list(@PathVariable UUID projectId, Pageable pageable) {
        return PageResponse.from(versionService.list(currentUser.id(), projectId, pageable), VersionResponse::of);
    }

    @Operation(summary = "Get a snapshot's metadata")
    @GetMapping("/{versionId}")
    public VersionResponse get(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        return VersionResponse.of(versionService.get(currentUser.id(), projectId, versionId));
    }

    @Operation(summary = "Get the full edit graph captured in a snapshot")
    @GetMapping("/{versionId}/graph")
    public ProjectGraph graph(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        return versionService.graphOf(currentUser.id(), projectId, versionId);
    }

    @Operation(summary = "Restore the project's arrangement from a snapshot (auto-backs-up first)")
    @PostMapping("/{versionId}/restore")
    public VersionResponse restore(@PathVariable UUID projectId, @PathVariable UUID versionId) {
        return VersionResponse.of(versionService.restore(currentUser.id(), projectId, versionId));
    }
}
