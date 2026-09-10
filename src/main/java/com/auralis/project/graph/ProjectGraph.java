package com.auralis.project.graph;

import com.auralis.project.domain.EffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical, self-contained description of everything needed to render a
 * project: the arrangement plus the storage keys of every referenced asset.
 * Assembled live from the entities or rehydrated from an {@code EditVersion}
 * snapshot, and consumed by the render module.
 */
public record ProjectGraph(
        UUID projectId,
        String name,
        int sampleRate,
        Double tempoBpm,
        double masterGainDb,
        List<TrackNode> tracks,
        List<EffectNode> masterEffects
) {

    public record TrackNode(
            UUID id,
            String name,
            int position,
            double gainDb,
            double pan,
            boolean muted,
            boolean soloed,
            List<ClipNode> clips,
            List<EffectNode> effects
    ) {}

    public record ClipNode(
            UUID id,
            UUID sourceAssetId,
            String assetObjectKey,
            String assetFilename,
            double timelineStartSeconds,
            double sourceOffsetSeconds,
            double durationSeconds,
            double gainDb,
            double fadeInSeconds,
            double fadeOutSeconds,
            double playbackRate,
            boolean reversed,
            List<EffectNode> effects
    ) {}

    public record EffectNode(
            UUID id,
            EffectType type,
            boolean enabled,
            Map<String, Object> params
    ) {}

    /** Latest timeline position any clip ends at, in seconds. */
    public double totalDurationSeconds() {
        double max = 0;
        for (TrackNode t : tracks) {
            for (ClipNode c : t.clips()) {
                double scaled = c.playbackRate() > 0 ? c.durationSeconds() : c.durationSeconds();
                max = Math.max(max, c.timelineStartSeconds() + scaled);
            }
        }
        return max;
    }

    public boolean hasAnySolo() {
        return tracks.stream().anyMatch(TrackNode::soloed);
    }
}
