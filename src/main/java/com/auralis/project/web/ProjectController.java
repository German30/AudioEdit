package com.auralis.project.web;

import com.auralis.auth.security.CurrentUser;
import com.auralis.common.web.PageResponse;
import com.auralis.project.ProjectService;
import com.auralis.project.dto.ProjectDtos.CreateProjectRequest;
import com.auralis.project.dto.ProjectDtos.MemberResponse;
import com.auralis.project.dto.ProjectDtos.ProjectResponse;
import com.auralis.project.dto.ProjectDtos.UpdateProjectRequest;
import com.auralis.project.dto.ProjectDtos.UpsertMemberRequest;
import com.auralis.project.graph.GraphAssembler;
import com.auralis.project.graph.ProjectGraph;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Projects")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final GraphAssembler graphAssembler;
    private final CurrentUser currentUser;

    @Operation(summary = "Create a project (owner is added as a member automatically)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest body) {
        return ProjectResponse.of(projectService.create(
                currentUser.id(), body.name(), body.description(), body.sampleRate(), body.tempoBpm()));
    }

    @Operation(summary = "List projects the caller owns or collaborates on")
    @GetMapping
    public PageResponse<ProjectResponse> list(Pageable pageable) {
        return PageResponse.from(projectService.list(currentUser.id(), pageable), ProjectResponse::of);
    }

    @Operation(summary = "Open a project (updates lastOpenedAt)")
    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable UUID projectId) {
        return ProjectResponse.of(projectService.open(currentUser.id(), projectId));
    }

    @Operation(summary = "Update project metadata / master settings")
    @PatchMapping("/{projectId}")
    public ProjectResponse update(@PathVariable UUID projectId, @Valid @RequestBody UpdateProjectRequest body) {
        return ProjectResponse.of(projectService.update(currentUser.id(), projectId, body.name(),
                body.description(), body.tempoBpm(), body.masterGainDb(), body.archived()));
    }

    @Operation(summary = "Delete a project and its whole arrangement (owner only)")
    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID projectId) {
        projectService.delete(currentUser.id(), projectId);
    }

    @Operation(summary = "The fully-resolved edit graph (arrangement + asset keys)")
    @GetMapping("/{projectId}/graph")
    public ProjectGraph graph(@PathVariable UUID projectId) {
        projectService.open(currentUser.id(), projectId); // access check
        return graphAssembler.assemble(projectId);
    }

    // ---- members ----

    @Operation(summary = "List project members")
    @GetMapping("/{projectId}/members")
    public List<MemberResponse> members(@PathVariable UUID projectId) {
        return projectService.members(currentUser.id(), projectId).stream().map(MemberResponse::of).toList();
    }

    @Operation(summary = "Add a member or change their role (owner only)")
    @PutMapping("/{projectId}/members")
    public MemberResponse upsertMember(@PathVariable UUID projectId, @Valid @RequestBody UpsertMemberRequest body) {
        return MemberResponse.of(projectService.addOrUpdateMember(
                currentUser.id(), projectId, body.userId(), body.role()));
    }

    @Operation(summary = "Remove a member (owner only)")
    @DeleteMapping("/{projectId}/members/{memberUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable UUID projectId, @PathVariable UUID memberUserId) {
        projectService.removeMember(currentUser.id(), projectId, memberUserId);
    }
}
