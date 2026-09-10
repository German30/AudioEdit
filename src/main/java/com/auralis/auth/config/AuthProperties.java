package com.auralis.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Binds the {@code auralis.auth.*} configuration tree.
 */
@ConfigurationProperties(prefix = "auralis.auth")
public record AuthProperties(
        Jwt jwt,
        String frontendRedirectUri,
        List<String> corsAllowedOrigins,
        boolean devLoginEnabled
) {
    public record Jwt(
            String secret,
            String issuer,
            Duration accessTokenTtl,
            Duration refreshTokenTtl
    ) {}
}
