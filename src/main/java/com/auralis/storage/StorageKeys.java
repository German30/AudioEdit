package com.auralis.storage;

import java.util.Locale;
import java.util.UUID;

/**
 * Canonical object-key layout. Keeping this in one place makes lifecycle rules
 * and prefix-based cleanup predictable.
 */
public final class StorageKeys {

    private StorageKeys() {}

    public static String sourceAsset(UUID userId, UUID assetId, String filename) {
        return "users/%s/assets/%s/source/%s".formatted(userId, assetId, sanitize(filename));
    }

    public static String assetWaveform(UUID assetId) {
        return "assets/%s/waveform.json".formatted(assetId);
    }

    public static String renderOutput(UUID projectId, UUID jobId, String extension) {
        return "projects/%s/renders/%s.%s".formatted(projectId, jobId, extension.toLowerCase(Locale.ROOT));
    }

    public static String renderStemZip(UUID projectId, UUID jobId) {
        return "projects/%s/renders/%s-stems.zip".formatted(projectId, jobId);
    }

    public static String sanitize(String filename) {
        String cleaned = filename == null ? "file" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.length() > 180 ? cleaned.substring(cleaned.length() - 180) : cleaned;
    }
}
