package com.auralis.auth.domain;

/**
 * Coarse account roles. Fine-grained access to individual projects is handled
 * separately by {@code ProjectMember} roles.
 */
public enum Role {
    /** Regular signed-up user. */
    USER,
    /** Staff account with access to the admin API. */
    ADMIN
}
