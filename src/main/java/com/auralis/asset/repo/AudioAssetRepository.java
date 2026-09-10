package com.auralis.asset.repo;

import com.auralis.asset.domain.AudioAsset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AudioAssetRepository extends JpaRepository<AudioAsset, UUID> {

    Page<AudioAsset> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId, Pageable pageable);

    Optional<AudioAsset> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

    long countByOwnerUserId(UUID ownerUserId);
}
