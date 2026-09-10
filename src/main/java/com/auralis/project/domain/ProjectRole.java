package com.auralis.project.domain;

/**
 * A member's capability on a single project. Ordinal order matters:
 * {@code OWNER > EDITOR > VIEWER} is used for "at least" checks.
 */
public enum ProjectRole {
    VIEWER,
    EDITOR,
    OWNER;

    public boolean atLeast(ProjectRole required) {
        return this.ordinal() >= required.ordinal();
    }
}
