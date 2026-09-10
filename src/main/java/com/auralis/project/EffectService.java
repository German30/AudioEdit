package com.auralis.project;

import com.auralis.common.error.ApiException;
import com.auralis.project.domain.Effect;
import com.auralis.project.domain.EffectScope;
import com.auralis.project.domain.EffectType;
import com.auralis.project.domain.ProjectRole;
import com.auralis.project.repo.ClipRepository;
import com.auralis.project.repo.EffectRepository;
import com.auralis.project.repo.TrackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EffectService {

    private final EffectRepository effectRepository;
    private final ClipRepository clipRepository;
    private final TrackRepository trackRepository;
    private final ProjectAccessService access;

    @Transactional(readOnly = true)
    public List<Effect> list(UUID userId, UUID projectId) {
        access.requireRole(userId, projectId, ProjectRole.VIEWER);
        return effectRepository.findByProjectIdOrderByPositionAsc(projectId);
    }

    @Transactional
    public Effect add(UUID userId, UUID projectId, NewEffect body) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        validateTarget(projectId, body.scope(), body.clipId(), body.trackId());

        Effect effect = new Effect();
        effect.setProjectId(projectId);
        effect.setScope(body.scope());
        effect.setClipId(body.scope() == EffectScope.CLIP ? body.clipId() : null);
        effect.setTrackId(body.scope() == EffectScope.TRACK ? body.trackId() : null);
        effect.setType(body.type());
        effect.setEnabled(body.enabled() == null || body.enabled());
        if (body.params() != null) effect.getParams().putAll(body.params());
        effect.setPosition(nextPosition(projectId, body.scope(), body.clipId(), body.trackId()));
        return effectRepository.save(effect);
    }

    @Transactional
    public Effect update(UUID userId, UUID projectId, UUID effectId, EffectPatch patch) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        Effect effect = require(projectId, effectId);
        if (patch.enabled() != null) effect.setEnabled(patch.enabled());
        if (patch.position() != null) effect.setPosition(Math.max(0, patch.position()));
        if (patch.params() != null) {
            effect.getParams().clear();
            effect.getParams().putAll(patch.params());
        }
        return effect;
    }

    @Transactional
    public void delete(UUID userId, UUID projectId, UUID effectId) {
        access.requireRole(userId, projectId, ProjectRole.EDITOR);
        effectRepository.delete(require(projectId, effectId));
    }

    private Effect require(UUID projectId, UUID effectId) {
        return effectRepository.findByIdAndProjectId(effectId, projectId)
                .orElseThrow(() -> ApiException.notFound("Effect"));
    }

    private void validateTarget(UUID projectId, EffectScope scope, UUID clipId, UUID trackId) {
        switch (scope) {
            case CLIP -> {
                if (clipId == null) throw ApiException.badRequest("clipId is required for CLIP scope");
                clipRepository.findByIdAndProjectId(clipId, projectId)
                        .orElseThrow(() -> ApiException.notFound("Clip"));
            }
            case TRACK -> {
                if (trackId == null) throw ApiException.badRequest("trackId is required for TRACK scope");
                trackRepository.findByIdAndProjectId(trackId, projectId)
                        .orElseThrow(() -> ApiException.notFound("Track"));
            }
            case MASTER -> { /* no target */ }
        }
    }

    private int nextPosition(UUID projectId, EffectScope scope, UUID clipId, UUID trackId) {
        List<Effect> siblings = switch (scope) {
            case CLIP -> effectRepository.findByClipIdOrderByPositionAsc(clipId);
            case TRACK -> effectRepository.findByTrackIdOrderByPositionAsc(trackId);
            case MASTER -> effectRepository.findByProjectIdAndScopeOrderByPositionAsc(projectId, EffectScope.MASTER);
        };
        return siblings.isEmpty() ? 0 : siblings.get(siblings.size() - 1).getPosition() + 1;
    }

    public record NewEffect(EffectScope scope, UUID clipId, UUID trackId, EffectType type,
                            Boolean enabled, Map<String, Object> params) {}

    public record EffectPatch(Boolean enabled, Integer position, Map<String, Object> params) {}
}
