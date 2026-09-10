package com.auralis.project.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "project_members", indexes = {
        @Index(name = "ux_project_member", columnList = "project_id,user_id", unique = true),
        @Index(name = "ix_project_member_user", columnList = "user_id")
})
public class ProjectMember extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private UUID projectId;

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProjectRole role = ProjectRole.VIEWER;

    public ProjectMember(UUID projectId, UUID userId, ProjectRole role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }
}
