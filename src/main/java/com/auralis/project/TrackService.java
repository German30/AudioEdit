package com.auralis.project;

import com.auralis.common.error.ApiException;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.domain.Track;
import com.auralis.project.repo.ClipRepository;
import com.auralis.project.repo.EffectRepository;
import com.auralis.project.repo.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackRepository trackRepository;
    private final ClipRepository clipRepository;
    private final EffectRepository effectRepository;
    private final ProjectAccessService access;

    @Transactional(readOnly = true)
    public List<Track> list(UUID userId, UUID projectId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return trackRepository.findByProjectIdOrderByPositionAsc(projectId);
    }

    @Transactional
    public Track add(UUID userId, UUID projectId, String name, String color) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Track track = new Track();
        track.setProjectId(projectId);
        track.setName(name == null || name.isBlank() ? "Track " + (trackRepository.countByProjectId(projectId) + 1) : name);
        track.setColor(color);
        track.setPosition((int) trackRepository.countByProjectId(projectId));
        return trackRepository.save(track);
    }

    @Transactional
    public Track update(UUID userId, UUID projectId, UUID trackId, TrackPatch patch) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Track track = require(projectId, trackId);
        if (patch.name() != null) track.setName(patch.name());
        if (patch.gainDb() != null) track.setGainDb(patch.gainDb());
        if (patch.pan() != null) track.setPan(clamp(patch.pan(), -1, 1));
        if (patch.muted() != null) track.setMuted(patch.muted());
        if (patch.soloed() != null) track.setSoloed(patch.soloed());
        if (patch.color() != null) track.setColor(patch.color());
        return track;
    }

    @Transactional
    public void delete(UUID userId, UUID projectId, UUID trackId) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Track track = require(projectId, trackId);
        effectRepository.deleteByTrackId(trackId);
        clipRepository.findByTrackIdOrderByTimelineStartSecondsAsc(trackId)
                .forEach(c -> effectRepository.deleteByClipId(c.getId()));
        clipRepository.deleteByTrackId(trackId);
        trackRepository.delete(track);
        renumber(projectId);
    }

    @Transactional
    public List<Track> reorder(UUID userId, UUID projectId, List<UUID> orderedTrackIds) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        List<Track> tracks = trackRepository.findByProjectIdOrderByPositionAsc(projectId);
        if (orderedTrackIds.size() != tracks.size()) {
            throw ApiException.badRequest("Reorder list must contain every track exactly once");
        }
        var byId = new java.util.HashMap<UUID, Track>();
        tracks.forEach(t -> byId.put(t.getId(), t));
        int pos = 0;
        List<Track> result = new ArrayList<>();
        for (UUID id : orderedTrackIds) {
            Track t = byId.get(id);
            if (t == null) throw ApiException.badRequest("Unknown track in reorder list: " + id);
            t.setPosition(pos++);
            result.add(t);
        }
        return result;
    }

    private Track require(UUID projectId, UUID trackId) {
        return trackRepository.findByIdAndProjectId(trackId, projectId)
                .orElseThrow(() -> ApiException.notFound("Track"));
    }

    private void renumber(UUID projectId) {
        List<Track> tracks = trackRepository.findByProjectIdOrderByPositionAsc(projectId);
        for (int i = 0; i < tracks.size(); i++) tracks.get(i).setPosition(i);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public record TrackPatch(String name, Double gainDb, Double pan, Boolean muted, Boolean soloed, String color) {}
}
