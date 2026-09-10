package com.auralis.project;

import com.auralis.common.error.ApiException;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.ProjectMember;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.repo.ClipRepository;
import com.auralis.project.repo.EditVersionRepository;
import com.auralis.project.repo.EffectRepository;
import com.auralis.project.repo.ProjectMemberRepository;
import com.auralis.project.repo.ProjectRepository;
import com.auralis.project.repo.TrackRepository;
import com.auralis.subscription.QuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final TrackRepository trackRepository;
    private final ClipRepository clipRepository;
    private final EffectRepository effectRepository;
    private final EditVersionRepository versionRepository;
    private final ProjectAccessService access;
    private final QuotaService quotaService;

    @Transactional
    public Project create(UUID userId, String name, String description, Integer sampleRate, Double tempoBpm) {
        quotaService.assertCanCreateProject(userId, projectRepository.countByOwnerUserId(userId));

        Project project = new Project();
        project.setOwnerUserId(userId);
        project.setName(name);
        project.setDescription(description);
        if (sampleRate != null) project.setSampleRate(sampleRate);
        project.setTempoBpm(tempoBpm);
        project.setLastOpenedAt(Instant.now());
        project = projectRepository.save(project);

        memberRepository.save(new ProjectMember(project.getId(), userId, ProjectRole.OWNER));
        return project;
    }

    @Transactional(readOnly = true)
    public Page<Project> list(UUID userId, Pageable pageable) {
        return projectRepository.findAccessibleBy(userId, pageable);
    }

    @Transactional
    public Project open(UUID userId, UUID projectId) {
        Project project = access.requireRole(userId, projectId, ProjectRole.VIEWER);
        project.setLastOpenedAt(Instant.now());
        return project;
    }

    @Transactional
    public Project update(UUID userId, UUID projectId, String name, String description,
                          Double tempoBpm, Double masterGainDb, Boolean archived) {
        Project project = access.requireRole(userId, projectId, ProjectRole.EDITOR);
        if (name != null) project.setName(name);
        if (description != null) project.setDescription(description);
        if (tempoBpm != null) project.setTempoBpm(tempoBpm);
        if (masterGainDb != null) project.setMasterGainDb(masterGainDb);
        if (archived != null) project.setArchived(archived);
        return project;
    }

    @Transactional
    public void delete(UUID userId, UUID projectId) {
        access.requireRole(userId, projectId, ProjectRole.OWNER);
        effectRepository.deleteByProjectId(projectId);
        clipRepository.deleteByProjectId(projectId);
        trackRepository.deleteByProjectId(projectId);
        versionRepository.deleteByProjectId(projectId);
        memberRepository.deleteByProjectId(projectId);
        projectRepository.deleteById(projectId);
    }

    // ---- membership -------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjectMember> members(UUID userId, UUID projectId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return memberRepository.findByProjectId(projectId);
    }

    @Transactional
    public ProjectMember addOrUpdateMember(UUID actingUserId, UUID projectId, UUID memberUserId, ProjectRole role) {
        Project project = access.requireRole(actingUserId, projectId, ProjectRole.OWNER);
        if (memberUserId.equals(project.getOwnerUserId())) {
            throw ApiException.badRequest("The owner's role cannot be changed");
        }
        if (role == ProjectRole.OWNER) {
            throw ApiException.badRequest("Ownership transfer is not supported here");
        }
        ProjectMember existing = memberRepository.findByProjectIdAndUserId(projectId, memberUserId).orElse(null);
        if (existing == null) {
            // countByProjectId includes the OWNER row, so it already equals
            // (current collaborators) + 1 for the member we're about to add.
            quotaService.assertCollaboratorLimit(project.getOwnerUserId(),
                    memberRepository.countByProjectId(projectId));
            return memberRepository.save(new ProjectMember(projectId, memberUserId, role));
        }
        existing.setRole(role);
        return existing;
    }

    @Transactional
    public void removeMember(UUID actingUserId, UUID projectId, UUID memberUserId) {
        Project project = access.requireRole(actingUserId, projectId, ProjectRole.OWNER);
        if (memberUserId.equals(project.getOwnerUserId())) {
            throw ApiException.badRequest("The owner cannot be removed");
        }
        memberRepository.findByProjectIdAndUserId(projectId, memberUserId)
                .ifPresent(memberRepository::delete);
    }
}
