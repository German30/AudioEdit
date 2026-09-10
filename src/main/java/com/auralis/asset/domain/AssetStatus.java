package com.auralis.asset.domain;

public enum AssetStatus {
    /** Row created, waiting for the client to PUT bytes to the presigned URL. */
    PENDING_UPLOAD,
    /** Bytes received; ffprobe + waveform generation running. */
    PROCESSING,
    /** Analyzed and usable in projects. */
    READY,
    /** Analysis failed; see {@code failureReason}. */
    FAILED
}
