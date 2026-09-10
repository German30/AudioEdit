package com.auralis.asset.domain;

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

/**
 * A source audio file uploaded by a user. Immutable audio-wise once
 * {@code READY}; projects reference it from clips but never mutate it.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "audio_assets", indexes = {
        @Index(name = "ix_assets_owner", columnList = "owner_user_id"),
        @Index(name = "ix_assets_status", columnList = "status")
})
public class AudioAsset extends BaseEntity {

    @Column(name = "owner_user_id", nullable = false, length = 36)
    private UUID ownerUserId;

    @Column(nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "object_key", nullable = false, length = 512)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssetStatus status = AssetStatus.PENDING_UPLOAD;

    // ---- populated by analysis ----
    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @Column(name = "sample_rate")
    private Integer sampleRate;

    @Column
    private Integer channels;

    @Column(length = 40)
    private String codec;

    @Column(name = "bit_rate_kbps")
    private Integer bitRateKbps;

    @Column(name = "waveform_key", length = 512)
    private String waveformKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
