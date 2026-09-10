package com.auralis.project.domain;

public enum EffectScope {
    /** Applied to a single clip's audio. */
    CLIP,
    /** Applied to the whole track after its clips are laid out. */
    TRACK,
    /** Applied to the final mix bus. */
    MASTER
}
