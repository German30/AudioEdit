package com.auralis.auth.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A single issued refresh token. Tokens are opaque random strings; only their
 * SHA-256 hash is stored. Rotation: on use, the current row is marked
 * {@code revoked} and {@code replacedBy} points at the successor.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "ux_refresh_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "ix_refresh_user", columnList = "user_id")
})
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(length = 36)
    private UUID replacedBy;

    /** Coarse client fingerprint for audit ("web", user-agent hash, ...). */
    @Column(length = 128)
    private String userAgent;

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(Instant.now());
    }
}
