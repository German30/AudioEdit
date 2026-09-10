package com.auralis.common.error;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would exceed the limits granted by the caller's
 * subscription plan (project count, monthly render minutes, storage bytes,
 * collaborators, export format, ...).
 */
public class QuotaExceededException extends ApiException {

    public QuotaExceededException(String entitlement, String detail) {
        super(HttpStatus.PAYMENT_REQUIRED, "quota_exceeded",
                "Plan limit reached for '" + entitlement + "': " + detail);
    }
}
