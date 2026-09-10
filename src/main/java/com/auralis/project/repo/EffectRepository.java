package com.auralis.project.repo;

import com.auralis.project.domain.Effect;
import com.auralis.project.domain.EffectScope;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EffectRepository extends JpaRepository<Effect, UUID> {

    List<Effect> findByProjectIdOrderByPositionAsc(UUID projectId);

    List<Effect> findByClipIdOrderByPositionAsc(UUID clipId);

    List<Effect> findByTrackIdOrderByPositionAsc(UUID trackId);

    List<Effect> findByProjectIdAndScopeOrderByPositionAsc(UUID projectId, EffectScope scope);

    Optional<Effect> findByIdAndProjectId(UUID id, UUID projectId);

    void deleteByClipId(UUID clipId);

    void deleteByTrackId(UUID trackId);

    void deleteByProjectId(UUID projectId);
}
