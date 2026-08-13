package com.jachwisunbae.property.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.AcceptanceTest;
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

class ActiveChecklistAcceptanceTest extends AcceptanceTest {

    private static final String PROPERTIES_URL = "/api/properties";
    private static final String CHECKLISTS_URL = "/api/checklists";
    private static final String LOGIN_URL = "/api/auth/google";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("UPDATE check_items SET is_active = TRUE");
    }

    @DisplayName("회원은 API-401·402로 세 단계 연결을 설정·교체·해제하고 기존 조회·수정·삭제 API에서 실제 상태를 본다")
    @Test
    void manageActiveChecklistsThroughApi() {
        final String token = login("active-api-owner");
        final long firstPropertyId = createProperty(token, "첫 매물");
        final long secondPropertyId = createProperty(token, "둘째 매물");
        final long phoneId = createChecklist(token, "전화", "ONLINE_PHONE", 201L);
        final long siteId = createChecklist(token, "현장", "ON_SITE", 101L, 102L);
        final long replacementId = createChecklist(token, "현장 교체", "ON_SITE", 103L);
        final long contractId = createChecklist(token, "계약", "PRE_CONTRACT", 301L);

        final JsonNode phone = assign(token, firstPropertyId, "ONLINE_PHONE", phoneId)
                .getBody().path("data");
        final JsonNode site = assign(token, firstPropertyId, "ON_SITE", siteId)
                .getBody().path("data");
        assign(token, firstPropertyId, "PRE_CONTRACT", contractId);
        assign(token, secondPropertyId, "ON_SITE", siteId);
        final ResponseEntity<JsonNode> sameAssignment = assign(token, firstPropertyId, "ON_SITE", siteId);

        assertThat(phone.path("propertyId").asLong()).isEqualTo(firstPropertyId);
        assertThat(phone.path("stage").asText()).isEqualTo("ONLINE_PHONE");
        assertThat(site.path("checklistId").asLong()).isEqualTo(siteId);
        assertThat(site.path("name").asText()).isEqualTo("현장");
        assertThat(site.path("itemCount").asInt()).isEqualTo(2);
        assertThat(sameAssignment.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode detail = getProperty(token, firstPropertyId);
        assertThat(detail.path("activeChecklists")).hasSize(3);
        assertThat(detail.path("activeChecklists").findValuesAsText("stage"))
                .containsExactly("ONLINE_PHONE", "ON_SITE", "PRE_CONTRACT");
        assertThat(detail.path("deletionImpact").path("activeChecklistCount").asInt()).isEqualTo(3);
        assertThat(checklistDetail(token, siteId).path("assignedPropertyCount").asInt()).isEqualTo(2);

        final ResponseEntity<JsonNode> replaced = assign(token, firstPropertyId, "ON_SITE", replacementId);

        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checklistDetail(token, siteId).path("assignedPropertyCount").asInt()).isOne();
        assertThat(checklistDetail(token, replacementId).path("assignedPropertyCount").asInt()).isOne();
        final JsonNode siteList = exchange(
                CHECKLISTS_URL + "?stage=ON_SITE&page=0&size=20",
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data").path("content");
        assertThat(siteList.findValuesAsText("assignedPropertyCount"))
                .containsExactlyInAnyOrder("1", "1");

        exchange(
                CHECKLISTS_URL + "/" + replacementId,
                HttpMethod.PUT,
                token,
                Map.of("name", "현장 수정", "checkItemIds", List.of(102L, 103L))
        );
        detail = getProperty(token, firstPropertyId);
        final JsonNode onSite = findStage(detail.path("activeChecklists"), "ON_SITE");
        assertThat(onSite.path("name").asText()).isEqualTo("현장 수정");
        assertThat(onSite.path("itemCount").asInt()).isEqualTo(2);

        assertThat(unassign(token, firstPropertyId, "ON_SITE").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(unassign(token, firstPropertyId, "ON_SITE").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(checklistDetail(token, replacementId).path("assignedPropertyCount").asInt()).isZero();
        assertThat(getProperty(token, firstPropertyId).path("activeChecklists")).hasSize(2);

        assign(token, firstPropertyId, "ON_SITE", replacementId);
        assertThat(exchange(
                CHECKLISTS_URL + "/" + replacementId,
                HttpMethod.DELETE,
                token,
                null
        ).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findStage(getProperty(token, firstPropertyId).path("activeChecklists"), "ON_SITE").isMissingNode())
                .isTrue();

        assign(token, firstPropertyId, "ON_SITE", siteId);
        assertThat(exchange(
                PROPERTIES_URL + "/" + firstPropertyId,
                HttpMethod.DELETE,
                token,
                null
        ).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(checklistDetail(token, siteId).path("assignedPropertyCount").asInt()).isOne();
        assertThat(getProperty(token, secondPropertyId).path("activeChecklists"))
                .singleElement()
                .extracting(node -> node.path("checklistId").asLong())
                .isEqualTo(siteId);
    }

    @DisplayName("API-401·402는 매물과 체크리스트 소유권·존재·단계를 숨기고 실패해도 기존 연결을 유지한다")
    @Test
    void protectOwnershipAndValidateStage() {
        final String ownerToken = login("active-api-real-owner");
        final String otherToken = login("active-api-other-owner");
        final long propertyId = createProperty(ownerToken, "소유 매물");
        final long otherPropertyId = createProperty(otherToken, "타인 매물");
        final long checklistId = createChecklist(ownerToken, "소유 현장", "ON_SITE", 101L);
        final long otherChecklistId = createChecklist(otherToken, "타인 현장", "ON_SITE", 102L);
        final long phoneId = createChecklist(ownerToken, "소유 전화", "ONLINE_PHONE", 201L);
        final long deletedChecklistId = createChecklist(ownerToken, "삭제 현장", "ON_SITE", 103L);
        exchange(CHECKLISTS_URL + "/" + deletedChecklistId, HttpMethod.DELETE, ownerToken, null);
        assign(ownerToken, propertyId, "ON_SITE", checklistId);

        assertError(assign(ownerToken, propertyId, "ON_SITE", otherChecklistId),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(assign(ownerToken, otherPropertyId, "ON_SITE", checklistId),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(assign(ownerToken, otherPropertyId, "ON_SITE", otherChecklistId),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(assign(ownerToken, 999_999L, "ON_SITE", checklistId),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(assign(ownerToken, propertyId, "ON_SITE", 999_999L),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(assign(ownerToken, propertyId, "ON_SITE", deletedChecklistId),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(assign(ownerToken, propertyId, "ON_SITE", phoneId),
                HttpStatus.BAD_REQUEST, "CHECKLIST_STAGE_MISMATCH");
        assertError(assign(ownerToken, propertyId, "ON_SITE", 0),
                HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
        assertError(assign(ownerToken, propertyId, "INVALID", checklistId),
                HttpStatus.BAD_REQUEST, "INVALID_STAGE");
        assertError(unassign(ownerToken, otherPropertyId, "ON_SITE"),
                HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
        assertError(unassign(ownerToken, propertyId, "INVALID"),
                HttpStatus.BAD_REQUEST, "INVALID_STAGE");
        assertError(assign(null, propertyId, "ON_SITE", checklistId),
                HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");

        assertThat(findStage(getProperty(ownerToken, propertyId).path("activeChecklists"), "ON_SITE")
                .path("checklistId").asLong()).isEqualTo(checklistId);
        assertThat(unassign(ownerToken, propertyId, "ONLINE_PHONE").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @DisplayName("OpenAPI JSON에 API-401·402의 Bearer 인증, Body와 성공 응답 계약을 공개한다")
    @Test
    void openApiContainsActiveChecklistContract() {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode operationPath = response.getBody().path("paths")
                .path(PROPERTIES_URL + "/{propertyId}/active-checklists/{stage}");
        assertThat(operationPath.has("put")).isTrue();
        assertThat(operationPath.has("delete")).isTrue();
        assertThat(operationPath.path("put").path("security").isArray()).isTrue();
        assertThat(operationPath.path("delete").path("security").isArray()).isTrue();
        final JsonNode schemas = response.getBody().path("components").path("schemas");
        assertThat(schemas.path("AssignActiveChecklistRequest").path("required").toString())
                .contains("checklistId");
        assertThat(schemas.path("AssignActiveChecklistRequest").path("properties")
                .path("checklistId").path("type").asText()).isEqualTo("integer");
        assertThat(schemas.path("ActiveChecklistResponse").path("properties")
                .path("itemCount").path("type").asText()).isEqualTo("integer");
        assertThat(schemas.path("ActiveChecklistResponse").path("properties").has("propertyId")).isTrue();
        assertThat(schemas.path("PropertyActiveChecklistResponse").path("properties").has("propertyId"))
                .isFalse();
        assertThat(schemas.path("PropertyActiveChecklistResponse").path("properties")
                .path("stage").path("type").asText()).isEqualTo("string");
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

    private ResponseEntity<JsonNode> assign(
            final String token,
            final long propertyId,
            final String stage,
            final long checklistId
    ) {
        return exchange(
                activeUrl(propertyId, stage),
                HttpMethod.PUT,
                token,
                Map.of("checklistId", checklistId)
        );
    }

    private ResponseEntity<JsonNode> unassign(
            final String token,
            final long propertyId,
            final String stage
    ) {
        return exchange(activeUrl(propertyId, stage), HttpMethod.DELETE, token, null);
    }

    private String activeUrl(final long propertyId, final String stage) {
        return PROPERTIES_URL + "/" + propertyId + "/active-checklists/" + stage;
    }

    private JsonNode getProperty(final String token, final long propertyId) {
        return exchange(
                PROPERTIES_URL + "/" + propertyId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
    }

    private JsonNode checklistDetail(final String token, final long checklistId) {
        return exchange(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
    }

    private JsonNode findStage(final JsonNode activeChecklists, final String stage) {
        for (final JsonNode activeChecklist : activeChecklists) {
            if (activeChecklist.path("stage").asText().equals(stage)) {
                return activeChecklist;
            }
        }
        return activeChecklists.path(-1);
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
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
        assertThat(response.getBody().path("errors").isArray()).isTrue();
        assertThat(response.getBody().has("trace")).isFalse();
    }
}
