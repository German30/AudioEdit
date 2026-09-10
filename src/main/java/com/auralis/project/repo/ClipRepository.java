package com.auralis.project.repo;

import com.auralis.project.domain.Clip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClipRepository extends JpaRepository<Clip, UUID> {

    List<Clip> findByProjectIdOrderByTimelineStartSecondsAsc(UUID projectId);

    List<Clip> findByTrackIdOrderByTimelineStartSecondsAsc(UUID trackId);

    Optional<Clip> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsBySourceAssetId(UUID sourceAssetId);

    void deleteByTrackId(UUID trackId);

    void deleteByProjectId(UUID projectId);
}
