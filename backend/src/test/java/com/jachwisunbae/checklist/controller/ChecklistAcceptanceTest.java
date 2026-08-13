package com.jachwisunbae.checklist.controller;

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

class ChecklistAcceptanceTest extends AcceptanceTest {

    private static final String CHECK_ITEMS_URL = "/api/check-items";
    private static final String PRESETS_URL = "/api/checklist-presets";
    private static final String CHECKLISTS_URL = "/api/checklists";
    private static final String LOGIN_URL = "/api/auth/google";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteData() {
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("DELETE FROM check_items WHERE id >= 9000");
        jdbcTemplate.update("UPDATE check_items SET is_active = TRUE");
    }

    @DisplayName("회원은 단계별 활성 기준 항목을 검색하고 유형·단계 프리셋의 고정 순서를 조회한다")
    @Test
    void getCatalogAndPreset() {
        final String token = login("catalog-owner");
        jdbcTemplate.update(
                "INSERT INTO check_items (id, stage, question, is_active) VALUES (9001, 'ON_SITE', '경고! 할인 10%_확인', TRUE)"
        );
        jdbcTemplate.update(
                "INSERT INTO check_items (id, stage, question, is_active) VALUES (9002, 'ON_SITE', '할인 100 확인', TRUE)"
        );
        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id = 101");

        final ResponseEntity<JsonNode> searched = exchange(
                CHECK_ITEMS_URL + "?stage=ON_SITE&query=! 할인 10%_&page=0&size=20",
                HttpMethod.GET,
                token,
                null
        );
        final ResponseEntity<JsonNode> empty = exchange(
                CHECK_ITEMS_URL + "?stage=ON_SITE&query=검색결과없음",
                HttpMethod.GET,
                token,
                null
        );
        final ResponseEntity<JsonNode> preset = exchange(
                PRESETS_URL + "?presetType=ONE_ROOM&stage=ONLINE_PHONE",
                HttpMethod.GET,
                token,
                null
        );

        assertThat(searched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(searched.getBody().path("data").path("content")).hasSize(1);
        assertThat(searched.getBody().path("data").path("content").get(0).path("checkItemId").asLong())
                .isEqualTo(9001L);
        assertThat(searched.getBody().path("data").path("totalElements").asLong()).isEqualTo(1L);
        assertThat(empty.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(empty.getBody().path("data").path("content").isEmpty()).isTrue();
        assertThat(empty.getBody().path("data").path("totalElements").asLong()).isZero();
        assertThat(preset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(preset.getBody().path("data").path("presetType").asText()).isEqualTo("ONE_ROOM");
        assertThat(preset.getBody().path("data").path("stage").asText()).isEqualTo("ONLINE_PHONE");
        assertThat(preset.getBody().path("data").path("items")).hasSize(15);
        assertContinuousOrders(preset.getBody().path("data").path("items"));
    }

    @DisplayName("회원은 같은 단계 체크리스트를 여러 개 생성하고 목록·상세·전체 변경·삭제한다")
    @Test
    void manageMultipleChecklists() {
        final String token = login("checklist-api-owner");
        final ResponseEntity<JsonNode> firstCreate = createChecklist(
                token,
                "현장 기본",
                "ON_SITE",
                List.of(101L, 103L)
        );
        final ResponseEntity<JsonNode> secondCreate = createChecklist(
                token,
                "현장 기본",
                "ON_SITE",
                List.of(105L)
        );

        assertThat(firstCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondCreate.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final long firstId = firstCreate.getBody().path("data").path("checklistId").asLong();
        final long secondId = secondCreate.getBody().path("data").path("checklistId").asLong();
        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(firstCreate.getHeaders().getLocation()).hasPath(CHECKLISTS_URL + "/" + firstId);

        final ResponseEntity<JsonNode> changed = exchange(
                CHECKLISTS_URL + "/" + firstId,
                HttpMethod.PUT,
                token,
                Map.of("name", "현장 최종", "checkItemIds", List.of(103L, 102L))
        );
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().path("data").path("stage").asText()).isEqualTo("ON_SITE");
        assertThat(changed.getBody().path("data").path("items").findValuesAsText("checkItemId"))
                .containsExactly("103", "102");
        assertContinuousOrders(changed.getBody().path("data").path("items"));

        final ResponseEntity<JsonNode> list = exchange(
                CHECKLISTS_URL + "?stage=ON_SITE&page=0&size=20",
                HttpMethod.GET,
                token,
                null
        );
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().path("data").path("totalElements").asLong()).isEqualTo(2L);
        assertThat(list.getBody().path("data").path("content").get(0).path("checklistId").asLong())
                .isEqualTo(firstId);
        assertThat(list.getBody().path("data").path("content").get(0).path("itemCount").asInt()).isEqualTo(2);
        assertThat(list.getBody().path("data").path("content").get(0).path("assignedPropertyCount").asInt())
                .isZero();
        final String emptyMemberToken = login("checklist-empty-owner");
        final JsonNode emptyList = exchange(
                CHECKLISTS_URL + "?stage=ON_SITE",
                HttpMethod.GET,
                emptyMemberToken,
                null
        ).getBody().path("data");
        assertThat(emptyList.path("content").isEmpty()).isTrue();
        assertThat(emptyList.path("totalElements").asLong()).isZero();

        final ResponseEntity<JsonNode> detail = exchange(
                CHECKLISTS_URL + "/" + firstId,
                HttpMethod.GET,
                token,
                null
        );
        assertThat(detail.getBody().path("data").path("name").asText()).isEqualTo("현장 최종");
        assertThat(detail.getBody().path("data").path("items").get(0).path("question").asText()).isNotBlank();
        assertThat(detail.getBody().path("data").path("createdAt").asText()).isNotBlank();
        assertThat(detail.getBody().path("data").path("updatedAt").asText()).isNotBlank();

        final ResponseEntity<JsonNode> deleted = exchange(
                CHECKLISTS_URL + "/" + firstId,
                HttpMethod.DELETE,
                token,
                null
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertError(exchange(CHECKLISTS_URL + "/" + firstId, HttpMethod.GET, token, null),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(exchange(CHECKLISTS_URL + "/" + firstId, HttpMethod.PUT, token,
                        Map.of("name", "삭제 후 변경", "checkItemIds", List.of(101L))),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(exchange(CHECKLISTS_URL + "/" + firstId, HttpMethod.DELETE, token, null),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertThat(exchange(CHECKLISTS_URL + "/" + secondId, HttpMethod.GET, token, null).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklist_items WHERE checklist_id = ?",
                Long.class,
                firstId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(72L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklist_presets", Long.class)).isEqualTo(6L);
    }

    @DisplayName("API-304·305·306은 PROVIDED·CUSTOM 혼합 구성과 안정적인 로컬 ID를 제공하고 legacy 유실을 409로 막는다")
    @Test
    void manageV11ChecklistItemsAndProtectLegacyReplacement() {
        final String token = login("checklist-v11-owner");
        final ResponseEntity<JsonNode> created = exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", "혼합 체크리스트",
                        "stage", "ON_SITE",
                        "items", List.of(
                                Map.of("origin", "PROVIDED", "sourceCheckItemId", 101L),
                                Map.of("origin", "CUSTOM", "question", "  창틀 곰팡이는 괜찮은가?  "),
                                Map.of("origin", "PROVIDED", "sourceCheckItemId", 102L)
                        )
                )
        );

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final JsonNode createdData = created.getBody().path("data");
        final long checklistId = createdData.path("checklistId").asLong();
        final long providedId = createdData.path("items").get(0).path("checklistItemId").asLong();
        final long customId = createdData.path("items").get(1).path("checklistItemId").asLong();
        assertThat(createdData.path("items").findValuesAsText("origin"))
                .containsExactly("PROVIDED", "CUSTOM", "PROVIDED");
        assertThat(createdData.path("items").get(1).path("sourceCheckItemId").isNull()).isTrue();
        assertThat(createdData.path("items").get(1).path("checkItemId").isNull()).isTrue();
        assertThat(createdData.path("items").get(1).path("question").asText())
                .isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(createdData.path("items").get(1).path("guide").isNull()).isTrue();

        final ResponseEntity<JsonNode> changed = exchange(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.PUT,
                token,
                Map.of(
                        "name", "혼합 변경",
                        "items", List.of(
                                Map.of(
                                        "origin", "CUSTOM",
                                        "checklistItemId", customId,
                                        "question", "곰팡이 냄새는 괜찮은가?"
                                ),
                                Map.of("origin", "PROVIDED", "sourceCheckItemId", 101L),
                                Map.of("origin", "CUSTOM", "question", "환기 상태는 괜찮은가?")
                        )
                )
        );
        final JsonNode changedItems = changed.getBody().path("data").path("items");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changedItems.get(0).path("checklistItemId").asLong()).isEqualTo(customId);
        assertThat(changedItems.get(1).path("checklistItemId").asLong()).isEqualTo(providedId);
        assertThat(changedItems.get(2).path("checklistItemId").asLong()).isNotIn(customId, providedId);
        assertContinuousOrders(changedItems);

        final ResponseEntity<JsonNode> blocked = exchange(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.PUT,
                token,
                Map.of("name", "legacy 유실", "checkItemIds", List.of(103L))
        );
        assertError(blocked, HttpStatus.CONFLICT, "CHECKLIST_REQUIRES_V11_CLIENT");
        final JsonNode unchanged = exchange(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(unchanged.path("name").asText()).isEqualTo("혼합 변경");
        assertThat(unchanged.path("items")).hasSize(3);

        final JsonNode summary = exchange(
                CHECKLISTS_URL + "?stage=ON_SITE",
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data").path("content").get(0);
        assertThat(summary.path("itemCount").asInt()).isEqualTo(3);
        final JsonNode catalog = exchange(
                CHECK_ITEMS_URL + "?stage=ON_SITE&query=곰팡이 냄새",
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(catalog.path("content").isEmpty()).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM check_items", Long.class)).isEqualTo(72L);
    }

    @DisplayName("API-306은 다른 체크리스트의 CUSTOM 로컬 ID를 404로 숨긴다")
    @Test
    void hideForeignChecklistItemId() {
        final String token = login("checklist-local-id-owner");
        final long firstId = createV11CustomChecklist(token, "첫 체크리스트", "첫 질문인가?");
        final long secondId = createV11CustomChecklist(token, "둘째 체크리스트", "둘째 질문인가?");
        final long foreignItemId = exchange(
                CHECKLISTS_URL + "/" + secondId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data").path("items").get(0).path("checklistItemId").asLong();

        assertError(exchange(
                CHECKLISTS_URL + "/" + firstId,
                HttpMethod.PUT,
                token,
                Map.of(
                        "name", "변조",
                        "items", List.of(Map.of(
                                "origin", "CUSTOM",
                                "checklistItemId", foreignItemId,
                                "question", "변조 질문인가?"
                        ))
                )
        ), HttpStatus.NOT_FOUND, "CHECKLIST_ITEM_NOT_FOUND");
    }

    @DisplayName("다른 회원의 체크리스트는 모든 경로에서 찾을 수 없음으로 숨기고 원본을 유지한다")
    @Test
    void protectOwnership() {
        final String ownerToken = login("checklist-real-owner");
        final String otherToken = login("checklist-api-other");
        final long checklistId = createChecklist(
                ownerToken,
                "소유 체크리스트",
                "ON_SITE",
                List.of(101L)
        ).getBody().path("data").path("checklistId").asLong();

        assertError(exchange(CHECKLISTS_URL + "/" + checklistId, HttpMethod.GET, otherToken, null),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(exchange(CHECKLISTS_URL + "/" + checklistId, HttpMethod.PUT, otherToken,
                        Map.of("name", "변조", "checkItemIds", List.of(102L))),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");
        assertError(exchange(CHECKLISTS_URL + "/" + checklistId, HttpMethod.DELETE, otherToken, null),
                HttpStatus.NOT_FOUND, "CHECKLIST_NOT_FOUND");

        final JsonNode original = exchange(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data");
        assertThat(original.path("name").asText()).isEqualTo("소유 체크리스트");
        assertThat(original.path("items").get(0).path("checkItemId").asLong()).isEqualTo(101L);
    }

    @DisplayName("체크리스트 입력·항목·단계·페이지 오류를 계약된 오류 코드로 반환하고 데이터를 남기지 않는다")
    @Test
    void validateChecklistRequests() {
        final String token = login("checklist-api-validation");

        assertError(createChecklist(token, "빈 목록", "ON_SITE", List.of()),
                HttpStatus.BAD_REQUEST, "CHECKLIST_EMPTY");
        assertError(createChecklist(token, "중복", "ON_SITE", List.of(101L, 101L)),
                HttpStatus.BAD_REQUEST, "CHECKLIST_ITEM_DUPLICATED");
        assertError(createChecklist(token, "단계 불일치", "ON_SITE", List.of(201L)),
                HttpStatus.BAD_REQUEST, "CHECKLIST_ITEM_STAGE_MISMATCH");
        assertError(createChecklist(token, "없는 항목", "ON_SITE", List.of(999_999L)),
                HttpStatus.NOT_FOUND, "CHECK_ITEM_NOT_FOUND");
        assertError(createChecklist(token, "잘못된 단계", "INVALID", List.of(101L)),
                HttpStatus.BAD_REQUEST, "INVALID_STAGE");
        assertError(exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", "표현 충돌",
                        "stage", "ON_SITE",
                        "items", List.of(Map.of("origin", "CUSTOM", "question", "질문인가?")),
                        "checkItemIds", List.of(101L)
                )
        ), HttpStatus.BAD_REQUEST, "CHECKLIST_ITEMS_REPRESENTATION_CONFLICT");
        assertError(exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", "출처 충돌",
                        "stage", "ON_SITE",
                        "items", List.of(Map.of(
                                "origin", "CUSTOM",
                                "sourceCheckItemId", 101L,
                                "question", "질문인가?"
                        ))
                )
        ), HttpStatus.BAD_REQUEST, "CUSTOM_CHECKLIST_ITEM_INVALID");
        assertError(exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", "긴 질문",
                        "stage", "ON_SITE",
                        "items", List.of(Map.of("origin", "CUSTOM", "question", "🏠".repeat(201)))
                )
        ), HttpStatus.BAD_REQUEST, "CUSTOM_CHECKLIST_ITEM_INVALID");
        assertError(exchange(CHECK_ITEMS_URL + "?stage=INVALID", HttpMethod.GET, token, null),
                HttpStatus.BAD_REQUEST, "INVALID_STAGE");
        assertError(exchange(CHECK_ITEMS_URL + "?stage=ON_SITE&page=-1", HttpMethod.GET, token, null),
                HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST");
        assertError(exchange(PRESETS_URL + "?presetType=INVALID&stage=ON_SITE", HttpMethod.GET, token, null),
                HttpStatus.NOT_FOUND, "CHECKLIST_PRESET_NOT_FOUND");

        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id = 101");
        assertError(createChecklist(token, "비활성", "ON_SITE", List.of(101L)),
                HttpStatus.BAD_REQUEST, "CHECK_ITEM_INACTIVE");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM checklists", Long.class)).isZero();
    }

    @DisplayName("인증이 없거나 JWT 형식이 잘못된 체크 항목·프리셋·체크리스트 요청을 거부한다")
    @Test
    void requireAuthentication() {
        for (final String url : List.of(
                CHECK_ITEMS_URL + "?stage=ON_SITE",
                PRESETS_URL + "?presetType=ONE_ROOM&stage=ON_SITE",
                CHECKLISTS_URL + "?stage=ON_SITE"
        )) {
            assertError(exchange(url, HttpMethod.GET, null, null),
                    HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED");
            assertError(exchange(url, HttpMethod.GET, "not-a-jwt", null),
                    HttpStatus.UNAUTHORIZED, "ACCESS_TOKEN_INVALID");
        }
    }

    @DisplayName("OpenAPI JSON에 API-301부터 API-307까지의 Bearer 인증과 항목 배열 계약을 공개한다")
    @Test
    void openApiContainsChecklistContract() {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode paths = response.getBody().path("paths");
        assertThat(paths.path(CHECK_ITEMS_URL).has("get")).isTrue();
        assertThat(paths.path(CHECK_ITEMS_URL).size()).isEqualTo(1);
        assertThat(paths.path(PRESETS_URL).has("get")).isTrue();
        assertThat(paths.path(PRESETS_URL).size()).isEqualTo(1);
        assertThat(paths.path(CHECKLISTS_URL).has("get")).isTrue();
        assertThat(paths.path(CHECKLISTS_URL).has("post")).isTrue();
        assertThat(paths.path(CHECKLISTS_URL + "/{checklistId}").has("get")).isTrue();
        assertThat(paths.path(CHECKLISTS_URL + "/{checklistId}").has("put")).isTrue();
        assertThat(paths.path(CHECKLISTS_URL + "/{checklistId}").has("delete")).isTrue();
        assertThat(paths.path(CHECK_ITEMS_URL).path("get").path("security").isArray()).isTrue();
        assertThat(paths.path(PRESETS_URL).path("get").path("security").isArray()).isTrue();
        assertThat(paths.path(CHECKLISTS_URL).path("post").path("security").isArray()).isTrue();

        final JsonNode schemas = response.getBody().path("components").path("schemas");
        assertThat(schemas.path("CreateChecklistRequest").path("properties")
                .path("checkItemIds").path("type").asText()).isEqualTo("array");
        assertThat(schemas.path("CreateChecklistRequest").path("properties")
                .path("checkItemIds").path("deprecated").asBoolean()).isTrue();
        assertThat(schemas.path("CreateChecklistRequest").path("properties")
                .path("items").path("type").asText()).isEqualTo("array");
        assertThat(schemas.path("ReplaceChecklistRequest").path("properties")
                .path("checkItemIds").path("type").asText()).isEqualTo("array");
        assertThat(schemas.path("ChecklistItemResponse").path("properties")
                .path("checklistItemId").path("format").asText()).isEqualTo("int64");
        assertThat(schemas.path("ChecklistItemResponse").path("properties")
                .path("checkItemId").path("deprecated").asBoolean()).isTrue();
        assertThat(schemas.path("ReplaceChecklistRequest").path("properties").has("stage")).isFalse();
        assertThat(paths.path(CHECKLISTS_URL + "/{checklistId}").path("put")
                .path("responses").has("409")).isTrue();
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

    private ResponseEntity<JsonNode> createChecklist(
            final String token,
            final String name,
            final String stage,
            final List<Long> checkItemIds
    ) {
        return exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of("name", name, "stage", stage, "checkItemIds", checkItemIds)
        );
    }

    private long createV11CustomChecklist(final String token, final String name, final String question) {
        return exchange(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", name,
                        "stage", "ON_SITE",
                        "items", List.of(Map.of("origin", "CUSTOM", "question", question))
                )
        ).getBody().path("data").path("checklistId").asLong();
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

    private void assertContinuousOrders(final JsonNode items) {
        for (int index = 0; index < items.size(); index++) {
            assertThat(items.get(index).path("order").asInt()).isEqualTo(index + 1);
        }
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
