package com.auralis.project.domain;

import com.auralis.common.domain.BaseEntity;
import com.auralis.common.domain.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A DSP effect in an insert chain. Exactly one of {@code clipId} / {@code trackId}
 * is set for {@code CLIP} / {@code TRACK} scope; both null for {@code MASTER}.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "effects", indexes = {
        @Index(name = "ix_effects_project", columnList = "project_id"),
        @Index(name = "ix_effects_clip", columnList = "clip_id"),
        @Index(name = "ix_effects_track", columnList = "track_id")
})
public class Effect extends BaseEntity {

    @Column(name = "project_id", nullable = false, length = 36)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EffectScope scope;

    @Column(name = "clip_id", length = 36)
    private UUID clipId;

    @Column(name = "track_id", length = 36)
    private UUID trackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EffectType type;

    /** Position in the insert chain for its scope target, 0-based. */
    @Column(name = "position", nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean enabled = true;

    @Lob
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "params_json", nullable = false, columnDefinition = "text")
    private Map<String, Object> params = new LinkedHashMap<>();
}
