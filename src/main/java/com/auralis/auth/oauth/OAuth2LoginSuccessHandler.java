package com.auralis.auth.oauth;

import com.auralis.auth.AuthService;
import com.auralis.auth.config.AuthProperties;
import com.auralis.auth.domain.AuthProvider;
import com.auralis.auth.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * After a successful OAuth2 provider login we mint our own token pair and bounce
 * the browser back to the SPA with the tokens in the query string. The SPA is
 * expected to read them, store them, and clean the URL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final AuthProperties props;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2User principal = token.getPrincipal();

        NormalizedProfile profile = normalize(registrationId, principal);
        User user = authService.provision(profile.provider(), profile.providerId(),
                profile.email(), profile.displayName(), profile.avatarUrl());
        AuthService.TokenBundle bundle = authService.login(user, request.getHeader("User-Agent"));

        String target = UriComponentsBuilder.fromUriString(props.frontendRedirectUri())
                .queryParam("access_token", bundle.accessToken())
                .queryParam("refresh_token", bundle.refreshToken())
                .queryParam("expires_in", bundle.expiresInSeconds())
                .build().toUriString();

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private NormalizedProfile normalize(String registrationId, OAuth2User p) {
        return switch (registrationId) {
            case "google" -> new NormalizedProfile(
                    AuthProvider.GOOGLE,
                    str(p, "sub"),
                    str(p, "email"),
                    firstNonBlank(str(p, "name"), str(p, "email")),
                    str(p, "picture"));
            case "github" -> {
                String login = str(p, "login");
                String email = firstNonBlank(str(p, "email"), login + "@users.noreply.github.com");
                yield new NormalizedProfile(
                        AuthProvider.GITHUB,
                        String.valueOf(p.getAttributes().get("id")),
                        email,
                        firstNonBlank(str(p, "name"), login),
                        str(p, "avatar_url"));
            }
            default -> throw new IllegalStateException("Unsupported OAuth2 provider: " + registrationId);
        };
    }

    private static String str(OAuth2User user, String key) {
        Object v = user.getAttributes().get(key);
        return v == null ? null : v.toString();
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private record NormalizedProfile(AuthProvider provider, String providerId, String email,
                                     String displayName, String avatarUrl) {}
}
