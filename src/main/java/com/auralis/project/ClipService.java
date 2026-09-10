package com.auralis.project;

import com.auralis.asset.domain.AssetStatus;
import com.auralis.asset.domain.AudioAsset;
import com.auralis.asset.repo.AudioAssetRepository;
import com.auralis.common.error.ApiException;
import com.auralis.project.domain.Clip;
import com.auralis.project.domain.Project;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.domain.Track;
import com.auralis.project.repo.ClipRepository;
import com.auralis.project.repo.EffectRepository;
import com.auralis.project.repo.ProjectMemberRepository;
import com.auralis.project.repo.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClipService {

    private final ClipRepository clipRepository;
    private final TrackRepository trackRepository;
    private final EffectRepository effectRepository;
    private final AudioAssetRepository assetRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAccessService access;

    @Transactional(readOnly = true)
    public List<Clip> list(UUID userId, UUID projectId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return clipRepository.findByProjectIdOrderByTimelineStartSecondsAsc(projectId);
    }

    @Transactional
    public Clip add(UUID userId, UUID projectId, UUID trackId, UUID sourceAssetId, NewClip body) {
        Project project = access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Track track = trackRepository.findByIdAndProjectId(trackId, projectId)
                .orElseThrow(() -> ApiException.notFound("Track"));

        AudioAsset asset = assetRepository.findById(sourceAssetId)
                .orElseThrow(() -> ApiException.notFound("Asset"));
        assertAssetUsableBy(project, userId, asset);
        if (asset.getStatus() != AssetStatus.READY) {
            throw ApiException.unprocessable("asset_not_ready", "Asset is " + asset.getStatus());
        }

        double sourceDuration = asset.getDurationSeconds() == null ? 0 : asset.getDurationSeconds();
        double offset = clampMin(body.sourceOffsetSeconds(), 0);
        double duration = body.durationSeconds() != null && body.durationSeconds() > 0
                ? body.durationSeconds()
                : Math.max(sourceDuration - offset, 0);

        Clip clip = new Clip();
        clip.setProjectId(projectId);
        clip.setTrackId(track.getId());
        clip.setSourceAssetId(sourceAssetId);
        clip.setName(body.name() != null ? body.name() : asset.getOriginalFilename());
        clip.setTimelineStartSeconds(clampMin(body.timelineStartSeconds(), 0));
        clip.setSourceOffsetSeconds(offset);
        clip.setDurationSeconds(duration);
        if (body.gainDb() != null) clip.setGainDb(body.gainDb());
        if (body.fadeInSeconds() != null) clip.setFadeInSeconds(clampMin(body.fadeInSeconds(), 0));
        if (body.fadeOutSeconds() != null) clip.setFadeOutSeconds(clampMin(body.fadeOutSeconds(), 0));
        if (body.playbackRate() != null) clip.setPlaybackRate(clampRange(body.playbackRate(), 0.1, 8.0));
        if (body.reversed() != null) clip.setReversed(body.reversed());
        return clipRepository.save(clip);
    }

    @Transactional
    public Clip update(UUID userId, UUID projectId, UUID clipId, ClipPatch patch) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Clip clip = require(projectId, clipId);
        if (patch.trackId() != null && !patch.trackId().equals(clip.getTrackId())) {
            trackRepository.findByIdAndProjectId(patch.trackId(), projectId)
                    .orElseThrow(() -> ApiException.notFound("Track"));
            clip.setTrackId(patch.trackId());
        }
        if (patch.name() != null) clip.setName(patch.name());
        if (patch.timelineStartSeconds() != null) clip.setTimelineStartSeconds(clampMin(patch.timelineStartSeconds(), 0));
        if (patch.sourceOffsetSeconds() != null) clip.setSourceOffsetSeconds(clampMin(patch.sourceOffsetSeconds(), 0));
        if (patch.durationSeconds() != null) clip.setDurationSeconds(clampMin(patch.durationSeconds(), 0));
        if (patch.gainDb() != null) clip.setGainDb(patch.gainDb());
        if (patch.fadeInSeconds() != null) clip.setFadeInSeconds(clampMin(patch.fadeInSeconds(), 0));
        if (patch.fadeOutSeconds() != null) clip.setFadeOutSeconds(clampMin(patch.fadeOutSeconds(), 0));
        if (patch.playbackRate() != null) clip.setPlaybackRate(clampRange(patch.playbackRate(), 0.1, 8.0));
        if (patch.reversed() != null) clip.setReversed(patch.reversed());
        return clip;
    }

    @Transactional
    public void delete(UUID userId, UUID projectId, UUID clipId) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Clip clip = require(projectId, clipId);
        effectRepository.deleteByClipId(clipId);
        clipRepository.delete(clip);
    }

    /** Split a clip at an absolute timeline position, returning [left, right]. */
    @Transactional
    public List<Clip> split(UUID userId, UUID projectId, UUID clipId, double atTimelineSeconds) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Clip left = require(projectId, clipId);
        double clipEnd = left.getTimelineStartSeconds() + left.getDurationSeconds();
        if (atTimelineSeconds <= left.getTimelineStartSeconds() || atTimelineSeconds >= clipEnd) {
            throw ApiException.badRequest("Split point must be inside the clip");
        }
        double leftDuration = atTimelineSeconds - left.getTimelineStartSeconds();
        double rate = left.getPlaybackRate() <= 0 ? 1.0 : left.getPlaybackRate();

        Clip right = new Clip();
        right.setProjectId(projectId);
        right.setTrackId(left.getTrackId());
        right.setSourceAssetId(left.getSourceAssetId());
        right.setName(left.getName());
        right.setTimelineStartSeconds(atTimelineSeconds);
        right.setSourceOffsetSeconds(left.getSourceOffsetSeconds() + leftDuration * rate);
        right.setDurationSeconds(clipEnd - atTimelineSeconds);
        right.setGainDb(left.getGainDb());
        right.setFadeOutSeconds(left.getFadeOutSeconds());
        right.setPlaybackRate(left.getPlaybackRate());
        right.setReversed(left.isReversed());

        left.setDurationSeconds(leftDuration);
        left.setFadeOutSeconds(0);
        Clip savedRight = clipRepository.save(right);
        return List.of(left, savedRight);
    }

    private Clip require(UUID projectId, UUID clipId) {
        return clipRepository.findByIdAndProjectId(clipId, projectId)
                .orElseThrow(() -> ApiException.notFound("Clip"));
    }

    /** The asset must be owned by a member of the project (usually the person adding it). */
    private void assertAssetUsableBy(Project project, UUID actingUserId, AudioAsset asset) {
        boolean ownerIsMember = asset.getOwnerUserId().equals(project.getOwnerUserId())
                || asset.getOwnerUserId().equals(actingUserId)
                || memberRepository.findByProjectIdAndUserId(project.getId(), asset.getOwnerUserId()).isPresent();
        if (!ownerIsMember) {
            throw ApiException.forbidden("That asset does not belong to a member of this project");
        }
    }

    private static double clampMin(Double v, double min) {
        return v == null ? min : Math.max(min, v);
    }

    private static double clampRange(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public record NewClip(String name, Double timelineStartSeconds, Double sourceOffsetSeconds,
                          Double durationSeconds, Double gainDb, Double fadeInSeconds, Double fadeOutSeconds,
                          Double playbackRate, Boolean reversed) {}

    public record ClipPatch(UUID trackId, String name, Double timelineStartSeconds, Double sourceOffsetSeconds,
                            Double durationSeconds, Double gainDb, Double fadeInSeconds, Double fadeOutSeconds,
                            Double playbackRate, Boolean reversed) {}
}
