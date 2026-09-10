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

/**
 * A placement of (part of) an {@link com.auralis.asset.domain.AudioAsset}
 * on a track's timeline. All positions/durations are in seconds on the project
 * timeline; {@code sourceOffsetSeconds} is the in-point inside the source asset.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "clips", indexes = {
        @Index(name = "ix_clips_track", columnList = "track_id"),
        @Index(name = "ix_clips_asset", columnList = "source_asset_id")
})
public class Clip extends BaseEntity {

    @Column(name = "track_id", nullable = false, length = 36)
    private UUID trackId;

    @Column(name = "project_id", nullable = false, length = 36)
    private UUID projectId;

    @Column(name = "source_asset_id", nullable = false, length = 36)
    private UUID sourceAssetId;

    @Column(length = 140)
    private String name;

    @Column(name = "timeline_start_seconds", nullable = false)
    private double timelineStartSeconds;

    @Column(name = "source_offset_seconds", nullable = false)
    private double sourceOffsetSeconds = 0.0;

    /** Length on the timeline, before playback-rate scaling. */
    @Column(name = "duration_seconds", nullable = false)
    private double durationSeconds;

    @Column(name = "gain_db", nullable = false)
    private double gainDb = 0.0;

    @Column(name = "fade_in_seconds", nullable = false)
    private double fadeInSeconds = 0.0;

    @Column(name = "fade_out_seconds", nullable = false)
    private double fadeOutSeconds = 0.0;

    /** 1.0 = original speed. Carries over the legacy app's speed control (0.25–3.0). */
    @Column(name = "playback_rate", nullable = false)
    private double playbackRate = 1.0;

    /** Play the source backwards (legacy "Audio invertido"). */
    @Column(nullable = false)
    private boolean reversed = false;
}
