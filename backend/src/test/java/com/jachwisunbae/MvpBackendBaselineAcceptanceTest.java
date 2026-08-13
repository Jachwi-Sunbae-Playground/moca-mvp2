package com.jachwisunbae;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.AcceptanceTest;
import com.jachwisunbae.common.FakePhotoStorage;
import com.jachwisunbae.common.TestImages;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ExtendWith(OutputCaptureExtension.class)
class MvpBackendBaselineAcceptanceTest extends AcceptanceTest {

    private static final String PROPERTIES_URL = "/api/properties";
    private static final String CHECKLISTS_URL = "/api/checklists";
    private static final Pattern PATH_VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)}");
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
    private static final List<ApiContract> API_CONTRACTS = List.of(
            api("API-001", "post", "/api/auth/google", "200", false),
            api("API-002", "get", "/api/members/me", "200", true),
            api("API-101", "get", "/api/properties", "200", true),
            api("API-102", "post", "/api/properties", "201", true),
            api("API-103", "get", "/api/properties/{propertyId}", "200", true),
            api("API-104", "patch", "/api/properties/{propertyId}", "200", true),
            api("API-105", "delete", "/api/properties/{propertyId}", "204", true),
            api("API-106", "put", "/api/properties/{propertyId}/memo", "200", true),
            api("API-201", "get", "/api/properties/{propertyId}/photos", "200", true),
            api("API-202", "post", "/api/properties/{propertyId}/photos", "201", true),
            api("API-203", "get", "/api/properties/{propertyId}/photos/{photoId}/content", "200", true),
            api("API-204", "delete", "/api/properties/{propertyId}/photos/{photoId}", "204", true),
            api("API-301", "get", "/api/check-items", "200", true),
            api("API-302", "get", "/api/checklist-presets", "200", true),
            api("API-303", "get", "/api/checklists", "200", true),
            api("API-304", "post", "/api/checklists", "201", true),
            api("API-305", "get", "/api/checklists/{checklistId}", "200", true),
            api("API-306", "put", "/api/checklists/{checklistId}", "200", true),
            api("API-307", "delete", "/api/checklists/{checklistId}", "204", true),
            api("API-401", "put", "/api/properties/{propertyId}/active-checklists/{stage}", "200", true),
            api("API-402", "delete", "/api/properties/{propertyId}/active-checklists/{stage}", "204", true),
            api("API-501", "get", "/api/properties/{propertyId}/visits", "200", true),
            api("API-502", "post", "/api/properties/{propertyId}/visits", "201", true),
            api("API-503", "get", "/api/visits/{visitId}", "200", true),
            api("API-504", "patch", "/api/visits/{visitId}/items/{visitItemId}", "200", true),
            api("API-505", "patch", "/api/visits/{visitId}", "200", true),
            api("API-506", "patch", "/api/visits/{visitId}/items/{visitItemId}/memo", "200", true)
    );

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakePhotoStorage photoStorage;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
        photoStorage.reset();
    }

    @DisplayName("Flyway 초기화 뒤 애플리케이션과 health endpoint가 정상 기동한다")
    @Test
    void startApplicationAndExposeHealth() {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/actuator/health", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().path("status").asText()).isEqualTo("UP");
    }

    @DisplayName("v1.1 전체 흐름은 구조화 메모·CUSTOM 스냅샷·독립 CAS·완료 뒤 편집·삭제 경계를 보존한다")
    @Test
    void connectCoreDomainsAndProtectOwnership() {
        final String ownerToken = login("baseline-owner");
        final String otherToken = login("baseline-other");
        final long firstPropertyId = createProperty(
                ownerToken,
                "첫 원룸",
                "https://example.com/rooms/first"
        );
        final long secondPropertyId = createProperty(ownerToken, "둘째 원룸", "학교 게시판에서 발견");

        final ResponseEntity<JsonNode> updatedProperty = exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId,
                HttpMethod.PATCH,
                ownerToken,
                Map.of("name", "첫 원룸 재방문")
        );
        assertThat(updatedProperty.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode firstProperty = exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data");
        final JsonNode secondProperty = exchangeJson(
                PROPERTIES_URL + "/" + secondPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data");
        assertThat(firstProperty.path("discoverySource").path("type").asText()).isEqualTo("URL");
        assertThat(secondProperty.path("discoverySource").path("type").asText()).isEqualTo("TEXT");

        final ResponseEntity<JsonNode> savedStructuredMemo = exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId + "/memo",
                HttpMethod.PUT,
                ownerToken,
                structuredMemo("8월 20일 오후 2시", "채광과 소음 재확인")
        );
        assertThat(savedStructuredMemo.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode structuredMemo = savedStructuredMemo.getBody().path("data");
        assertThat(structuredMemo.path("viewingSchedule").asText()).isEqualTo("8월 20일 오후 2시");
        assertThat(structuredMemo.path("governmentSupport").asText()).isEqualTo("정부 지원 확인");
        assertThat(structuredMemo.path("additionalMemo").asText()).isEqualTo("채광과 소음 재확인");
        assertThat(structuredMemo.path("content").asText()).isEqualTo("채광과 소음 재확인");
        assertThat(structuredMemo.path("savedAt").isTextual()).isTrue();

        final JsonNode legacyMemo = exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId + "/memo",
                HttpMethod.PUT,
                ownerToken,
                Map.of("content", "legacy 추가 메모 수정")
        ).getBody().path("data");
        assertThat(legacyMemo.path("viewingSchedule").asText()).isEqualTo("8월 20일 오후 2시");
        assertThat(legacyMemo.path("moveInAvailability").asText()).isEqualTo("9월 1일부터 가능");
        assertThat(legacyMemo.path("governmentSupport").asText()).isEqualTo("정부 지원 확인");
        assertThat(legacyMemo.path("additionalMemo").asText()).isEqualTo("legacy 추가 메모 수정");
        assertThat(legacyMemo.path("content").asText()).isEqualTo("legacy 추가 메모 수정");
        assertThat(exchangeJson(
                PROPERTIES_URL + "?query=재방문",
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data").path("content")).hasSize(1);

        final long photoId = uploadPhoto(ownerToken, firstPropertyId);
        assertThat(photoStorage.size()).isOne();
        final ResponseEntity<byte[]> photoContent = restTemplate.exchange(
                PROPERTIES_URL + "/%d/photos/%d/content".formatted(firstPropertyId, photoId),
                HttpMethod.GET,
                new HttpEntity<>(headers(ownerToken, null)),
                byte[].class
        );
        assertThat(photoContent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(photoContent.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(photoContent.getHeaders().getCacheControl()).isEqualTo("private, no-store");
        assertThat(photoContent.getBody()).isEqualTo(TestImages.png());
        assertThat(exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data").path("photoPreview").path("photos").get(0).path("photoId").asLong())
                .isEqualTo(photoId);

        final JsonNode createdChecklist = createV11Checklist(ownerToken, "첫 현장 점검", "창틀 곰팡이는 괜찮은가?");
        final long checklistId = createdChecklist.path("checklistId").asLong();
        final long customChecklistItemId = findItem(createdChecklist.path("items"), "CUSTOM")
                .path("checklistItemId").asLong();
        assign(ownerToken, firstPropertyId, checklistId);
        assign(ownerToken, firstPropertyId, checklistId);
        assign(ownerToken, secondPropertyId, checklistId);
        assertThat(getChecklist(ownerToken, checklistId).path("assignedPropertyCount").asInt()).isEqualTo(2);

        final JsonNode firstVisit = startVisit(ownerToken, firstPropertyId);
        final long firstVisitId = firstVisit.path("visitId").asLong();
        final JsonNode firstCustomItem = findItem(firstVisit.path("stages").get(0).path("items"), "CUSTOM");
        final long firstCustomVisitItemId = firstCustomItem.path("visitItemId").asLong();
        assertThat(firstVisit.path("stages").get(0).path("items").findValuesAsText("origin"))
                .containsExactly("PROVIDED", "CUSTOM");
        assertThat(firstCustomItem.path("sourceChecklistItemId").asLong()).isEqualTo(customChecklistItemId);
        assertThat(firstCustomItem.path("sourceCheckItemId").isNull()).isTrue();
        assertThat(firstCustomItem.path("question").asText()).isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(firstCustomItem.path("statusVersion").asLong()).isZero();
        assertThat(firstCustomItem.path("memoVersion").asLong()).isZero();

        final ResponseEntity<JsonNode> firstStatusSave = saveStatus(
                ownerToken,
                firstVisitId,
                firstCustomVisitItemId,
                "CAUTION",
                0
        );
        final ResponseEntity<JsonNode> firstMemoSave = saveInlineMemo(
                ownerToken,
                firstVisitId,
                firstCustomVisitItemId,
                "창틀 실리콘을 다시 확인",
                0
        );
        assertThat(firstStatusSave.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstStatusSave.getBody().path("data").path("item").path("statusVersion").asLong()).isOne();
        assertThat(firstMemoSave.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstMemoSave.getBody().path("data").path("memoVersion").asLong()).isOne();
        assertConflict(saveStatus(ownerToken, firstVisitId, firstCustomVisitItemId, "GOOD", 0),
                "VISIT_ITEM_STATUS_VERSION_CONFLICT");
        assertConflict(saveInlineMemo(ownerToken, firstVisitId, firstCustomVisitItemId, "유실되면 안 되는 입력", 0),
                "VISIT_ITEM_MEMO_VERSION_CONFLICT");
        final JsonNode independentlySavedItem = findItem(
                getVisit(ownerToken, firstVisitId).path("stages").get(0).path("items"),
                "CUSTOM"
        );
        assertThat(independentlySavedItem.path("status").asText()).isEqualTo("CAUTION");
        assertThat(independentlySavedItem.path("statusVersion").asLong()).isOne();
        assertThat(independentlySavedItem.path("statusSavedAt").isTextual()).isTrue();
        assertThat(independentlySavedItem.path("inlineMemo").asText()).isEqualTo("창틀 실리콘을 다시 확인");
        assertThat(independentlySavedItem.path("memoVersion").asLong()).isOne();
        assertThat(independentlySavedItem.path("memoSavedAt").isTextual()).isTrue();

        final JsonNode firstCompleted = completeVisit(ownerToken, firstVisitId);
        final String firstCompletedAt = firstCompleted.path("completedAt").asText();
        assertThat(firstCompletedAt).isNotBlank();
        assertThat(saveStatus(ownerToken, firstVisitId, firstCustomVisitItemId, "GOOD", 1).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(saveInlineMemo(
                ownerToken,
                firstVisitId,
                firstCustomVisitItemId,
                "완료 뒤 확정한 창틀 메모",
                1
        ).getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode firstAfterCompletion = getVisit(ownerToken, firstVisitId);
        assertThat(firstAfterCompletion.path("completedAt").asText()).isEqualTo(firstCompletedAt);
        final JsonNode editedAfterCompletion = findItem(
                firstAfterCompletion.path("stages").get(0).path("items"),
                "CUSTOM"
        );
        assertThat(editedAfterCompletion.path("status").asText()).isEqualTo("GOOD");
        assertThat(editedAfterCompletion.path("statusVersion").asLong()).isEqualTo(2);
        assertThat(editedAfterCompletion.path("inlineMemo").asText()).isEqualTo("완료 뒤 확정한 창틀 메모");
        assertThat(editedAfterCompletion.path("memoVersion").asLong()).isEqualTo(2);

        final ResponseEntity<JsonNode> replaced = exchangeJson(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.PUT,
                ownerToken,
                Map.of(
                        "name", "바뀐 현장 점검",
                        "items", List.of(
                                Map.of(
                                        "origin", "CUSTOM",
                                        "checklistItemId", customChecklistItemId,
                                        "question", "수정한 창틀 결로는 괜찮은가?"
                                ),
                                Map.of("origin", "PROVIDED", "sourceCheckItemId", 102L)
                        )
                )
        );
        assertThat(replaced.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(replaced.getBody().path("data").path("items").get(0).path("checklistItemId").asLong())
                .isEqualTo(customChecklistItemId);
        final JsonNode secondPropertyAfterReplacement = exchangeJson(
                PROPERTIES_URL + "/" + secondPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data");
        assertThat(secondPropertyAfterReplacement.path("activeChecklists").get(0).path("name").asText())
                .isEqualTo("바뀐 현장 점검");
        assertThat(findItem(
                getVisit(ownerToken, firstVisitId).path("stages").get(0).path("items"),
                "CUSTOM"
        ).path("question").asText()).isEqualTo("창틀 곰팡이는 괜찮은가?");

        final JsonNode secondVisit = startVisit(ownerToken, secondPropertyId);
        final long secondVisitId = secondVisit.path("visitId").asLong();
        final JsonNode secondCustomItem = findItem(secondVisit.path("stages").get(0).path("items"), "CUSTOM");
        final long secondCustomVisitItemId = secondCustomItem.path("visitItemId").asLong();
        assertThat(secondCustomItem.path("question").asText()).isEqualTo("수정한 창틀 결로는 괜찮은가?");
        assertThat(secondCustomItem.path("status").asText()).isEqualTo("UNCONFIRMED");
        assertThat(secondCustomItem.path("statusVersion").asLong()).isZero();
        assertThat(secondCustomItem.path("inlineMemo").asText()).isEmpty();
        assertThat(secondCustomItem.path("memoVersion").asLong()).isZero();
        final String secondCompletedAt = completeVisit(ownerToken, secondVisitId).path("completedAt").asText();
        assertThat(completeVisit(ownerToken, secondVisitId).path("completedAt").asText())
                .isEqualTo(secondCompletedAt);

        final ResponseEntity<JsonNode> removedCustom = exchangeJson(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.PUT,
                ownerToken,
                Map.of(
                        "name", "PROVIDED 전용 점검",
                        "items", List.of(Map.of("origin", "PROVIDED", "sourceCheckItemId", 103L))
                )
        );
        assertThat(removedCustom.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode preservedFirstCustom = findItem(
                getVisit(ownerToken, firstVisitId).path("stages").get(0).path("items"),
                "CUSTOM"
        );
        final JsonNode preservedSecondCustom = findItem(
                getVisit(ownerToken, secondVisitId).path("stages").get(0).path("items"),
                "CUSTOM"
        );
        assertThat(preservedFirstCustom.path("sourceChecklistItemId").isNull()).isTrue();
        assertThat(preservedFirstCustom.path("question").asText()).isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(preservedSecondCustom.path("sourceChecklistItemId").isNull()).isTrue();
        assertThat(preservedSecondCustom.path("question").asText()).isEqualTo("수정한 창틀 결로는 괜찮은가?");

        assertHidden(exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId,
                HttpMethod.GET,
                otherToken,
                null
        ), "PROPERTY_NOT_FOUND");
        assertHidden(exchangeJson(
                PROPERTIES_URL + "/%d/photos/%d/content".formatted(firstPropertyId, photoId),
                HttpMethod.GET,
                otherToken,
                null
        ), "PROPERTY_NOT_FOUND");
        assertHidden(exchangeJson(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.GET,
                otherToken,
                null
        ), "CHECKLIST_NOT_FOUND");
        assertHidden(exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId + "/active-checklists/ON_SITE",
                HttpMethod.PUT,
                otherToken,
                Map.of("checklistId", checklistId)
        ), "PROPERTY_NOT_FOUND");
        assertHidden(exchangeJson(
                "/api/visits/" + secondVisitId,
                HttpMethod.GET,
                otherToken,
                null
        ), "VISIT_NOT_FOUND");
        assertHidden(exchangeJson(
                "/api/visits/%d/items/%d".formatted(secondVisitId, secondCustomVisitItemId),
                HttpMethod.PATCH,
                otherToken,
                Map.of("status", "GOOD", "expectedStatusVersion", 0)
        ), "VISIT_NOT_FOUND");
        assertHidden(exchangeJson(
                "/api/visits/%d/items/%d/memo".formatted(secondVisitId, secondCustomVisitItemId),
                HttpMethod.PATCH,
                otherToken,
                Map.of("memo", "타인 메모", "expectedMemoVersion", 0)
        ), "VISIT_NOT_FOUND");

        assertThat(exchangeJson(
                PROPERTIES_URL + "/" + firstPropertyId,
                HttpMethod.DELETE,
                ownerToken,
                null
        ).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(photoStorage.size()).isZero();
        assertHidden(exchangeJson(
                "/api/visits/" + firstVisitId,
                HttpMethod.GET,
                ownerToken,
                null
        ), "VISIT_NOT_FOUND");
        assertThat(getChecklist(ownerToken, checklistId).path("assignedPropertyCount").asInt()).isOne();
        assertThat(exchangeJson(
                PROPERTIES_URL + "/" + secondPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data").path("activeChecklists")).hasSize(1);
        final JsonNode unaffectedSecondVisit = getVisit(ownerToken, secondVisitId);
        assertThat(unaffectedSecondVisit.path("status").asText()).isEqualTo("COMPLETED");
        assertThat(unaffectedSecondVisit.path("completedAt").asText()).isEqualTo(secondCompletedAt);

        assertThat(exchangeJson(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.DELETE,
                ownerToken,
                null
        ).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        final JsonNode preservedAfterChecklistDeletion = getVisit(ownerToken, secondVisitId);
        assertThat(preservedAfterChecklistDeletion.path("stages").get(0).path("sourceChecklistId").isNull()).isTrue();
        assertThat(findItem(
                preservedAfterChecklistDeletion.path("stages").get(0).path("items"),
                "CUSTOM"
        ).path("question").asText()).isEqualTo("수정한 창틀 결로는 괜찮은가?");
        assertThat(exchangeJson(
                PROPERTIES_URL + "/" + secondPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getBody().path("data").path("activeChecklists")).isEmpty();
        assertThat(exchangeJson(
                PROPERTIES_URL + "/" + secondPropertyId,
                HttpMethod.GET,
                ownerToken,
                null
        ).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getVisit(ownerToken, secondVisitId).path("completedAt").asText()).isEqualTo(secondCompletedAt);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM properties", Long.class)).isOne();
    }

    @DisplayName("실제 OpenAPI는 정확히 27개 MVP API와 26개 보호 API의 Bearer·오류 계약을 공개한다")
    @Test
    void exposeExactOpenApiBaseline(final CapturedOutput output) {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);
        final ResponseEntity<JsonNode> repeatedResponse = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(repeatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        final JsonNode document = response.getBody();
        final JsonNode paths = document.path("paths");
        assertThat(document.path("openapi").asText()).isEqualTo("3.1.0");
        assertThat(document.path("info").path("version").asText()).isEqualTo("1차 MVP v1.1");
        assertThat(actualOperations(paths))
                .containsExactlyInAnyOrderElementsOf(API_CONTRACTS.stream().map(ApiContract::key).toList());
        assertThat(API_CONTRACTS).filteredOn(ApiContract::protectedApi).hasSize(26);
        assertThat(API_CONTRACTS).filteredOn(contract -> !contract.protectedApi()).hasSize(1);

        for (final ApiContract contract : API_CONTRACTS) {
            final JsonNode operation = paths.path(contract.path()).path(contract.method());
            assertThat(operation.isMissingNode()).as(contract.id()).isFalse();
            assertThat(parameterNames(operation, null)).as(contract.id()).doesNotContain("memberId");
            assertThat(parameterNames(operation, "path"))
                    .as(contract.id() + " path parameters")
                    .containsExactlyInAnyOrderElementsOf(pathVariableNames(contract.path()));
            assertThat(operation.path("responses").has(contract.successStatus())).as(contract.id()).isTrue();
            if (!"204".equals(contract.successStatus())) {
                assertThat(operation.path("responses").path(contract.successStatus()).path("content").isEmpty())
                        .as(contract.id() + " success response schema")
                        .isFalse();
            }
            if (contract.protectedApi()) {
                assertThat(operation.path("security").get(0).has("bearerAuth")).as(contract.id()).isTrue();
                final JsonNode errorContent = operation.path("responses").path("401").path("content");
                assertThat(errorContent.isEmpty()).as(contract.id() + " 401 error content").isFalse();
                assertThat(errorContent.elements().next().path("schema").isMissingNode())
                        .as(contract.id() + " 401 error schema").isFalse();
            } else {
                assertThat(operation.path("security").isMissingNode() || operation.path("security").isEmpty())
                        .as(contract.id())
                        .isTrue();
            }
        }

        assertThat(parameterNames(paths.path("/api/properties").path("get"), "query"))
                .as("API-101 query parameters")
                .containsExactlyInAnyOrder("query", "page", "size");
        assertThat(parameterNames(paths.path("/api/check-items").path("get"), "query"))
                .as("API-301 query parameters")
                .containsExactlyInAnyOrder("stage", "query", "page", "size");
        assertThat(parameterNames(paths.path("/api/checklist-presets").path("get"), "query"))
                .as("API-302 query parameters")
                .containsExactlyInAnyOrder("presetType", "stage");
        assertThat(parameterNames(paths.path("/api/checklists").path("get"), "query"))
                .as("API-303 query parameters")
                .containsExactlyInAnyOrder("stage", "page", "size");
        assertThat(parameterNames(paths.path("/api/properties/{propertyId}/visits").path("get"), "query"))
                .as("API-501 query parameters")
                .containsExactlyInAnyOrder("page", "size");
        assertThat(paths.path("/api/visits/{visitId}").path("patch").isMissingNode()).as("API-505").isFalse();
        assertThat(paths.path("/api/visits/{visitId}/items/{visitItemId}/memo")
                .path("patch").isMissingNode()).as("API-506").isFalse();

        final JsonNode bearer = document.path("components").path("securitySchemes").path("bearerAuth");
        assertThat(bearer.path("type").asText()).isEqualTo("http");
        assertThat(bearer.path("scheme").asText()).isEqualTo("bearer");
        assertThat(bearer.path("bearerFormat").asText()).isEqualTo("JWT");
        final JsonNode schemas = document.path("components").path("schemas");

        final JsonNode photoUpload = paths.path("/api/properties/{propertyId}/photos").path("post")
                .path("requestBody").path("content").path(MediaType.MULTIPART_FORM_DATA_VALUE);
        assertThat(photoUpload.isMissingNode()).isFalse();
        final JsonNode uploadPhotoRequest = schemas.path("UploadPhotoRequest");
        assertThat(textValues(uploadPhotoRequest.path("required"))).contains("file");
        assertThat(uploadPhotoRequest.path("properties").path("file").path("type").asText()).isEqualTo("string");
        assertThat(uploadPhotoRequest.path("properties").path("file").path("format").asText()).isEqualTo("binary");
        final JsonNode photoStreamSchema = paths
                .path("/api/properties/{propertyId}/photos/{photoId}/content")
                .path("get").path("responses").path("200").path("content").path("image/*").path("schema");
        assertThat(photoStreamSchema.path("type").asText()).isEqualTo("string");
        assertThat(photoStreamSchema.path("format").asText()).isEqualTo("binary");

        final JsonNode memoRequest = schemas.path("SavePropertyMemoRequest").path("properties");
        assertThat(memoRequest.fieldNames()).toIterable().contains(
                "viewingSchedule",
                "moveInAvailability",
                "provisionalDeposit",
                "roomOptions",
                "maintenanceAndUtilities",
                "commuteTime",
                "governmentSupport",
                "additionalMemo",
                "content"
        );
        assertThat(memoRequest.path("content").path("deprecated").asBoolean()).isTrue();
        assertThat(memoRequest.has("expectedVersion")).isFalse();
        final JsonNode memoResponse = schemas.path("PropertyMemoResponse").path("properties");
        assertThat(memoResponse.path("content").path("deprecated").asBoolean()).isTrue();
        assertThat(memoResponse.has("version")).isFalse();
        assertNullable(memoResponse.path("savedAt"));

        final JsonNode checklistItemRequest = schemas.path("ChecklistItemRequest").path("properties");
        assertThat(checklistItemRequest.fieldNames()).toIterable().containsExactlyInAnyOrder(
                "origin",
                "sourceCheckItemId",
                "checklistItemId",
                "question"
        );
        assertThat(textValues(checklistItemRequest.path("origin").path("enum")))
                .containsExactly("PROVIDED", "CUSTOM");
        assertNullable(checklistItemRequest.path("sourceCheckItemId"));
        assertNullable(checklistItemRequest.path("checklistItemId"));
        assertNullable(checklistItemRequest.path("question"));
        assertThat(schemas.path("CreateChecklistRequest").path("properties")
                .path("checkItemIds").path("deprecated").asBoolean()).isTrue();
        assertThat(schemas.path("ReplaceChecklistRequest").path("properties")
                .path("checkItemIds").path("deprecated").asBoolean()).isTrue();
        final JsonNode checklistItemResponse = schemas.path("ChecklistItemResponse").path("properties");
        assertThat(checklistItemResponse.path("checkItemId").path("deprecated").asBoolean()).isTrue();
        assertNullable(checklistItemResponse.path("sourceCheckItemId"));
        assertNullable(checklistItemResponse.path("checkItemId"));
        assertNullable(checklistItemResponse.path("guide"));
        assertThat(paths.path("/api/checklists/{checklistId}").path("put").path("responses").has("409"))
                .isTrue();

        final JsonNode visitItemRequest = schemas.path("UpdateVisitItemRequest").path("properties");
        assertThat(visitItemRequest.path("expectedStatusVersion").path("minimum").asLong()).isZero();
        assertThat(visitItemRequest.path("expectedVersion").path("minimum").asLong()).isZero();
        assertThat(visitItemRequest.path("expectedVersion").path("deprecated").asBoolean()).isTrue();
        final JsonNode visitMemoRequest = schemas.path("UpdateVisitItemMemoRequest").path("properties");
        assertThat(visitMemoRequest.path("memo").path("maxLength").asInt()).isEqualTo(200);
        assertThat(visitMemoRequest.path("expectedMemoVersion").path("minimum").asLong()).isZero();
        final JsonNode visitDetailItem = schemas.path("VisitDetailItemResponse").path("properties");
        assertThat(visitDetailItem.fieldNames()).toIterable().contains(
                "status",
                "statusVersion",
                "statusSavedAt",
                "inlineMemo",
                "memoVersion",
                "memoSavedAt",
                "version",
                "savedAt"
        );
        assertNullable(visitDetailItem.path("sourceChecklistItemId"));
        assertNullable(visitDetailItem.path("sourceCheckItemId"));
        assertNullable(visitDetailItem.path("memoSavedAt"));
        assertThat(visitDetailItem.path("version").path("deprecated").asBoolean()).isTrue();
        assertThat(visitDetailItem.path("savedAt").path("deprecated").asBoolean()).isTrue();
        final JsonNode statusItem = schemas.path("VisitItemStatusItemResponse").path("properties");
        assertThat(statusItem.has("inlineMemo")).isFalse();
        assertThat(statusItem.path("version").path("deprecated").asBoolean()).isTrue();
        assertThat(statusItem.path("savedAt").path("deprecated").asBoolean()).isTrue();
        assertThat(schemas.path("VisitItemMemoResponse").path("properties").fieldNames()).toIterable()
                .containsExactlyInAnyOrder("visitItemId", "memo", "memoVersion", "memoSavedAt");
        assertThat(paths.path("/api/visits/{visitId}/items/{visitItemId}").path("patch")
                .path("responses").has("409")).isTrue();
        assertThat(paths.path("/api/visits/{visitId}/items/{visitItemId}/memo").path("patch")
                .path("responses").has("409")).isTrue();

        assertThat(actualOperations(paths)).noneMatch(operation ->
                operation.contains("refresh")
                        || operation.contains("session")
                        || operation.contains("logout")
                        || operation.contains("compare")
                        || operation.contains("comparison")
                        || operation.contains("export")
                        || operation.contains("/map")
                        || operation.contains("moving")
                        || operation.contains("tips")
                        || operation.contains("help")
        );
        assertThat(output.getAll()).doesNotContain("Json Processing Exception occurred");
    }

    private String login(final String subject) {
        return exchangeJson(
                "/api/auth/google",
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
        return createProperty(token, name, "직접 발견");
    }

    private long createProperty(final String token, final String name, final String discoverySource) {
        return exchangeJson(
                PROPERTIES_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", name,
                        "depositAmount", 10_000_000,
                        "monthlyRentAmount", 500_000,
                        "discoverySource", discoverySource
                )
        ).getBody().path("data").path("propertyId").asLong();
    }

    private Map<String, String> structuredMemo(final String viewingSchedule, final String additionalMemo) {
        final Map<String, String> memo = new LinkedHashMap<>();
        memo.put("viewingSchedule", viewingSchedule);
        memo.put("moveInAvailability", "9월 1일부터 가능");
        memo.put("provisionalDeposit", "가계약금 30만 원");
        memo.put("roomOptions", "냉장고와 세탁기 포함");
        memo.put("maintenanceAndUtilities", "관리비와 전기·가스 별도");
        memo.put("commuteTime", "학교까지 버스로 20분");
        memo.put("governmentSupport", "정부 지원 확인");
        memo.put("additionalMemo", additionalMemo);
        return memo;
    }

    private JsonNode createV11Checklist(final String token, final String name, final String customQuestion) {
        final ResponseEntity<JsonNode> response = exchangeJson(
                CHECKLISTS_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", name,
                        "stage", "ON_SITE",
                        "items", List.of(
                                Map.of("origin", "PROVIDED", "sourceCheckItemId", 101L),
                                Map.of("origin", "CUSTOM", "question", customQuestion)
                        )
                )
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().path("data");
    }

    private JsonNode findItem(final JsonNode items, final String origin) {
        for (final JsonNode item : items) {
            if (origin.equals(item.path("origin").asText())) {
                return item;
            }
        }
        throw new IllegalStateException("방문 기준선에서 origin 항목을 찾지 못했다: " + origin);
    }

    private ResponseEntity<JsonNode> saveStatus(
            final String token,
            final long visitId,
            final long visitItemId,
            final String status,
            final long expectedStatusVersion
    ) {
        return exchangeJson(
                "/api/visits/%d/items/%d".formatted(visitId, visitItemId),
                HttpMethod.PATCH,
                token,
                Map.of("status", status, "expectedStatusVersion", expectedStatusVersion)
        );
    }

    private ResponseEntity<JsonNode> saveInlineMemo(
            final String token,
            final long visitId,
            final long visitItemId,
            final String memo,
            final long expectedMemoVersion
    ) {
        return exchangeJson(
                "/api/visits/%d/items/%d/memo".formatted(visitId, visitItemId),
                HttpMethod.PATCH,
                token,
                Map.of("memo", memo, "expectedMemoVersion", expectedMemoVersion)
        );
    }

    private void assign(final String token, final long propertyId, final long checklistId) {
        final ResponseEntity<JsonNode> response = exchangeJson(
                PROPERTIES_URL + "/" + propertyId + "/active-checklists/ON_SITE",
                HttpMethod.PUT,
                token,
                Map.of("checklistId", checklistId)
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private JsonNode getChecklist(final String token, final long checklistId) {
        return exchangeJson(
                CHECKLISTS_URL + "/" + checklistId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
    }

    private JsonNode startVisit(final String token, final long propertyId) {
        return exchangeJson(
                PROPERTIES_URL + "/" + propertyId + "/visits",
                HttpMethod.POST,
                token,
                null
        ).getBody().path("data");
    }

    private JsonNode getVisit(final String token, final long visitId) {
        return exchangeJson(
                "/api/visits/" + visitId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
    }

    private JsonNode completeVisit(final String token, final long visitId) {
        return exchangeJson(
                "/api/visits/" + visitId,
                HttpMethod.PATCH,
                token,
                Map.of("status", "COMPLETED")
        ).getBody().path("data");
    }

    private long uploadPhoto(final String token, final long propertyId) {
        final HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_PNG);
        final ByteArrayResource resource = new ByteArrayResource(TestImages.png()) {
            @Override
            public String getFilename() {
                return "client-name.png";
            }
        };
        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, partHeaders));
        final ResponseEntity<JsonNode> response = restTemplate.exchange(
                PROPERTIES_URL + "/" + propertyId + "/photos",
                HttpMethod.POST,
                new HttpEntity<>(body, headers(token, MediaType.MULTIPART_FORM_DATA)),
                JsonNode.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().toString()).doesNotContain("client-name.png", "storageKey");
        return response.getBody().path("data").path("photoId").asLong();
    }

    private ResponseEntity<JsonNode> exchangeJson(
            final String url,
            final HttpMethod method,
            final String token,
            final Object body
    ) {
        return restTemplate.exchange(
                url,
                method,
                new HttpEntity<>(body, headers(token, MediaType.APPLICATION_JSON)),
                JsonNode.class
        );
    }

    private HttpHeaders headers(final String token, final MediaType contentType) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(contentType);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private void assertConflict(final ResponseEntity<JsonNode> response, final String code) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
    }

    private void assertHidden(final ResponseEntity<JsonNode> response, final String code) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().path("code").asText()).isEqualTo(code);
    }

    private List<String> actualOperations(final JsonNode paths) {
        final List<String> operations = new ArrayList<>();
        paths.properties().forEach(path -> path.getValue().properties().forEach(method ->
                operations.add(method.getKey() + " " + path.getKey())
        ));
        return operations;
    }

    private List<String> textValues(final JsonNode array) {
        final List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.asText()));
        return values;
    }

    private List<String> parameterNames(final JsonNode operation, final String location) {
        final List<String> names = new ArrayList<>();
        operation.path("parameters").forEach(parameter -> {
            if (location == null || location.equals(parameter.path("in").asText())) {
                names.add(parameter.path("name").asText());
            }
        });
        return names;
    }

    private List<String> pathVariableNames(final String path) {
        final List<String> names = new ArrayList<>();
        final Matcher matcher = PATH_VARIABLE_PATTERN.matcher(path);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private void assertNullable(final JsonNode schema) {
        assertThat(schema.path("nullable").asBoolean()
                || schema.path("type").toString().contains("null")
                || schema.path("oneOf").toString().contains("\"null\"")
                || schema.path("anyOf").toString().contains("\"null\""))
                .as("nullable schema: " + schema)
                .isTrue();
    }

    private static ApiContract api(
            final String id,
            final String method,
            final String path,
            final String successStatus,
            final boolean protectedApi
    ) {
        return new ApiContract(id, method, path, successStatus, protectedApi);
    }

    private record ApiContract(
            String id,
            String method,
            String path,
            String successStatus,
            boolean protectedApi
    ) {

        String key() {
            return method + " " + path;
        }
    }
}
