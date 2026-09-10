package com.auralis.auth.domain;

import com.auralis.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "users", indexes = {
        @Index(name = "ux_users_email", columnList = "email", unique = true),
        @Index(name = "ix_users_provider", columnList = "provider,provider_id")
})
public class User extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Column
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuthProvider provider;

    /** The subject id from the OAuth2 provider (Google {@code sub}, GitHub numeric id). */
    @Column(name = "provider_id")
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column
    private Instant lastLoginAt;

    public User(String email, String displayName, AuthProvider provider, String providerId) {
        this.email = email;
        this.displayName = displayName;
        this.provider = provider;
        this.providerId = providerId;
    }
}
