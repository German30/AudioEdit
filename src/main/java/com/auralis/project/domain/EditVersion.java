package com.auralis.project.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * An immutable JSON snapshot of a project's full edit graph. Used for history,
 * "restore to this point", and as the frozen input to a render job.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "edit_versions", indexes = @Index(name = "ix_versions_project", columnList = "project_id,createdAt"))
public class EditVersion extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private UUID projectId;

    @Column(name = "author_user_id", nullable = false, length = 36)
    private UUID authorUserId;

    @Column(length = 200)
    private String label;

    /** Monotonic per-project revision number. */
    @Column(name = "revision", nullable = false)
    private long revision;

    @Lob
    @Column(name = "graph_json", nullable = false, columnDefinition = "longtext")
    private String graphJson;
}
