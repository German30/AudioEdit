package com.auralis.project.repo;

import com.auralis.project.domain.Track;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository extends JpaRepository<Track, UUID> {

    List<Track> findByProjectIdOrderByPositionAsc(UUID projectId);

    Optional<Track> findByIdAndProjectId(UUID id, UUID projectId);

    long countByProjectId(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
