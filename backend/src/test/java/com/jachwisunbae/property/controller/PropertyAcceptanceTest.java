package com.jachwisunbae.property.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.AcceptanceTest;
import java.util.LinkedHashMap;
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

class PropertyAcceptanceTest extends AcceptanceTest {

    private static final String PROPERTIES_URL = "/api/properties";
    private static final String LOGIN_URL = "/api/auth/google";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteData() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
    }

    @DisplayName("회원은 URL과 일반 텍스트 발견 경로의 매물을 여러 개 등록하고 독립적으로 관리한다")
    @Test
    void manageMultipleProperties() {
        final String token = login("property-owner");
        final ResponseEntity<JsonNode> firstCreate = createProperty(
                token,
                "신림역 원룸",
                10_000_000,
                550_000,
                " https://example.com/listings/10 "
        );
        final ResponseEntity<JsonNode> secondCreate = createProperty(
                token,
                "봉천역 원룸",
                20_000_000,
                650_000,
                " 직방 앱에서 발견 "
        );

        assertThat(firstCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final long firstId = firstCreate.getBody().path("data").path("propertyId").asLong();
        final long secondId = secondCreate.getBody().path("data").path("propertyId").asLong();
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(firstCreate.getHeaders().getLocation()).hasPath("/api/properties/" + firstId);
        assertThat(firstCreate.getBody().path("data").path("discoverySource").path("type").asText())
                .isEqualTo("URL");
        assertThat(secondCreate.getBody().path("data").path("discoverySource").path("type").asText())
                .isEqualTo("TEXT");

        final ResponseEntity<JsonNode> list = exchange(PROPERTIES_URL, HttpMethod.GET, token, null);
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().path("data").path("totalElements").asLong()).isEqualTo(2L);
        assertThat(list.getBody().path("data").path("content").get(0).path("propertyId").asLong())
                .isEqualTo(secondId);
        assertThat(list.getBody().path("data").path("content").get(0).path("photoCount").asInt()).isZero();
        assertThat(list.getBody().path("data").path("content").get(0).path("recentVisit").isNull()).isTrue();

        final ResponseEntity<JsonNode> detail = exchange(
                PROPERTIES_URL + "/" + firstId,
                HttpMethod.GET,
                token,
                null
        );
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().path("data").path("name").asText()).isEqualTo("신림역 원룸");
        assertThat(detail.getBody().path("data").path("memo").path("content").asText()).isEmpty();
        assertThat(detail.getBody().path("data").path("memo").path("additionalMemo").asText()).isEmpty();
        assertThat(detail.getBody().path("data").path("memo").path("viewingSchedule").asText()).isEmpty();
        assertThat(detail.getBody().path("data").path("memo").path("savedAt").isTextual()).isTrue();
        assertThat(detail.getBody().path("data").path("activeChecklists").isEmpty()).isTrue();
        assertThat(detail.getBody().path("data").path("photoPreview").path("totalCount").asInt()).isZero();
        assertThat(detail.getBody().path("data").path("deletionImpact").path("visitCount").asInt()).isZero();

        final ResponseEntity<JsonNode> update = exchange(
                PROPERTIES_URL + "/" + firstId,
                HttpMethod.PATCH,
                token,
                Map.of("name", "신림역 원룸 2차 방문", "monthlyRentAmount", 530_000)
        );
        assertThat(update.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(update.getBody().path("data").path("name").asText()).isEqualTo("신림역 원룸 2차 방문");
        assertThat(update.getBody().path("data").path("depositAmount").asLong()).isEqualTo(10_000_000L);
        assertThat(update.getBody().path("data").path("monthlyRentAmount").asLong()).isEqualTo(530_000L);

        final ResponseEntity<JsonNode> firstMemo = exchange(
                PROPERTIES_URL + "/" + firstId + "/memo",
                HttpMethod.PUT,
                token,
                Map.of("content", "채광은 좋음")
        );
        final ResponseEntity<JsonNode> secondMemo = exchange(
                PROPERTIES_URL + "/" + firstId + "/memo",
                HttpMethod.PUT,
                token,
                Map.of("content", "골목 소음 다시 확인")
        );
        assertThat(firstMemo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondMemo.getBody().path("data").has("version")).isFalse();
        assertThat(secondMemo.getBody().path("data").path("content").asText()).isEqualTo("골목 소음 다시 확인");
        assertThat(secondMemo.getBody().path("data").path("additionalMemo").asText())
                .isEqualTo("골목 소음 다시 확인");

        final ResponseEntity<JsonNode> secondDetail = exchange(
                PROPERTIES_URL + "/" + secondId,
                HttpMethod.GET,
                token,
                null
        );
        assertThat(secondDetail.getBody().path("data").path("name").asText()).isEqualTo("봉천역 원룸");
        assertThat(secondDetail.getBody().path("data").path("memo").path("content").asText()).isEmpty();

        final ResponseEntity<JsonNode> delete = exchange(
                PROPERTIES_URL + "/" + firstId,
                HttpMethod.DELETE,
                token,
                null
        );
        assertThat(delete.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertError(
                exchange(PROPERTIES_URL + "/" + firstId, HttpMethod.GET, token, null),
                HttpStatus.NOT_FOUND,
                "PROPERTY_NOT_FOUND"
        );
        assertError(
                exchange(PROPERTIES_URL + "/" + firstId, HttpMethod.PATCH, token, Map.of("name", "삭제 후 수정")),
                HttpStatus.NOT_FOUND,
                "PROPERTY_NOT_FOUND"
        );
        assertError(
                exchange(PROPERTIES_URL + "/" + firstId + "/memo", HttpMethod.PUT, token, Map.of("content", "삭제 후 메모")),
                HttpStatus.NOT_FOUND,
                "PROPERTY_NOT_FOUND"
        );
        assertError(
                exchange(PROPERTIES_URL + "/" + firstId, HttpMethod.DELETE, token, null),
                HttpStatus.NOT_FOUND,
                "PROPERTY_NOT_FOUND"
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM properties", Long.class)).isEqualTo(1L);
    }

    @DisplayName("구조화 사전 메모를 전체 저장·조회하고 legacy content 수정 시 일곱 필드를 보존한다")
    @Test
    void saveStructuredMemoAndPreserveItForLegacyRequest() {
        final String token = login("structured-memo-owner");
        final long propertyId = createProperty(token, "구조화 메모 매물", 1, 2, "발견 경로")
                .getBody().path("data").path("propertyId").asLong();

        final ResponseEntity<JsonNode> structured = exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                structuredMemo("방문 일정", "구조화 추가 메모")
        );

        assertThat(structured.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode structuredMemo = structured.getBody().path("data");
        assertThat(structuredMemo.path("viewingSchedule").asText()).isEqualTo("방문 일정");
        assertThat(structuredMemo.path("moveInAvailability").asText()).isEqualTo("입주 가능일");
        assertThat(structuredMemo.path("additionalMemo").asText()).isEqualTo("구조화 추가 메모");
        assertThat(structuredMemo.path("content").asText()).isEqualTo("구조화 추가 메모");
        assertThat(structuredMemo.path("savedAt").isTextual()).isTrue();
        assertThat(structuredMemo.has("version")).isFalse();
        assertThat(structuredMemo.has("expectedVersion")).isFalse();

        final JsonNode detailMemo = exchange(
                PROPERTIES_URL + "/" + propertyId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data").path("memo");
        assertThat(detailMemo).isEqualTo(structuredMemo);

        final JsonNode legacyMemo = exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                Map.of("content", "legacy 수정 메모")
        ).getBody().path("data");

        assertThat(legacyMemo.path("viewingSchedule").asText()).isEqualTo("방문 일정");
        assertThat(legacyMemo.path("governmentSupport").asText()).isEqualTo("정부 지원");
        assertThat(legacyMemo.path("additionalMemo").asText()).isEqualTo("legacy 수정 메모");
        assertThat(legacyMemo.path("content").asText()).isEqualTo("legacy 수정 메모");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT memo FROM properties WHERE id = ?",
                String.class,
                propertyId
        )).isEqualTo("legacy 수정 메모");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT additional_memo FROM property_pre_visit_memos WHERE property_id = ?",
                String.class,
                propertyId
        )).isEqualTo("legacy 수정 메모");
    }

    @DisplayName("구조화 사전 메모의 여덟 빈 문자열은 전체 메모 지우기로 저장한다")
    @Test
    void clearStructuredMemo() {
        final String token = login("clear-memo-owner");
        final long propertyId = createProperty(token, "메모 지우기 매물", 1, 2, "발견 경로")
                .getBody().path("data").path("propertyId").asLong();
        exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                structuredMemo("방문 일정", "추가 메모")
        );

        final JsonNode cleared = exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                structuredMemo("", "")
        ).getBody().path("data");

        assertThat(cleared.path("viewingSchedule").asText()).isEmpty();
        assertThat(cleared.path("moveInAvailability").asText()).isEmpty();
        assertThat(cleared.path("governmentSupport").asText()).isEmpty();
        assertThat(cleared.path("additionalMemo").asText()).isEmpty();
        assertThat(cleared.path("content").asText()).isEmpty();
        assertThat(cleared.path("savedAt").isTextual()).isTrue();
    }

    @DisplayName("목록은 인증 회원 매물만 반환하고 빈 목록과 이름 검색을 지원한다")
    @Test
    void listOnlyOwnedProperties() {
        final String ownerToken = login("list-owner");
        final String emptyMemberToken = login("list-empty-member");
        createProperty(ownerToken, "신림역 원룸", 1, 2, "신림 앱");
        createProperty(ownerToken, "봉천역 원룸", 3, 4, "봉천 앱");

        final ResponseEntity<JsonNode> searched = exchange(
                PROPERTIES_URL + "?query=신림&page=0&size=20",
                HttpMethod.GET,
                ownerToken,
                null
        );
        final ResponseEntity<JsonNode> empty = exchange(PROPERTIES_URL, HttpMethod.GET, emptyMemberToken, null);

        assertThat(searched.getBody().path("data").path("content")).hasSize(1);
        assertThat(searched.getBody().path("data").path("content").get(0).path("name").asText())
                .isEqualTo("신림역 원룸");
        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(empty.getBody().path("data").path("content").isEmpty()).isTrue();
        assertThat(empty.getBody().path("data").path("totalElements").asLong()).isZero();
        assertThat(empty.getBody().path("data").path("totalPages").asLong()).isZero();
        assertThat(empty.getBody().path("data").path("hasNext").asBoolean()).isFalse();
    }

    @DisplayName("다른 회원의 매물은 모든 조회·변경 경로에서 찾을 수 없음으로 숨기고 원본을 유지한다")
    @Test
    void protectOwnership() {
        final String ownerToken = login("ownership-owner");
        final String otherToken = login("ownership-other");
        final long propertyId = createProperty(
                ownerToken,
                "소유 매물",
                10_000_000,
                500_000,
                "원래 경로"
        ).getBody().path("data").path("propertyId").asLong();

        assertError(exchange(PROPERTIES_URL + "/" + propertyId, HttpMethod.GET, otherToken, null),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(exchange(PROPERTIES_URL + "/" + propertyId, HttpMethod.PATCH, otherToken,
                        Map.of("name", "변조 이름")),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(exchange(PROPERTIES_URL + "/" + propertyId + "/memo", HttpMethod.PUT, otherToken,
                        Map.of("content", "변조 메모")),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(exchange(PROPERTIES_URL + "/" + propertyId, HttpMethod.DELETE, otherToken, null),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");

        final JsonNode ownerDetail = exchange(
                PROPERTIES_URL + "/" + propertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data");
        assertThat(ownerDetail.path("name").asText()).isEqualTo("소유 매물");
        assertThat(ownerDetail.path("memo").path("content").asText()).isEmpty();
    }

    @DisplayName("필수 입력·금액·PATCH·메모 검증 실패는 데이터를 변경하거나 민감한 원문을 노출하지 않는다")
    @Test
    void validatePropertyRequests() {
        final String token = login("validation-owner");
        final ResponseEntity<JsonNode> missingField = exchange(
                PROPERTIES_URL,
                HttpMethod.POST,
                token,
                Map.of("name", "매물", "depositAmount", -1, "monthlyRentAmount", 500_000)
        );
        assertError(missingField, HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
        assertThat(missingField.getBody().toString()).doesNotContain("discoverySource\":null");

        final long propertyId = createProperty(token, "검증 매물", 1, 2, "비공개 발견 경로")
                .getBody().path("data").path("propertyId").asLong();
        assertError(exchange(PROPERTIES_URL + "/" + propertyId, HttpMethod.PATCH, token, Map.of()),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
        assertError(exchangeRaw(PROPERTIES_URL + "/" + propertyId, HttpMethod.PATCH, token,
                        "{\"name\":null}"),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST");

        final String longMemo = "민감메모".repeat(1_251);
        final ResponseEntity<JsonNode> memoError = exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                Map.of("content", longMemo)
        );
        assertError(memoError, HttpStatus.BAD_REQUEST, "PROPERTY_MEMO_INVALID");
        assertThat(memoError.getBody().toString()).doesNotContain(longMemo);
        assertError(exchange(
                        PROPERTIES_URL + "/" + propertyId + "/memo",
                        HttpMethod.PUT,
                        token,
                        Map.of("viewingSchedule", "일정")
                ),
                HttpStatus.BAD_REQUEST,
                "PROPERTY_MEMO_INVALID");
        assertError(exchangeRaw(
                        PROPERTIES_URL + "/" + propertyId + "/memo",
                        HttpMethod.PUT,
                        token,
                        """
                        {
                          "viewingSchedule": null,
                          "moveInAvailability": "",
                          "provisionalDeposit": "",
                          "roomOptions": "",
                          "maintenanceAndUtilities": "",
                          "commuteTime": "",
                          "governmentSupport": "",
                          "additionalMemo": ""
                        }
                        """
                ),
                HttpStatus.BAD_REQUEST,
                "PROPERTY_MEMO_INVALID");
        final ResponseEntity<JsonNode> ambiguousMemo = exchangeRaw(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                """
                {"content":"혼합 민감 메모", "viewingSchedule":null}
                """
        );
        assertError(ambiguousMemo, HttpStatus.BAD_REQUEST, "AMBIGUOUS_MEMO_CONTENT");
        assertThat(ambiguousMemo.getBody().toString()).doesNotContain("혼합 민감 메모");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT memo FROM properties WHERE id = ?",
                String.class,
                propertyId
        )).isEmpty();
    }

    @DisplayName("문자 수 경계는 UTF-16 길이가 아닌 유니코드 코드포인트 기준으로 검증한다")
    @Test
    void acceptUnicodeCodePointBoundaries() {
        final String token = login("unicode-owner");
        final String name = "🏠".repeat(50);
        final String discoverySource = "🔎".repeat(500);
        final String memo = "📝".repeat(5_000);
        final String structuredField = "📅".repeat(200);

        final ResponseEntity<JsonNode> created = createProperty(token, name, 0, 0, discoverySource);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final long propertyId = created.getBody().path("data").path("propertyId").asLong();

        final ResponseEntity<JsonNode> savedMemo = exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                Map.of("content", memo)
        );
        assertThat(savedMemo.getStatusCode()).isEqualTo(HttpStatus.OK);
        final ResponseEntity<JsonNode> savedStructuredMemo = exchange(
                PROPERTIES_URL + "/" + propertyId + "/memo",
                HttpMethod.PUT,
                token,
                structuredMemo(structuredField, memo)
        );
        assertThat(savedStructuredMemo.getStatusCode()).isEqualTo(HttpStatus.OK);

        final JsonNode detail = exchange(
                PROPERTIES_URL + "/" + propertyId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(detail.path("name").asText()).isEqualTo(name);
        assertThat(detail.path("discoverySource").path("value").asText()).isEqualTo(discoverySource);
        assertThat(detail.path("memo").path("content").asText()).isEqualTo(memo);
        assertThat(detail.path("memo").path("viewingSchedule").asText()).isEqualTo(structuredField);
    }

    @DisplayName("페이지 범위와 검색어 길이가 잘못되면 계약된 오류를 반환한다")
    @Test
    void rejectInvalidListRequest() {
        final String token = login("page-owner");

        assertError(exchange(PROPERTIES_URL + "?page=-1", HttpMethod.GET, token, null),
                HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST");
        assertError(exchange(PROPERTIES_URL + "?size=101", HttpMethod.GET, token, null),
                HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST");
        assertError(exchange(PROPERTIES_URL + "?query=" + "가".repeat(51), HttpMethod.GET, token, null),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
    }

    @DisplayName("인증이 없거나 JWT 형식이 잘못된 매물 요청을 거부한다")
    @Test
    void requireAuthentication() {
        assertError(exchange(PROPERTIES_URL, HttpMethod.GET, null, null),
                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
        assertError(exchange(PROPERTIES_URL, HttpMethod.GET, "not-a-jwt", null),
                HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_INVALID");
    }

    @DisplayName("OpenAPI에 API-101부터 API-106까지의 Bearer 인증 계약을 공개한다")
    @Test
    void openApiContainsPropertyContract() {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode paths = response.getBody().path("paths");
        assertThat(paths.path(PROPERTIES_URL).has("get")).isTrue();
        assertThat(paths.path(PROPERTIES_URL).has("post")).isTrue();
        assertThat(paths.path(PROPERTIES_URL + "/{propertyId}").has("get")).isTrue();
        assertThat(paths.path(PROPERTIES_URL + "/{propertyId}").has("patch")).isTrue();
        assertThat(paths.path(PROPERTIES_URL + "/{propertyId}").has("delete")).isTrue();
        assertThat(paths.path(PROPERTIES_URL + "/{propertyId}/memo").has("put")).isTrue();
        assertThat(paths.path(PROPERTIES_URL).path("get").path("security").isArray()).isTrue();
        final JsonNode updateProperties = response.getBody()
                .path("components")
                .path("schemas")
                .path("UpdatePropertyRequest")
                .path("properties");
        assertThat(updateProperties.path("name").path("type").asText()).isEqualTo("string");
        assertThat(updateProperties.path("depositAmount").path("type").asText()).isEqualTo("integer");
        final JsonNode memoRequestProperties = response.getBody()
                .path("components")
                .path("schemas")
                .path("SavePropertyMemoRequest")
                .path("properties");
        assertThat(memoRequestProperties.has("viewingSchedule")).isTrue();
        assertThat(memoRequestProperties.has("governmentSupport")).isTrue();
        assertThat(memoRequestProperties.has("additionalMemo")).isTrue();
        assertThat(memoRequestProperties.path("content").path("deprecated").asBoolean()).isTrue();
        assertThat(memoRequestProperties.has("expectedVersion")).isFalse();
        final JsonNode memoResponseProperties = response.getBody()
                .path("components")
                .path("schemas")
                .path("PropertyMemoResponse")
                .path("properties");
        assertThat(memoResponseProperties.has("viewingSchedule")).isTrue();
        assertThat(memoResponseProperties.has("additionalMemo")).isTrue();
        assertThat(memoResponseProperties.has("content")).isTrue();
        assertThat(memoResponseProperties.has("savedAt")).isTrue();
        assertThat(memoResponseProperties.has("version")).isFalse();
    }

    private Map<String, String> structuredMemo(
            final String viewingSchedule,
            final String additionalMemo
    ) {
        final Map<String, String> memo = new LinkedHashMap<>();
        memo.put("viewingSchedule", viewingSchedule);
        memo.put("moveInAvailability", viewingSchedule.isEmpty() ? "" : "입주 가능일");
        memo.put("provisionalDeposit", viewingSchedule.isEmpty() ? "" : "가계약금");
        memo.put("roomOptions", viewingSchedule.isEmpty() ? "" : "방 옵션");
        memo.put("maintenanceAndUtilities", viewingSchedule.isEmpty() ? "" : "관리비와 공과금");
        memo.put("commuteTime", viewingSchedule.isEmpty() ? "" : "통학 시간");
        memo.put("governmentSupport", viewingSchedule.isEmpty() ? "" : "정부 지원");
        memo.put("additionalMemo", additionalMemo);
        return memo;
    }

    private String login(final String subject) {
        final Map<String, String> request = Map.of(
                "authorizationCode", "valid-code:" + subject,
                "codeVerifier", CODE_VERIFIER,
                "nonce", "valid-nonce",
                "redirectUri", REDIRECT_URI
        );
        return exchange(LOGIN_URL, HttpMethod.POST, null, request)
                .getBody()
                .path("data")
                .path("accessToken")
                .asText();
    }

    private ResponseEntity<JsonNode> createProperty(
            final String token,
            final String name,
            final long depositAmount,
            final long monthlyRentAmount,
            final String discoverySource
    ) {
        return exchange(
                PROPERTIES_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", name,
                        "depositAmount", depositAmount,
                        "monthlyRentAmount", monthlyRentAmount,
                        "discoverySource", discoverySource
                )
        );
    }

    private ResponseEntity<JsonNode> exchange(
            final String url,
            final HttpMethod method,
            final String token,
            final Object body
    ) {
        final HttpHeaders headers = headers(token);
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private ResponseEntity<JsonNode> exchangeRaw(
            final String url,
            final HttpMethod method,
            final String token,
            final String body
    ) {
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers(token)), JsonNode.class);
    }

    private HttpHeaders headers(final String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private void assertError(
            final ResponseEntity<JsonNode> response,
            final HttpStatus status,
            final String code
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
        assertThat(response.getBody().path("errors").isArray()).isTrue();
        assertThat(response.getBody().has("trace")).isFalse();
    }
}
