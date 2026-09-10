package com.auralis.project;

import com.auralis.common.error.ApiException;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.repo.ProjectMemberRepository;
import com.auralis.project.repo.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Resolves and enforces a caller's {@link ProjectRole} on a project. The owner
 * always has {@code OWNER}; everyone else is looked up in {@code project_members}.
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project"));
    }

    @Transactional(readOnly = true)
    public ProjectRole roleOf(UUID userId, Project project) {
        if (project.getOwnerUserId().equals(userId)) {
            return ProjectRole.OWNER;
        }
        return memberRepository.findByProjectIdAndUserId(project.getId(), userId)
                .map(m -> m.getRole())
                .orElseThrow(() -> ApiException.notFound("Project"));
    }

    @Transactional(readOnly = true)
    public Project requireRole(UUID userId, UUID projectId, ProjectRole minimum) {
        Project project = requireProject(projectId);
        ProjectRole role = roleOf(userId, project);
        if (!role.atLeast(minimum)) {
            throw ApiException.forbidden("Requires " + minimum + " on this project");
        }
        return project;
    }
}
