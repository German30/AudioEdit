package com.auralis.storage;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

/**
 * Object-storage abstraction. Backed by MinIO in dev and S3-compatible storage
 * in production; callers never see the SDK types.
 */
public interface StorageService {

    /**
     * A short-lived URL the client PUTs the file bytes to directly, plus the
     * headers it must send. Keeps large uploads off the API server.
     */
    record PresignedUpload(String url, String method, Map<String, String> headers, String objectKey, Instant expiresAt) {}

    record PresignedDownload(String url, Instant expiresAt) {}

    PresignedUpload presignUpload(String objectKey, String contentType);

    PresignedDownload presignDownload(String objectKey, String downloadFilename);

    void upload(String objectKey, Path source, String contentType);

    void download(String objectKey, Path destination);

    void delete(String objectKey);

    boolean exists(String objectKey);

    /** Size in bytes, or {@code -1} if the object does not exist. */
    long size(String objectKey);
}
