package com.auralis.auth.dto;

import com.auralis.auth.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Request/response payloads for the auth + profile endpoints, grouped so the
 * small records stay together.
 */
public final class AuthDtos {

    private AuthDtos() {}

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken) {
        public static TokenResponse bearer(String accessToken, long expiresIn, String refreshToken) {
            return new TokenResponse(accessToken, "Bearer", expiresIn, refreshToken);
        }
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(String refreshToken) {}

    public record DevLoginRequest(@Email @NotBlank String email, @Size(max = 120) String displayName) {}

    public record UpdateProfileRequest(@Size(min = 1, max = 120) String displayName,
                                       @Size(max = 512) String avatarUrl) {}

    public record UserResponse(String id, String email, String displayName, String avatarUrl,
                               String role, String provider, Instant createdAt, Instant lastLoginAt) {
        public static UserResponse of(User u) {
            return new UserResponse(
                    u.getId().toString(), u.getEmail(), u.getDisplayName(), u.getAvatarUrl(),
                    u.getRole().name(), u.getProvider().name(), u.getCreatedAt(), u.getLastLoginAt());
        }
    }

    public record ProviderInfo(String id, String authorizeUrl) {}
}
