package com.auralis.render.domain;

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

import java.time.Instant;
import java.util.UUID;

/**
 * One export request. Runs against a frozen {@code EditVersion} so edits made
 * after enqueuing don't change the result.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "render_jobs", indexes = {
        @Index(name = "ix_render_project", columnList = "project_id,createdAt"),
        @Index(name = "ix_render_status", columnList = "status,createdAt")
})
public class RenderJob extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private UUID projectId;

    @Column(name = "edit_version_id", nullable = false, length = 36)
    private UUID editVersionId;

    @Column(name = "requested_by_user_id", nullable = false, length = 36)
    private UUID requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RenderStatus status = RenderStatus.QUEUED;

    @Column(nullable = false, length = 8)
    private String format = "mp3";

    @Column(name = "bitrate_kbps", nullable = false)
    private int bitrateKbps = 256;

    @Column(name = "sample_rate", nullable = false)
    private int sampleRate = 48_000;

    @Column(name = "include_stems", nullable = false)
    private boolean includeStems = false;

    @Column(name = "output_object_key", length = 512)
    private String outputObjectKey;

    @Column(name = "stems_object_key", length = 512)
    private String stemsObjectKey;

    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    /** Length of the rendered audio, seconds. */
    @Column(name = "rendered_seconds")
    private Double renderedSeconds;

    /** Whole seconds billed against the monthly render quota. */
    @Column(name = "billed_seconds")
    private Long billedSeconds;

    @Column(nullable = false)
    private int progress = 0;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested = false;
}
