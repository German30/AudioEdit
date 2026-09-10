package com.auralis.project.repo;

import com.auralis.project.domain.EditVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EditVersionRepository extends JpaRepository<EditVersion, UUID> {

    Page<EditVersion> findByProjectIdOrderByRevisionDesc(UUID projectId, Pageable pageable);

    Optional<EditVersion> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<EditVersion> findFirstByProjectIdOrderByRevisionDesc(UUID projectId);

    void deleteByProjectId(UUID projectId);
}
