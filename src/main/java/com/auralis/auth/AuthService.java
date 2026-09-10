package com.auralis.auth;

import com.auralis.auth.config.AuthProperties;
import com.auralis.auth.domain.AuthProvider;
import com.auralis.auth.domain.RefreshToken;
import com.auralis.auth.domain.User;
import com.auralis.auth.event.UserRegisteredEvent;
import com.auralis.auth.jwt.JwtService;
import com.auralis.auth.repo.RefreshTokenRepository;
import com.auralis.auth.repo.UserRepository;
import com.auralis.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns login/refresh/logout and the opaque refresh-token lifecycle (rotation +
 * revocation). Access tokens themselves are minted by {@link JwtService}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuthProperties props;
    private final ApplicationEventPublisher events;

    private final Base64StringKeyGenerator refreshTokenGenerator = new Base64StringKeyGenerator(48);

    /** Finds an existing account for the OAuth2 identity or creates a fresh one. */
    @Transactional
    public User provision(AuthProvider provider, String providerId, String email,
                          String displayName, String avatarUrl) {
        Optional<User> byProvider = userRepository.findByProviderAndProviderId(provider, providerId);
        if (byProvider.isPresent()) {
            User u = byProvider.get();
            u.setDisplayName(displayName);
            u.setAvatarUrl(avatarUrl);
            return u;
        }
        // Same email arriving from a different provider: link by adopting the new provider.
        Optional<User> byEmail = userRepository.findByEmailIgnoreCase(email);
        if (byEmail.isPresent()) {
            User u = byEmail.get();
            u.setProvider(provider);
            u.setProviderId(providerId);
            if (avatarUrl != null) u.setAvatarUrl(avatarUrl);
            return u;
        }
        User created = new User(email, displayName, provider, providerId);
        created.setAvatarUrl(avatarUrl);
        created = userRepository.save(created);
        events.publishEvent(new UserRegisteredEvent(created.getId(), created.getEmail()));
        return created;
    }

    @Transactional
    public TokenBundle login(User user, String userAgent) {
        user.setLastLoginAt(Instant.now());
        return issueBundle(user, userAgent);
    }

    @Transactional
    public TokenBundle refresh(String rawRefreshToken, String userAgent) {
        String hash = sha256(rawRefreshToken);
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "invalid_refresh_token", "Refresh token is not recognised"));
        if (!current.isActive()) {
            // Re-use of a revoked token => treat as compromise, drop the whole family.
            refreshTokenRepository.revokeAllForUser(current.getUserId());
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "invalid_refresh_token", "Refresh token is expired or revoked");
        }
        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> ApiException.notFound("User"));
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Account is disabled");
        }
        TokenBundle bundle = issueBundle(user, userAgent);
        current.setRevoked(true);
        current.setReplacedBy(UUID.fromString(bundle.refreshTokenId()));
        return bundle;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .ifPresent(t -> t.setRevoked(true));
    }

    @Transactional
    public void logoutAll(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private TokenBundle issueBundle(User user, String userAgent) {
        JwtService.IssuedAccessToken access = jwtService.issueAccessToken(user);

        String raw = refreshTokenGenerator.generateKey();
        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setTokenHash(sha256(raw));
        rt.setExpiresAt(Instant.now().plus(props.jwt().refreshTokenTtl()));
        rt.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 128)));
        rt = refreshTokenRepository.save(rt);

        return new TokenBundle(access.value(), access.expiresInSeconds(), raw, rt.getId().toString());
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public record TokenBundle(String accessToken, long expiresInSeconds, String refreshToken, String refreshTokenId) {}
}
