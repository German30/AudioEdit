package com.auralis.project.repo;

import com.auralis.project.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    long countByOwnerUserId(UUID ownerUserId);

    @Query("""
            select p from Project p
            where p.ownerUserId = :userId
               or p.id in (select m.projectId from ProjectMember m where m.userId = :userId)
            order by coalesce(p.lastOpenedAt, p.createdAt) desc
            """)
    Page<Project> findAccessibleBy(@Param("userId") UUID userId, Pageable pageable);
}
