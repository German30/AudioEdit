package com.auralis.project;

import com.auralis.asset.AssetUsageChecker;
import com.auralis.project.repo.ClipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lets the asset module know an asset is still placed on a project timeline, so
 * it refuses to delete it.
 */
@Component
@RequiredArgsConstructor
public class ClipAssetUsageChecker implements AssetUsageChecker {

    private final ClipRepository clipRepository;

    @Override
    public boolean isAssetInUse(UUID assetId) {
        return clipRepository.existsBySourceAssetId(assetId);
    }

    @Override
    public String describeUsage(UUID assetId) {
        return "the asset is used by one or more clips; remove those clips first";
    }
}
