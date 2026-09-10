package com.auralis.render.repo;

import com.auralis.render.domain.RenderJob;
import com.auralis.render.domain.RenderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RenderJobRepository extends JpaRepository<RenderJob, UUID> {

    Page<RenderJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    Optional<RenderJob> findByIdAndProjectId(UUID id, UUID projectId);

    List<RenderJob> findTop20ByStatusOrderByCreatedAtAsc(RenderStatus status);

    long countByStatus(RenderStatus status);

    /**
     * Atomic claim: succeeds (returns 1) only for the worker that flips QUEUED -> RUNNING first.
     * Runs in its own transaction so it works when called from the (non-transactional) scheduler.
     */
    @Modifying
    @Transactional
    @Query("""
            update RenderJob j
               set j.status = com.auralis.render.domain.RenderStatus.RUNNING,
                   j.startedAt = :now,
                   j.attempts = j.attempts + 1
             where j.id = :id and j.status = com.auralis.render.domain.RenderStatus.QUEUED
            """)
    int claim(@Param("id") UUID id, @Param("now") Instant now);

    /**
     * Recover jobs stuck in RUNNING after a crash (started long ago, never finished).
     * Runs in its own transaction (called from the non-transactional scheduler).
     */
    @Modifying
    @Transactional
    @Query("""
            update RenderJob j
               set j.status = com.auralis.render.domain.RenderStatus.QUEUED
             where j.status = com.auralis.render.domain.RenderStatus.RUNNING
               and j.startedAt < :cutoff
            """)
    int requeueStale(@Param("cutoff") Instant cutoff);
}
