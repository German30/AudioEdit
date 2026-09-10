package com.auralis.auth.web;

import com.auralis.auth.AuthService;
import com.auralis.auth.dto.AuthDtos.ProviderInfo;
import com.auralis.auth.dto.AuthDtos.LogoutRequest;
import com.auralis.auth.dto.AuthDtos.RefreshRequest;
import com.auralis.auth.dto.AuthDtos.TokenResponse;
import com.auralis.auth.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @Operation(summary = "List enabled OAuth2 identity providers and their start URLs")
    @GetMapping("/providers")
    public List<ProviderInfo> providers() {
        return List.of(
                new ProviderInfo("google", "/oauth2/authorization/google"),
                new ProviderInfo("github", "/oauth2/authorization/github"));
    }

    @Operation(summary = "Exchange a refresh token for a new access token (token rotation)")
    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody RefreshRequest body, HttpServletRequest request) {
        AuthService.TokenBundle b = authService.refresh(body.refreshToken(), request.getHeader("User-Agent"));
        return TokenResponse.bearer(b.accessToken(), b.expiresInSeconds(), b.refreshToken());
    }

    @Operation(summary = "Revoke a single refresh token")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody(required = false) LogoutRequest body) {
        authService.logout(body == null ? null : body.refreshToken());
    }

    @Operation(summary = "Revoke every refresh token for the authenticated user")
    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        authService.logoutAll(currentUser.id());
    }
}
