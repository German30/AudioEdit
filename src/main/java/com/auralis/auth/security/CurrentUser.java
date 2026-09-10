package com.auralis.auth.security;

import com.auralis.auth.domain.User;
import com.auralis.auth.repo.UserRepository;
import com.auralis.common.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Convenience accessor for the authenticated caller. Prefer {@link #id()} for
 * authorization checks (no DB hit) and {@link #entity()} only when the full
 * {@link User} row is actually needed.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public UUID id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "unauthorized", "Not authenticated");
        }
        return UUID.fromString(jwt.getSubject());
    }

    public User entity() {
        return userRepository.findById(id())
                .orElseThrow(() -> ApiException.notFound("Current user"));
    }
}
