package com.auralis.auth.jwt;

import com.auralis.auth.config.AuthProperties;
import com.auralis.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Issues the short-lived access tokens that authenticate calls to the API.
 * Refresh tokens are opaque and handled by {@link com.auralis.auth.AuthService}.
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    public static final String ROLES_CLAIM = "roles";
    public static final String EMAIL_CLAIM = "email";
    public static final String NAME_CLAIM = "name";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties props;

    public IssuedAccessToken issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.jwt().accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.getId().toString())
                .claim(ROLES_CLAIM, java.util.List.of(user.getRole().name()))
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(NAME_CLAIM, user.getDisplayName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, exp, props.jwt().accessTokenTtl().toSeconds());
    }

    public record IssuedAccessToken(String value, Instant expiresAt, long expiresInSeconds) {}
}
