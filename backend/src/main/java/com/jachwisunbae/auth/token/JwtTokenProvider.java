package com.jachwisunbae.auth.token;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

public class JwtTokenProvider {

    private final byte[] secret;
    private final String issuer;
    private final String audience;
    private final long accessTokenSeconds;
    private final Clock clock;

    public JwtTokenProvider(
            String secret,
            String issuer,
            String audience,
            long accessTokenSeconds,
            Clock clock) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenSeconds = accessTokenSeconds;
        this.clock = clock;
    }

    public String createAccessToken(Long memberId) {
        try {
            Instant now = clock.instant();
            return sign(createClaims(memberId, now));
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    public Long parseMemberId(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!isValid(jwt, claims)) {
                throw invalidToken();
            }
            return Long.valueOf(claims.getSubject());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private JWTClaimsSet createClaims(Long memberId, Instant issuedAt) {
        return new JWTClaimsSet.Builder()
                .subject(memberId.toString())
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plusSeconds(accessTokenSeconds)))
                .build();
    }

    private String sign(JWTClaimsSet claims) throws Exception {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret));
        return jwt.serialize();
    }

    private boolean isValid(SignedJWT jwt, JWTClaimsSet claims) throws Exception {
        return jwt.verify(new MACVerifier(secret))
                && issuer.equals(claims.getIssuer())
                && claims.getAudience().contains(audience)
                && claims.getExpirationTime().toInstant().isAfter(clock.instant());
    }

    private BusinessException invalidToken() {
        return new BusinessException(DomainErrorCode.ACCESS_TOKEN_INVALID, "Access Token이 올바르지 않습니다.");
    }
}
