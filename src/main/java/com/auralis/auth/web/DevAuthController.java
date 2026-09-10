package com.auralis.auth.web;

import com.auralis.auth.AuthService;
import com.auralis.auth.domain.AuthProvider;
import com.auralis.auth.domain.User;
import com.auralis.auth.dto.AuthDtos.DevLoginRequest;
import com.auralis.auth.dto.AuthDtos.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only shortcut so the API is usable without configuring real OAuth2 apps.
 * Enabled by {@code auralis.auth.dev-login-enabled=true} (set in the dev profile).
 */
@Tag(name = "Auth (dev)")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auralis.auth", name = "dev-login-enabled", havingValue = "true")
public class DevAuthController {

    private final AuthService authService;

    @Operation(summary = "Dev-only: create/return a LOCAL user and issue tokens without OAuth2")
    @PostMapping("/dev-login")
    public TokenResponse devLogin(@Valid @RequestBody DevLoginRequest body, HttpServletRequest request) {
        String name = body.displayName() == null || body.displayName().isBlank()
                ? body.email().split("@")[0] : body.displayName();
        User user = authService.provision(AuthProvider.LOCAL, "dev:" + body.email(), body.email(), name, null);
        AuthService.TokenBundle b = authService.login(user, request.getHeader("User-Agent"));
        return TokenResponse.bearer(b.accessToken(), b.expiresInSeconds(), b.refreshToken());
    }
}
