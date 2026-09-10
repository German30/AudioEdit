package com.auralis.project;

import com.auralis.common.error.ApiException;
import com.auralis.project.domain.EditVersion;
import com.auralis.project.domain.Effect;
import com.auralis.project.domain.EffectScope;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.domain.Track;
import com.auralis.project.graph.GraphAssembler;
import com.auralis.project.graph.ProjectGraph;
import com.auralis.project.repo.ClipRepository;
import com.auralis.project.repo.EditVersionRepository;
import com.auralis.project.repo.EffectRepository;
import com.auralis.project.repo.ProjectRepository;
import com.auralis.project.repo.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Named snapshots of the edit graph: history checkpoints and the frozen input a
 * render job runs against.
 */
@Service
@RequiredArgsConstructor
public class VersionService {

    private final EditVersionRepository versionRepository;
    private final ProjectRepository projectRepository;
    private final TrackRepository trackRepository;
    private final ClipRepository clipRepository;
    private final EffectRepository effectRepository;
    private final GraphAssembler graphAssembler;
    private final ProjectAccessService access;

    @Transactional
    public EditVersion snapshot(UUID userId, UUID projectId, String label) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        return capture(userId, projectId, label);
    }

    /**
     * Snapshot without the EDITOR check — for callers (render) that only need
     * VIEWER access and have already verified it.
     */
    @Transactional
    public EditVersion captureForRender(UUID userId, UUID projectId, String label) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return capture(userId, projectId, label);
    }

    private EditVersion capture(UUID userId, UUID projectId, String label) {
        ProjectGraph graph = graphAssembler.assemble(projectId);

        long nextRevision = versionRepository.findFirstByProjectIdOrderByRevisionDesc(projectId)
                .map(v -> v.getRevision() + 1).orElse(1L);

        EditVersion version = new EditVersion();
        version.setProjectId(projectId);
        version.setAuthorUserId(userId);
        version.setLabel(label);
        version.setRevision(nextRevision);
        version.setGraphJson(graphAssembler.toJson(graph));
        return versionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public Page<EditVersion> list(UUID userId, UUID projectId, Pageable pageable) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return versionRepository.findByProjectIdOrderByRevisionDesc(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public EditVersion get(UUID userId, UUID projectId, UUID versionId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return versionRepository.findByIdAndProjectId(versionId, projectId)
                .orElseThrow(() -> ApiException.notFound("Version"));
    }

    @Transactional(readOnly = true)
    public ProjectGraph graphOf(UUID userId, UUID projectId, UUID versionId) {
        EditVersion v = get(userId, projectId, versionId);
        return graphAssembler.fromJson(v.getGraphJson());
    }

    /**
     * Replace the project's live arrangement with the one captured in a version.
     * A fresh snapshot of the pre-restore state is taken first so nothing is lost.
     */
    @Transactional
    public EditVersion restore(UUID userId, UUID projectId, UUID versionId) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        EditVersion source = versionRepository.findByIdAndProjectId(versionId, projectId)
                .orElseThrow(() -> ApiException.notFound("Version"));
        snapshot(userId, projectId, "Auto-backup before restoring r" + source.getRevision());

        ProjectGraph graph = graphAssembler.fromJson(source.getGraphJson());
        Project project = projectRepository.findById(projectId).orElseThrow();

        effectRepository.deleteByProjectId(projectId);
        clipRepository.deleteByProjectId(projectId);
        trackRepository.deleteByProjectId(projectId);

        project.setSampleRate(graph.sampleRate());
        project.setTempoBpm(graph.tempoBpm());
        project.setMasterGainDb(graph.masterGainDb());

        Map<UUID, UUID> trackIdRemap = new LinkedHashMap<>();
        for (ProjectGraph.TrackNode tn : graph.tracks()) {
            Track track = new Track();
            track.setProjectId(projectId);
            track.setName(tn.name());
            track.setPosition(tn.position());
            track.setGainDb(tn.gainDb());
            track.setPan(tn.pan());
            track.setMuted(tn.muted());
            track.setSoloed(tn.soloed());
            track = trackRepository.save(track);
            trackIdRemap.put(tn.id(), track.getId());

            for (ProjectGraph.ClipNode cn : tn.clips()) {
                var clip = new com.auralis.project.domain.Clip();
                clip.setProjectId(projectId);
                clip.setTrackId(track.getId());
                clip.setSourceAssetId(cn.sourceAssetId());
                clip.setName(cn.assetFilename());
                clip.setTimelineStartSeconds(cn.timelineStartSeconds());
                clip.setSourceOffsetSeconds(cn.sourceOffsetSeconds());
                clip.setDurationSeconds(cn.durationSeconds());
                clip.setGainDb(cn.gainDb());
                clip.setFadeInSeconds(cn.fadeInSeconds());
                clip.setFadeOutSeconds(cn.fadeOutSeconds());
                clip.setPlaybackRate(cn.playbackRate());
                clip.setReversed(cn.reversed());
                clip = clipRepository.save(clip);
                persistEffects(projectId, EffectScope.CLIP, clip.getId(), null, cn.effects());
            }
            persistEffects(projectId, EffectScope.TRACK, null, track.getId(), tn.effects());
        }
        persistEffects(projectId, EffectScope.MASTER, null, null, graph.masterEffects());

        return snapshot(userId, projectId, "Restored from r" + source.getRevision());
    }

    private void persistEffects(UUID projectId, EffectScope scope, UUID clipId, UUID trackId,
                                java.util.List<ProjectGraph.EffectNode> nodes) {
        int pos = 0;
        for (ProjectGraph.EffectNode node : nodes) {
            Effect effect = new Effect();
            effect.setProjectId(projectId);
            effect.setScope(scope);
            effect.setClipId(clipId);
            effect.setTrackId(trackId);
            effect.setType(node.type());
            effect.setEnabled(node.enabled());
            effect.setPosition(pos++);
            if (node.params() != null) effect.getParams().putAll(node.params());
            effectRepository.save(effect);
        }
    }
}
