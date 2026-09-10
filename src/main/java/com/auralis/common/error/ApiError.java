package com.auralis.common.error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Uniform error body returned for every non-2xx response.
 *
 * @param code       stable machine-readable identifier (e.g. {@code quota_exceeded})
 * @param message    human-readable summary
 * @param status     HTTP status code
 * @param path       request path that produced the error
 * @param fieldErrors per-field validation messages, if any
 * @param timestamp  when the error was produced
 */
public record ApiError(
        String code,
        String message,
        int status,
        String path,
        Map<String, List<String>> fieldErrors,
        Instant timestamp
) {
    public static ApiError of(String code, String message, int status, String path) {
        return new ApiError(code, message, status, path, Map.of(), Instant.now());
    }
}
