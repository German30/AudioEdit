package com.auralis.project.dto;

import com.auralis.project.domain.Clip;
import com.auralis.project.domain.EditVersion;
import com.auralis.project.domain.Effect;
import com.auralis.project.domain.EffectScope;
import com.auralis.project.domain.EffectType;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.ProjectMember;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.domain.Track;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class ProjectDtos {

    private ProjectDtos() {}

    // ---- project ----
    public record CreateProjectRequest(
            @NotBlank @Size(max = 140) String name,
            @Size(max = 1000) String description,
            Integer sampleRate,
            Double tempoBpm) {}

    public record UpdateProjectRequest(
            @Size(min = 1, max = 140) String name,
            @Size(max = 1000) String description,
            Double tempoBpm,
            Double masterGainDb,
            Boolean archived) {}

    public record ProjectResponse(String id, String ownerUserId, String name, String description,
                                  int sampleRate, Double tempoBpm, double masterGainDb, boolean archived,
                                  Instant lastOpenedAt, Instant createdAt, Instant updatedAt) {
        public static ProjectResponse of(Project p) {
            return new ProjectResponse(p.getId().toString(), p.getOwnerUserId().toString(), p.getName(),
                    p.getDescription(), p.getSampleRate(), p.getTempoBpm(), p.getMasterGainDb(), p.isArchived(),
                    p.getLastOpenedAt(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    // ---- members ----
    public record UpsertMemberRequest(@NotNull UUID userId, @NotNull ProjectRole role) {}

    public record MemberResponse(String userId, String role, Instant addedAt) {
        public static MemberResponse of(ProjectMember m) {
            return new MemberResponse(m.getUserId().toString(), m.getRole().name(), m.getCreatedAt());
        }
    }

    // ---- tracks ----
    public record CreateTrackRequest(@Size(max = 120) String name, @Size(max = 9) String color) {}

    public record UpdateTrackRequest(String name, Double gainDb, Double pan, Boolean muted, Boolean soloed,
                                     String color) {}

    public record ReorderTracksRequest(@NotNull java.util.List<UUID> trackIds) {}

    public record TrackResponse(String id, String name, int position, double gainDb, double pan,
                                boolean muted, boolean soloed, String color) {
        public static TrackResponse of(Track t) {
            return new TrackResponse(t.getId().toString(), t.getName(), t.getPosition(), t.getGainDb(),
                    t.getPan(), t.isMuted(), t.isSoloed(), t.getColor());
        }
    }

    // ---- clips ----
    public record CreateClipRequest(
            @NotNull UUID trackId,
            @NotNull UUID sourceAssetId,
            String name,
            Double timelineStartSeconds,
            Double sourceOffsetSeconds,
            Double durationSeconds,
            Double gainDb,
            Double fadeInSeconds,
            Double fadeOutSeconds,
            Double playbackRate,
            Boolean reversed) {}

    public record UpdateClipRequest(
            UUID trackId, String name, Double timelineStartSeconds, Double sourceOffsetSeconds,
            Double durationSeconds, Double gainDb, Double fadeInSeconds, Double fadeOutSeconds,
            Double playbackRate, Boolean reversed) {}

    public record SplitClipRequest(@NotNull Double atTimelineSeconds) {}

    public record ClipResponse(String id, String trackId, String sourceAssetId, String name,
                               double timelineStartSeconds, double sourceOffsetSeconds, double durationSeconds,
                               double gainDb, double fadeInSeconds, double fadeOutSeconds,
                               double playbackRate, boolean reversed) {
        public static ClipResponse of(Clip c) {
            return new ClipResponse(c.getId().toString(), c.getTrackId().toString(),
                    c.getSourceAssetId().toString(), c.getName(), c.getTimelineStartSeconds(),
                    c.getSourceOffsetSeconds(), c.getDurationSeconds(), c.getGainDb(), c.getFadeInSeconds(),
                    c.getFadeOutSeconds(), c.getPlaybackRate(), c.isReversed());
        }
    }

    // ---- effects ----
    public record CreateEffectRequest(
            @NotNull EffectScope scope,
            UUID clipId,
            UUID trackId,
            @NotNull EffectType type,
            Boolean enabled,
            Map<String, Object> params) {}

    public record UpdateEffectRequest(Boolean enabled, Integer position, Map<String, Object> params) {}

    public record EffectResponse(String id, String scope, String clipId, String trackId, String type,
                                 boolean enabled, int position, Map<String, Object> params) {
        public static EffectResponse of(Effect e) {
            return new EffectResponse(e.getId().toString(), e.getScope().name(),
                    e.getClipId() == null ? null : e.getClipId().toString(),
                    e.getTrackId() == null ? null : e.getTrackId().toString(),
                    e.getType().name(), e.isEnabled(), e.getPosition(), e.getParams());
        }
    }

    // ---- versions ----
    public record SnapshotRequest(@Size(max = 200) String label) {}

    public record VersionResponse(String id, long revision, String label, String authorUserId, Instant createdAt) {
        public static VersionResponse of(EditVersion v) {
            return new VersionResponse(v.getId().toString(), v.getRevision(), v.getLabel(),
                    v.getAuthorUserId().toString(), v.getCreatedAt());
        }
    }
}
