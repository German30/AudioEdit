package com.auralis.project.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "tracks", indexes = @Index(name = "ix_tracks_project", columnList = "project_id"))
public class Track extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private UUID projectId;

    @Column(nullable = false, length = 120)
    private String name;

    /** Vertical order in the arrangement, 0-based. */
    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "gain_db", nullable = false)
    private double gainDb = 0.0;

    /** -1.0 (hard left) .. 1.0 (hard right). */
    @Column(nullable = false)
    private double pan = 0.0;

    @Column(nullable = false)
    private boolean muted = false;

    @Column(nullable = false)
    private boolean soloed = false;

    @Column(length = 9)
    private String color;
}
