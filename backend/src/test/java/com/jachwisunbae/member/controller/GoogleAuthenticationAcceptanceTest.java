package com.jachwisunbae.member.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.AcceptanceTest;
import com.jachwisunbae.common.security.JwtProperties;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

class GoogleAuthenticationAcceptanceTest extends AcceptanceTest {

    private static final String LOGIN_URL = "/api/auth/google";
    private static final String ME_URL = "/api/members/me";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void deleteMembers() {
        jdbcTemplate.update("DELETE FROM members");
    }

    @DisplayName("최초 로그인과 재로그인은 같은 회원을 사용하고 발급한 Access Token으로 본인을 조회한다")
    @Test
    void loginAndGetMe() {
        final ResponseEntity<JsonNode> firstLogin = login("valid-code:google-subject-1", "valid-nonce");
        final ResponseEntity<JsonNode> secondLogin = login("valid-code:google-subject-1", "valid-nonce");

        assertThat(firstLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstLogin.getBody()).isNotNull();
        final JsonNode firstData = firstLogin.getBody().path("data");
        assertThat(firstData.path("accessToken").asText()).isNotBlank();
        assertThat(firstData.path("tokenType").asText()).isEqualTo("Bearer");
        assertThat(firstData.path("expiresIn").asLong()).isEqualTo(43_200L);
        assertThat(firstData.path("member").path("memberId").asLong()).isPositive();
        assertThat(firstLogin.getHeaders().getFirst("X-Request-Id")).isNotBlank();
        assertThat(secondLogin.getBody().path("data").path("member").path("memberId").asLong())
                .isEqualTo(firstData.path("member").path("memberId").asLong());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM members", Long.class)).isEqualTo(1L);

        final String accessToken = firstData.path("accessToken").asText();
        final ResponseEntity<JsonNode> me = getMe("Bearer " + accessToken);

        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().path("data").path("memberId").asLong())
                .isEqualTo(firstData.path("member").path("memberId").asLong());
        assertThat(me.getBody().path("data").path("email").asText()).isEqualTo("google-subject-1@example.com");
    }

    @DisplayName("임의 memberId query를 보내도 JWT subject의 인증 회원을 조회한다")
    @Test
    void ignoreMemberIdQueryForAuthentication() {
        final JsonNode authenticated = login("valid-code:authenticated-member", "valid-nonce")
                .getBody().path("data");
        final JsonNode other = login("valid-code:other-member", "valid-nonce")
                .getBody().path("data");

        final ResponseEntity<JsonNode> response = getMe(
                ME_URL + "?memberId=" + other.path("member").path("memberId").asLong(),
                "Bearer " + authenticated.path("accessToken").asText()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("data").path("memberId").asLong())
                .isEqualTo(authenticated.path("member").path("memberId").asLong())
                .isNotEqualTo(other.path("member").path("memberId").asLong());
    }

    @DisplayName("Google 인증 요청 검증 오류는 인증정보의 거절 값을 노출하지 않는다")
    @Test
    void validationErrorDoesNotExposeAuthenticationValues() {
        final Map<String, String> request = Map.of(
                "authorizationCode", "",
                "codeVerifier", "too-short",
                "nonce", "",
                "redirectUri", REDIRECT_URI
        );

        final ResponseEntity<JsonNode> response = postJson(LOGIN_URL, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().path("code").asText()).isEqualTo("INVALID_REQUEST");
        assertThat(response.getBody().path("errors")).allSatisfy(error -> {
            final String field = error.path("field").asText();
            if (List.of("authorizationCode", "codeVerifier", "nonce").contains(field)) {
                assertThat(error.path("rejectedValue").isNull()).isTrue();
            }
        });
        assertThat(response.getBody().toString()).doesNotContain("too-short");
    }

    @DisplayName("유효하지 않은 Google authorization code를 계약된 오류로 변환한다")
    @Test
    void invalidGoogleAuthorizationCode() {
        final ResponseEntity<JsonNode> response = login("invalid-code", "valid-nonce");

        assertError(response, HttpStatus.BAD_REQUEST, "GOOGLE_AUTHORIZATION_CODE_INVALID");
    }

    @DisplayName("허용 목록에 없는 redirect URI는 Google 호출 전에 거부한다")
    @Test
    void rejectUnregisteredRedirectUri() {
        final Map<String, String> request = Map.of(
                "authorizationCode", "valid-code:google-subject",
                "codeVerifier", CODE_VERIFIER,
                "nonce", "valid-nonce",
                "redirectUri", "https://attacker.example.com/callback"
        );

        final ResponseEntity<JsonNode> response = postJson(LOGIN_URL, request);

        assertError(response, HttpStatus.BAD_REQUEST, "GOOGLE_AUTHORIZATION_CODE_INVALID");
    }

    @DisplayName("유효하지 않은 Google 신원 정보를 계약된 오류로 변환한다")
    @Test
    void invalidGoogleIdentity() {
        final ResponseEntity<JsonNode> response = login("valid-code:invalid-identity", "valid-nonce");

        assertError(response, HttpStatus.BAD_REQUEST, "GOOGLE_IDENTITY_INVALID");
    }

    @DisplayName("Google 상류 서비스 실패를 502 오류로 변환한다")
    @Test
    void googleUpstreamFailure() {
        final ResponseEntity<JsonNode> response = login("upstream-failure", "valid-nonce");

        assertError(response, HttpStatus.BAD_GATEWAY, "GOOGLE_AUTHENTICATION_FAILED");
        assertThat(response.getBody().toString()).doesNotContain("upstream-failure");
    }

    @DisplayName("Authorization 헤더가 없으면 인증 필요 오류를 반환한다")
    @Test
    void missingAuthorizationHeader() {
        final ResponseEntity<JsonNode> response = getMe(null);

        assertError(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
    }

    @DisplayName("Bearer가 아닌 인증 스킴은 인증 필요 오류를 반환한다")
    @Test
    void wrongAuthorizationScheme() {
        final ResponseEntity<JsonNode> response = getMe("Basic credentials");

        assertError(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
    }

    @DisplayName("형식이 잘못된 JWT를 유효하지 않은 Access Token으로 거부한다")
    @Test
    void malformedAccessToken() {
        final ResponseEntity<JsonNode> response = getMe("Bearer not-a-jwt");

        assertError(response, HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_INVALID");
    }

    @DisplayName("변조된 JWT를 유효하지 않은 Access Token으로 거부한다")
    @Test
    void tamperedAccessToken() {
        final String validToken = login("valid-code:google-subject-2", "valid-nonce")
                .getBody().path("data").path("accessToken").asText();
        final int signatureStart = validToken.lastIndexOf('.') + 1;
        final char replacement = validToken.charAt(signatureStart) == 'a' ? 'b' : 'a';
        final String tamperedToken = validToken.substring(0, signatureStart)
                + replacement
                + validToken.substring(signatureStart + 1);

        final ResponseEntity<JsonNode> response = getMe("Bearer " + tamperedToken);

        assertError(response, HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_INVALID");
    }

    @DisplayName("만료된 JWT를 만료 오류로 거부한다")
    @Test
    void expiredAccessToken() {
        final Instant now = Instant.now();
        final String expiredToken = createToken("1", now.minusSeconds(7_200), now.minusSeconds(3_600));

        final ResponseEntity<JsonNode> response = getMe("Bearer " + expiredToken);

        assertError(response, HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED");
    }

    @DisplayName("존재하지 않는 회원을 가리키는 JWT를 유효하지 않은 Access Token으로 거부한다")
    @Test
    void accessTokenForMissingMember() {
        final Instant now = Instant.now();
        final String token = createToken("999999", now, now.plusSeconds(3_600));

        final ResponseEntity<JsonNode> response = getMe("Bearer " + token);

        assertError(response, HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_INVALID");
    }

    @DisplayName("OpenAPI에 로그인과 현재 사용자 조회의 인증 계약을 공개한다")
    @Test
    void openApiContainsAuthenticationContract() {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("paths").has("/api/auth/google")).isTrue();
        assertThat(response.getBody().path("paths").has("/api/members/me")).isTrue();
        assertThat(response.getBody().path("paths").path("/api/members/me").path("get").path("security").isArray())
                .isTrue();
        assertThat(response.getBody().path("components").path("securitySchemes").has("bearerAuth")).isTrue();
    }

    @DisplayName("허용된 프론트엔드 Origin의 인증 API preflight를 승인한다")
    @Test
    void allowConfiguredCorsOrigin() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setOrigin("http://localhost:3000");
        headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        headers.set(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type");

        final ResponseEntity<JsonNode> response = restTemplate.exchange(
                LOGIN_URL,
                HttpMethod.OPTIONS,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:3000");
        assertThat(response.getHeaders().getAccessControlAllowCredentials()).isFalse();
    }

    private ResponseEntity<JsonNode> login(final String authorizationCode, final String nonce) {
        final Map<String, String> request = Map.of(
                "authorizationCode", authorizationCode,
                "codeVerifier", CODE_VERIFIER,
                "nonce", nonce,
                "redirectUri", REDIRECT_URI
        );
        return postJson(LOGIN_URL, request);
    }

    private ResponseEntity<JsonNode> postJson(final String url, final Object body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity(url, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> getMe(final String authorization) {
        return getMe(ME_URL, authorization);
    }

    private ResponseEntity<JsonNode> getMe(final String url, final String authorization) {
        final HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
    }

    private String createToken(final String subject, final Instant issuedAt, final Instant expiresAt) {
        final JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        final JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .audience(List.of(jwtProperties.audience()))
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("tokenType", "ACCESS")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void assertError(
            final ResponseEntity<JsonNode> response,
            final HttpStatus expectedStatus,
            final String expectedCode
    ) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asText()).isEqualTo(expectedCode);
        assertThat(response.getBody().path("errors").isArray()).isTrue();
        assertThat(response.getBody().has("trace")).isFalse();
    }
}
