package com.jachwisunbae.common.security;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtAccessTokenIssuer(
            final JwtEncoder jwtEncoder,
            final JwtProperties properties,
            final Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedAccessToken issue(final long memberId) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("회원 ID는 양수여야 합니다.");
        }

        final Instant issuedAt = clock.instant();
        final Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        final JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .audience(List.of(properties.audience()))
                .subject(Long.toString(memberId))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .build();
        final String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
    }
}
