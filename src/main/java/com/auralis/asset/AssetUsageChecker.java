package com.auralis.asset;

import java.util.UUID;

/**
 * Implemented by other modules (e.g. project) so the asset module can refuse to
 * delete an asset that is still in use without depending on those modules.
 */
public interface AssetUsageChecker {

    boolean isAssetInUse(UUID assetId);

    /** Human-readable reason for the block, shown to the caller. */
    default String describeUsage(UUID assetId) {
        return "asset is referenced by one or more projects";
    }
}
