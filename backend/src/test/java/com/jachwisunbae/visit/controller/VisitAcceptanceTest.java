package com.jachwisunbae.visit.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.AcceptanceTest;
import java.util.List;
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

class VisitAcceptanceTest extends AcceptanceTest {

    private static final String LOGIN_URL = "/api/auth/google";
    private static final String PROPERTIES_URL = "/api/properties";
    private static final String CHECKLISTS_URL = "/api/checklists";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
    }

    @DisplayName("API-501~506은 복수 방문·독립 상태와 메모 저장·완료와 매물 요약을 하나의 계약으로 제공한다")
    @Test
    void manageVisitsThroughApi() {
        final String token = login("visit-api-owner");
        final long propertyId = createProperty(token, "방문 매물");
        final long phoneId = createChecklist(token, "전화", "ONLINE_PHONE", 201L);
        final long siteId = createChecklist(token, "현장", "ON_SITE", 101L, 102L);
        assign(token, propertyId, "ONLINE_PHONE", phoneId);
        assign(token, propertyId, "ON_SITE", siteId);

        final ResponseEntity<JsonNode> firstCreate = exchange(
                PROPERTIES_URL + "/" + propertyId + "/visits",
                HttpMethod.POST,
                token,
                null
        );
        final ResponseEntity<JsonNode> secondCreate = exchange(
                PROPERTIES_URL + "/" + propertyId + "/visits",
                HttpMethod.POST,
                token,
                null
        );
        assertThat(firstCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final long firstVisitId = firstCreate.getBody().path("data").path("visitId").asLong();
        final JsonNode secondVisit = secondCreate.getBody().path("data");
        final long secondVisitId = secondVisit.path("visitId").asLong();
        final long firstVisitItemId = firstCreate.getBody().path("data").path("stages").get(0)
                .path("items").get(0).path("visitItemId").asLong();
        final long secondVisitItemId = secondVisit.path("stages").get(0)
                .path("items").get(0).path("visitItemId").asLong();
        assertThat(firstCreate.getHeaders().getLocation()).hasPath("/api/visits/" + firstVisitId);
        assertThat(secondVisit.path("stages")).hasSize(2);
        assertThat(secondVisit.path("summary").path("totalCount").asInt()).isEqualTo(3);
        assertThat(secondVisit.path("summary").path("unconfirmedCount").asInt()).isEqualTo(3);
        final JsonNode initialItem = secondVisit.path("stages").get(0).path("items").get(0);
        assertThat(initialItem.path("statusVersion").asLong()).isZero();
        assertThat(initialItem.path("version").asLong()).isZero();
        assertThat(initialItem.path("statusSavedAt").asText()).isEqualTo(initialItem.path("savedAt").asText());
        assertThat(initialItem.path("inlineMemo").asText()).isEmpty();
        assertThat(initialItem.path("memoVersion").asLong()).isZero();
        assertThat(initialItem.path("memoSavedAt").isNull()).isTrue();

        final JsonNode list = exchange(
                PROPERTIES_URL + "/" + propertyId + "/visits?page=0&size=1",
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(list.path("content")).hasSize(1);
        assertThat(list.path("content").get(0).path("visitId").asLong()).isEqualTo(secondVisitId);
        assertThat(list.path("totalElements").asLong()).isEqualTo(2);
        assertThat(list.path("hasNext").asBoolean()).isTrue();

        final ResponseEntity<JsonNode> saved = exchange(
                "/api/visits/" + secondVisitId + "/items/" + secondVisitItemId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "CAUTION", "expectedStatusVersion", 0)
        );
        assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode savedItem = saved.getBody().path("data").path("item");
        assertThat(savedItem.path("statusVersion").asLong()).isOne();
        assertThat(savedItem.path("version").asLong()).isOne();
        assertThat(savedItem.path("statusSavedAt").asText()).isEqualTo(savedItem.path("savedAt").asText());
        assertThat(savedItem.has("inlineMemo")).isFalse();
        assertThat(saved.getBody().path("data").path("visitSummary").path("cautionCount").asInt()).isOne();
        assertError(exchange(
                "/api/visits/" + secondVisitId + "/items/" + secondVisitItemId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "GOOD", "expectedVersion", 0)
        ), HttpStatus.CONFLICT, "VISIT_ITEM_STATUS_VERSION_CONFLICT");
        assertError(exchange(
                "/api/visits/" + secondVisitId + "/items/" + firstVisitItemId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "GOOD", "expectedVersion", 0)
        ), HttpStatus.NOT_FOUND, "VISIT_ITEM_NOT_FOUND");

        final ResponseEntity<JsonNode> completed = exchange(
                "/api/visits/" + secondVisitId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "COMPLETED")
        );
        final String completedAt = completed.getBody().path("data").path("completedAt").asText();
        final ResponseEntity<JsonNode> repeated = exchange(
                "/api/visits/" + secondVisitId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "COMPLETED")
        );
        assertThat(completed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completed.getBody().path("data").path("summary").path("unconfirmedCount").asInt())
                .isEqualTo(2);
        assertThat(repeated.getBody().path("data").path("completedAt").asText()).isEqualTo(completedAt);

        final ResponseEntity<JsonNode> afterCompletion = exchange(
                "/api/visits/" + secondVisitId + "/items/" + secondVisitItemId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "GOOD", "expectedVersion", 1)
        );
        assertThat(afterCompletion.getStatusCode()).isEqualTo(HttpStatus.OK);
        final ResponseEntity<JsonNode> memoAfterCompletion = exchange(
                "/api/visits/" + secondVisitId + "/items/" + secondVisitItemId + "/memo",
                HttpMethod.PATCH,
                token,
                Map.of("memo", "  완료 뒤에도 보존할 메모  ", "expectedMemoVersion", 0)
        );
        assertThat(memoAfterCompletion.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode memoData = memoAfterCompletion.getBody().path("data");
        assertThat(memoData.path("visitItemId").asLong()).isEqualTo(secondVisitItemId);
        assertThat(memoData.path("memo").asText()).isEqualTo("  완료 뒤에도 보존할 메모  ");
        assertThat(memoData.path("memoVersion").asLong()).isOne();
        assertThat(memoData.path("memoSavedAt").isTextual()).isTrue();
        assertThat(memoData.has("status")).isFalse();
        assertThat(memoData.has("summary")).isFalse();
        final JsonNode detail = exchange(
                "/api/visits/" + secondVisitId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(detail.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(detail.path("completedAt").asText()).isEqualTo(completedAt);
        assertThat(detail.path("summary").path("goodCount").asInt()).isOne();
        final JsonNode updatedItem = detail.path("stages").get(0).path("items").get(0);
        assertThat(updatedItem.path("status").asText()).isEqualTo("GOOD");
        assertThat(updatedItem.path("statusVersion").asLong()).isEqualTo(2);
        assertThat(updatedItem.path("inlineMemo").asText()).isEqualTo("  완료 뒤에도 보존할 메모  ");
        assertThat(updatedItem.path("memoVersion").asLong()).isOne();

        final JsonNode property = exchange(
                PROPERTIES_URL + "/" + propertyId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(property.path("recentVisit").path("visitId").asLong()).isEqualTo(secondVisitId);
        assertThat(property.path("recentVisit").path("summary").path("goodCount").asInt()).isOne();
        assertThat(property.path("deletionImpact").path("visitCount").asInt()).isEqualTo(2);
        final JsonNode propertyList = exchange(PROPERTIES_URL, HttpMethod.GET, token, null)
                .getBody().path("data").path("content").get(0);
        assertThat(propertyList.path("recentVisit").path("visitId").asLong()).isEqualTo(secondVisitId);

        assertThat(exchange(PROPERTIES_URL + "/" + propertyId, HttpMethod.DELETE, token, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertError(exchange("/api/visits/" + secondVisitId, HttpMethod.GET, token, null),
                HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        assertThat(exchange(CHECKLISTS_URL + "/" + siteId, HttpMethod.GET, token, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @DisplayName("API-502·503은 CUSTOM origin·nullable 출처·질문 순서를 스냅샷하고 원본 삭제 뒤에도 보존한다")
    @Test
    void snapshotCustomChecklistItemsThroughApi() {
        final String token = login("custom-visit-api-owner");
        final long propertyId = createProperty(token, "CUSTOM 방문 매물");
        final ResponseEntity<JsonNode> checklistResponse = exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", "혼합 현장",
                        "stage", "ON_SITE",
                        "items", List.of(
                                Map.of("origin", "PROVIDED", "sourceCheckItemId", 101L),
                                Map.of("origin", "CUSTOM", "question", "창틀 곰팡이는 괜찮은가?")
                        )
                )
        );
        final JsonNode checklist = checklistResponse.getBody().path("data");
        final long checklistId = checklist.path("checklistId").asLong();
        final long customChecklistItemId = checklist.path("items").get(1).path("checklistItemId").asLong();
        assign(token, propertyId, "ON_SITE", checklistId);

        final ResponseEntity<JsonNode> created = exchange(
                PROPERTIES_URL + "/" + propertyId + "/visits",
                HttpMethod.POST,
                token,
                null
        );
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode visit = created.getBody().path("data");
        final long visitId = visit.path("visitId").asLong();
        final JsonNode customSnapshot = visit.path("stages").get(0).path("items").get(1);
        assertThat(customSnapshot.path("origin").asText()).isEqualTo("CUSTOM");
        assertThat(customSnapshot.path("sourceChecklistItemId").asLong()).isEqualTo(customChecklistItemId);
        assertThat(customSnapshot.path("sourceCheckItemId").isNull()).isTrue();
        assertThat(customSnapshot.path("question").asText()).isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(customSnapshot.path("guide").isNull()).isTrue();
        assertThat(customSnapshot.path("order").asInt()).isEqualTo(2);
        assertThat(customSnapshot.path("status").asText()).isEqualTo("UNCONFIRMED");
        assertThat(customSnapshot.path("statusVersion").asLong()).isZero();
        assertThat(customSnapshot.path("version").asLong()).isZero();
        assertThat(customSnapshot.path("inlineMemo").asText()).isEmpty();
        assertThat(customSnapshot.path("memoVersion").asLong()).isZero();
        assertThat(customSnapshot.path("memoSavedAt").isNull()).isTrue();

        assertThat(exchange(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.DELETE,
                token,
                null
        ).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        final JsonNode preserved = exchange(
                "/api/visits/" + visitId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        final JsonNode preservedCustom = preserved.path("stages").get(0).path("items").get(1);
        assertThat(preserved.path("stages").get(0).path("sourceChecklistId").isNull()).isTrue();
        assertThat(preservedCustom.path("sourceChecklistItemId").isNull()).isTrue();
        assertThat(preservedCustom.path("origin").asText()).isEqualTo("CUSTOM");
        assertThat(preservedCustom.path("sourceCheckItemId").isNull()).isTrue();
        assertThat(preservedCustom.path("question").asText()).isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(preservedCustom.path("order").asInt()).isEqualTo(2);

        final long customVisitItemId = preservedCustom.path("visitItemId").asLong();
        final ResponseEntity<JsonNode> customMemo = exchange(
                "/api/visits/" + visitId + "/items/" + customVisitItemId + "/memo",
                HttpMethod.PATCH,
                token,
                Map.of("memo", "원본 삭제 뒤 CUSTOM 메모", "expectedMemoVersion", 0)
        );
        assertThat(customMemo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(customMemo.getBody().path("data").path("memo").asText())
                .isEqualTo("원본 삭제 뒤 CUSTOM 메모");
        final JsonNode preservedWithMemo = exchange(
                "/api/visits/" + visitId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data").path("stages").get(0).path("items").get(1);
        assertThat(preservedWithMemo.path("sourceChecklistItemId").isNull()).isTrue();
        assertThat(preservedWithMemo.path("inlineMemo").asText()).isEqualTo("원본 삭제 뒤 CUSTOM 메모");
        assertThat(preservedWithMemo.path("memoVersion").asLong()).isOne();
    }

    @DisplayName("방문 API는 인증·소유권·상태·페이지·활성 체크리스트 경계를 계약된 오류로 처리한다")
    @Test
    void validateVisitBoundaries() {
        final String ownerToken = login("visit-boundary-owner");
        final String otherToken = login("visit-boundary-other");
        final long propertyId = createProperty(ownerToken, "경계 매물");

        assertError(exchange(PROPERTIES_URL + "/" + propertyId + "/visits", HttpMethod.POST, ownerToken, null),
                HttpStatus.BAD_REQUEST, "ACTIVE_CHECKLIST_REQUIRED");
        assertError(exchange(PROPERTIES_URL + "/" + propertyId + "/visits", HttpMethod.GET, otherToken, null),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(exchange(PROPERTIES_URL + "/" + propertyId + "/visits?page=-1", HttpMethod.GET, ownerToken, null),
                HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST");
        assertError(exchange(PROPERTIES_URL + "/" + propertyId + "/visits", HttpMethod.GET, null, null),
                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");

        final long checklistId = createChecklist(ownerToken, "현장", "ON_SITE", 101L);
        assign(ownerToken, propertyId, "ON_SITE", checklistId);
        final JsonNode visit = exchange(
                PROPERTIES_URL + "/" + propertyId + "/visits",
                HttpMethod.POST,
                ownerToken,
                null
        ).getBody().path("data");
        final long visitId = visit.path("visitId").asLong();
        final long itemId = visit.path("stages").get(0).path("items").get(0).path("visitItemId").asLong();
        final JsonNode anotherVisit = exchange(
                PROPERTIES_URL + "/" + propertyId + "/visits",
                HttpMethod.POST,
                ownerToken,
                null
        ).getBody().path("data");
        final long anotherItemId = anotherVisit.path("stages").get(0).path("items").get(0)
                .path("visitItemId").asLong();

        assertError(exchange("/api/visits/" + visitId, HttpMethod.GET, otherToken, null),
                HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + itemId,
                HttpMethod.PATCH,
                ownerToken,
                Map.of("status", "BAD", "expectedVersion", 0)
        ), HttpStatus.BAD_REQUEST, "INVALID_CHECK_STATUS");
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + itemId,
                HttpMethod.PATCH,
                ownerToken,
                Map.of("status", "GOOD", "expectedStatusVersion", 0, "expectedVersion", 1)
        ), HttpStatus.BAD_REQUEST, "AMBIGUOUS_STATUS_VERSION");
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                otherToken,
                Map.of("memo", "타인 메모", "expectedMemoVersion", 0)
        ), HttpStatus.NOT_FOUND, "VISIT_NOT_FOUND");
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + anotherItemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("memo", "다른 방문 항목", "expectedMemoVersion", 0)
        ), HttpStatus.NOT_FOUND, "VISIT_ITEM_NOT_FOUND");
        final ResponseEntity<JsonNode> firstMemo = exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("memo", "첫 메모", "expectedMemoVersion", 0)
        );
        assertThat(firstMemo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstMemo.getBody().path("code").asText()).isEqualTo("SUCCESS");
        final ResponseEntity<JsonNode> memoConflict = exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("memo", "노출되면 안 되는 충돌 메모", "expectedMemoVersion", 0)
        );
        assertError(memoConflict, HttpStatus.CONFLICT, "VISIT_ITEM_MEMO_VERSION_CONFLICT");
        assertThat(memoConflict.getBody().toString()).doesNotContain("노출되면 안 되는 충돌 메모", "첫 메모");
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("memo", "🏠".repeat(201), "expectedMemoVersion", 1)
        ), HttpStatus.BAD_REQUEST, "VISIT_ITEM_MEMO_INVALID");
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("memo", "첫 줄\n둘째 줄", "expectedMemoVersion", 1)
        ), HttpStatus.BAD_REQUEST, "VISIT_ITEM_MEMO_INVALID");
        final Map<String, Object> nullMemo = new LinkedHashMap<>();
        nullMemo.put("memo", null);
        nullMemo.put("expectedMemoVersion", 1);
        assertError(exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                nullMemo
        ), HttpStatus.BAD_REQUEST, "VISIT_ITEM_MEMO_INVALID");
        final ResponseEntity<JsonNode> deletedMemo = exchange(
                "/api/visits/" + visitId + "/items/" + itemId + "/memo",
                HttpMethod.PATCH,
                ownerToken,
                Map.of("memo", "", "expectedMemoVersion", 1)
        );
        assertThat(deletedMemo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deletedMemo.getBody().path("data").path("memo").asText()).isEmpty();
        assertThat(deletedMemo.getBody().path("data").path("memoVersion").asLong()).isEqualTo(2);
        assertError(exchange(
                "/api/visits/" + visitId,
                HttpMethod.PATCH,
                ownerToken,
                Map.of("status", "IN_PROGRESS")
        ), HttpStatus.BAD_REQUEST, "INVALID_VISIT_STATUS");
    }

    @DisplayName("OpenAPI는 API-501~506의 독립 상태·메모·deprecated·Bearer 계약을 공개한다")
    @Test
    void openApiContainsVisitContract() {
        final JsonNode document = restTemplate.getForEntity("/v3/api-docs", JsonNode.class).getBody();
        final JsonNode paths = document.path("paths");

        assertThat(paths.path("/api/properties/{propertyId}/visits").has("get")).isTrue();
        assertThat(paths.path("/api/properties/{propertyId}/visits").has("post")).isTrue();
        assertThat(paths.path("/api/visits/{visitId}").has("get")).isTrue();
        assertThat(paths.path("/api/visits/{visitId}").has("patch")).isTrue();
        assertThat(paths.path("/api/visits/{visitId}/items/{visitItemId}").has("patch")).isTrue();
        assertThat(paths.path("/api/visits/{visitId}/items/{visitItemId}/memo").has("patch")).isTrue();
        assertThat(paths.path("/api/visits/{visitId}").path("patch").path("security").isArray()).isTrue();
        assertThat(paths.path("/api/visits/{visitId}/items/{visitItemId}/memo")
                .path("patch").path("security").isArray()).isTrue();
        final JsonNode updateSchema = document.path("components").path("schemas")
                .path("UpdateVisitItemRequest").path("properties");
        assertThat(updateSchema.path("status").path("type").asText()).isEqualTo("string");
        assertThat(updateSchema.path("expectedStatusVersion").path("minimum").asInt()).isZero();
        assertThat(updateSchema.path("expectedVersion").path("minimum").asInt()).isZero();
        assertThat(updateSchema.path("expectedVersion").path("deprecated").asBoolean()).isTrue();
        final JsonNode memoSchema = document.path("components").path("schemas")
                .path("UpdateVisitItemMemoRequest").path("properties");
        assertThat(memoSchema.path("memo").path("maxLength").asInt()).isEqualTo(200);
        assertThat(memoSchema.path("expectedMemoVersion").path("minimum").asInt()).isZero();
        final JsonNode detailItemSchema = document.path("components").path("schemas")
                .path("VisitDetailItemResponse").path("properties");
        assertThat(detailItemSchema.has("inlineMemo")).isTrue();
        assertThat(detailItemSchema.path("memoSavedAt").path("type").toString()).contains("null");
        assertThat(detailItemSchema.path("version").path("deprecated").asBoolean()).isTrue();
        assertThat(detailItemSchema.path("savedAt").path("deprecated").asBoolean()).isTrue();
        final JsonNode statusItemSchema = document.path("components").path("schemas")
                .path("VisitItemStatusItemResponse").path("properties");
        assertThat(statusItemSchema.has("statusVersion")).isTrue();
        assertThat(statusItemSchema.has("inlineMemo")).isFalse();
        assertThat(statusItemSchema.path("version").path("deprecated").asBoolean()).isTrue();
    }

    private String login(final String subject) {
        return exchange(
                LOGIN_URL,
                HttpMethod.POST,
                null,
                Map.of(
                        "authorizationCode", "valid-code:" + subject,
                        "codeVerifier", CODE_VERIFIER,
                        "nonce", "valid-nonce",
                        "redirectUri", REDIRECT_URI
                )
        ).getBody().path("data").path("accessToken").asText();
    }

    private long createProperty(final String token, final String name) {
        return exchange(
                PROPERTIES_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", name,
                        "depositAmount", 0,
                        "monthlyRentAmount", 0,
                        "discoverySource", "직접 발견"
                )
        ).getBody().path("data").path("propertyId").asLong();
    }

    private long createChecklist(
            final String token,
            final String name,
            final String stage,
            final Long... checkItemIds
    ) {
        return exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of("name", name, "stage", stage, "checkItemIds", List.of(checkItemIds))
        ).getBody().path("data").path("checklistId").asLong();
    }

    private void assign(
            final String token,
            final long propertyId,
            final String stage,
            final long checklistId
    ) {
        final ResponseEntity<JsonNode> response = exchange(
                PROPERTIES_URL + "/" + propertyId + "/active-checklists/" + stage,
                HttpMethod.PUT,
                token,
                Map.of("checklistId", checklistId)
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<JsonNode> exchange(
            final String url,
            final HttpMethod method,
            final String token,
            final Object body
    ) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private void assertError(
            final ResponseEntity<JsonNode> response,
            final HttpStatus status,
            final String code
    ) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
        assertThat(response.getBody().path("errors").isArray()).isTrue();
    }
}
