package com.auralis.project.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "projects", indexes = @Index(name = "ix_projects_owner", columnList = "owner_user_id"))
public class Project extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private UUID ownerUserId;

    @Column(nullable = false, length = 140)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Timeline/mix sample rate the project renders at. */
    @Column(name = "sample_rate", nullable = false)
    private int sampleRate = 48_000;

    @Column(name = "tempo_bpm")
    private Double tempoBpm;

    /** Master gain applied after the mix bus, in decibels. */
    @Column(name = "master_gain_db", nullable = false)
    private double masterGainDb = 0.0;

    @Column(nullable = false)
    private boolean archived = false;

    @Column(name = "last_opened_at")
    private Instant lastOpenedAt;
}
