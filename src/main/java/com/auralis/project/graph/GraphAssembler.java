package com.auralis.project.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.auralis.asset.domain.AssetStatus;
import com.auralis.asset.domain.AudioAsset;
import com.auralis.asset.repo.AudioAssetRepository;
import com.auralis.common.error.ApiException;
import com.auralis.project.domain.Clip;
import com.auralis.project.domain.Effect;
import com.auralis.project.domain.EffectScope;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.Track;
import com.auralis.project.repo.ClipRepository;
import com.auralis.project.repo.EffectRepository;
import com.auralis.project.repo.ProjectRepository;
import com.auralis.project.repo.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns the live project entities into a {@link ProjectGraph} (and back to/from
 * JSON for {@code EditVersion} snapshots). Fails fast if a referenced asset is
 * missing or still processing.
 */
@Component
@RequiredArgsConstructor
public class GraphAssembler {

    private final ProjectRepository projectRepository;
    private final TrackRepository trackRepository;
    private final ClipRepository clipRepository;
    private final EffectRepository effectRepository;
    private final AudioAssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ProjectGraph assemble(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ApiException.notFound("Project"));

        List<Track> tracks = trackRepository.findByProjectIdOrderByPositionAsc(projectId);
        List<Clip> clips = clipRepository.findByProjectIdOrderByTimelineStartSecondsAsc(projectId);
        List<Effect> effects = effectRepository.findByProjectIdOrderByPositionAsc(projectId);

        Map<UUID, List<Clip>> clipsByTrack = clips.stream().collect(Collectors.groupingBy(Clip::getTrackId));
        Map<UUID, List<Effect>> fxByClip = effects.stream()
                .filter(e -> e.getScope() == EffectScope.CLIP && e.getClipId() != null)
                .collect(Collectors.groupingBy(Effect::getClipId));
        Map<UUID, List<Effect>> fxByTrack = effects.stream()
                .filter(e -> e.getScope() == EffectScope.TRACK && e.getTrackId() != null)
                .collect(Collectors.groupingBy(Effect::getTrackId));
        List<ProjectGraph.EffectNode> masterFx = effects.stream()
                .filter(e -> e.getScope() == EffectScope.MASTER)
                .sorted(Comparator.comparingInt(Effect::getPosition))
                .map(this::toEffectNode)
                .toList();

        Map<UUID, AudioAsset> assetsById = resolveAssets(clips);

        List<ProjectGraph.TrackNode> trackNodes = tracks.stream().map(track -> {
            List<ProjectGraph.ClipNode> clipNodes = clipsByTrack.getOrDefault(track.getId(), List.of()).stream()
                    .sorted(Comparator.comparingDouble(Clip::getTimelineStartSeconds))
                    .map(clip -> toClipNode(clip, assetsById.get(clip.getSourceAssetId()),
                            sortedNodes(fxByClip.get(clip.getId()))))
                    .toList();
            return new ProjectGraph.TrackNode(
                    track.getId(), track.getName(), track.getPosition(), track.getGainDb(), track.getPan(),
                    track.isMuted(), track.isSoloed(), clipNodes, sortedNodes(fxByTrack.get(track.getId())));
        }).toList();

        return new ProjectGraph(
                project.getId(), project.getName(), project.getSampleRate(), project.getTempoBpm(),
                project.getMasterGainDb(), trackNodes, masterFx);
    }

    public String toJson(ProjectGraph graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize project graph", e);
        }
    }

    public ProjectGraph fromJson(String json) {
        try {
            return objectMapper.readValue(json, ProjectGraph.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not deserialize project graph", e);
        }
    }

    private Map<UUID, AudioAsset> resolveAssets(List<Clip> clips) {
        Set<UUID> ids = clips.stream().map(Clip::getSourceAssetId).collect(Collectors.toSet());
        Map<UUID, AudioAsset> byId = assetRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(AudioAsset::getId, Function.identity()));
        for (UUID id : ids) {
            AudioAsset a = byId.get(id);
            if (a == null) {
                throw ApiException.unprocessable("asset_missing", "Referenced asset " + id + " no longer exists");
            }
            if (a.getStatus() != AssetStatus.READY) {
                throw ApiException.unprocessable("asset_not_ready",
                        "Asset " + a.getOriginalFilename() + " is " + a.getStatus());
            }
        }
        return byId;
    }

    private List<ProjectGraph.EffectNode> sortedNodes(List<Effect> effects) {
        if (effects == null || effects.isEmpty()) return List.of();
        return effects.stream()
                .sorted(Comparator.comparingInt(Effect::getPosition))
                .map(this::toEffectNode)
                .toList();
    }

    private ProjectGraph.EffectNode toEffectNode(Effect e) {
        return new ProjectGraph.EffectNode(e.getId(), e.getType(), e.isEnabled(), e.getParams());
    }

    private ProjectGraph.ClipNode toClipNode(Clip c, AudioAsset asset, List<ProjectGraph.EffectNode> fx) {
        return new ProjectGraph.ClipNode(
                c.getId(), c.getSourceAssetId(), asset.getObjectKey(), asset.getOriginalFilename(),
                c.getTimelineStartSeconds(), c.getSourceOffsetSeconds(), c.getDurationSeconds(),
                c.getGainDb(), c.getFadeInSeconds(), c.getFadeOutSeconds(),
                c.getPlaybackRate(), c.isReversed(), fx);
    }
}
