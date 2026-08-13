package com.jachwisunbae.property.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.jachwisunbae.common.AcceptanceTest;
import com.jachwisunbae.common.FakePhotoStorage;
import com.jachwisunbae.common.TestImages;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

class PropertyPhotoAcceptanceTest extends AcceptanceTest {

    private static final String PROPERTIES_URL = "/api/properties";
    private static final String LOGIN_URL = "/api/auth/google";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/google/callback";
    private static final String CODE_VERIFIER = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakePhotoStorage photoStorage;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        photoStorage.reset();
    }

    @DisplayName("회원은 단건 multipart로 사진을 등록하고 목록·인증 본문·매물 요약에서 확인한 뒤 삭제한다")
    @Test
    void managePhotosThroughApi() {
        final String token = login("photo-api-owner");
        final long propertyId = createProperty(token, "사진 매물");
        final byte[] png = TestImages.png();

        final ResponseEntity<JsonNode> firstUpload = upload(
                token,
                propertyId,
                "../../같은이름.png",
                MediaType.IMAGE_PNG,
                png
        );
        final ResponseEntity<JsonNode> secondUpload = upload(
                token,
                propertyId,
                "../../같은이름.png",
                MediaType.IMAGE_PNG,
                png
        );

        assertThat(firstUpload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondUpload.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        final long firstPhotoId = firstUpload.getBody().path("data").path("photoId").asLong();
        final long secondPhotoId = secondUpload.getBody().path("data").path("photoId").asLong();
        final String firstContentUrl = "/api/properties/%d/photos/%d/content".formatted(propertyId, firstPhotoId);
        assertThat(firstUpload.getHeaders().getLocation()).hasPath(firstContentUrl);
        assertThat(firstUpload.getBody().toString()).doesNotContain("storageKey", "같은이름.png");
        assertThat(photoStorage.storageKeys()).hasSize(2);

        final ResponseEntity<JsonNode> list = exchangeJson(
                "/api/properties/%d/photos".formatted(propertyId),
                HttpMethod.GET,
                token,
                null
        );
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody().path("data").path("totalCount").asInt()).isEqualTo(2);
        assertThat(list.getBody().path("data").path("photos").get(0).path("photoId").asLong())
                .isEqualTo(firstPhotoId);
        assertThat(list.getBody().path("data").path("photos").get(1).path("photoId").asLong())
                .isEqualTo(secondPhotoId);

        final ResponseEntity<byte[]> content = restTemplate.exchange(
                firstContentUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers(token, null)),
                byte[].class
        );
        assertThat(content.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(content.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(content.getHeaders().getContentLength()).isEqualTo(png.length);
        assertThat(content.getHeaders().getCacheControl()).isEqualTo("private, no-store");
        assertThat(content.getBody()).isEqualTo(png);

        final JsonNode summary = exchangeJson(PROPERTIES_URL, HttpMethod.GET, token, null)
                .getBody().path("data").path("content").get(0);
        final JsonNode detail = exchangeJson(
                PROPERTIES_URL + "/" + propertyId,
                HttpMethod.GET,
                token,
                null
        ).getBody().path("data");
        assertThat(summary.path("photoCount").asInt()).isEqualTo(2);
        assertThat(detail.path("photoPreview").path("totalCount").asInt()).isEqualTo(2);
        assertThat(detail.path("photoPreview").path("photos")).hasSize(1);
        assertThat(detail.path("photoPreview").path("photos").get(0).path("photoId").asLong())
                .isEqualTo(firstPhotoId);
        assertThat(detail.path("deletionImpact").path("photoCount").asInt()).isEqualTo(2);

        final ResponseEntity<JsonNode> deleted = exchangeJson(
                "/api/properties/%d/photos/%d".formatted(propertyId, firstPhotoId),
                HttpMethod.DELETE,
                token,
                null
        );
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertError(
                exchangeJson(
                        "/api/properties/%d/photos/%d".formatted(propertyId, firstPhotoId),
                        HttpMethod.DELETE,
                        token,
                        null
                ),
                HttpStatus.NOT_FOUND,
                "PHOTO_NOT_FOUND"
        );
        assertThat(photoStorage.size()).isEqualTo(1);
    }

    @DisplayName("타인 매물과 매물·사진 ID 불일치는 같은 소유권 경계 안에서 숨긴다")
    @Test
    void protectPhotoOwnership() {
        final String ownerToken = login("photo-api-real-owner");
        final String otherToken = login("photo-api-other");
        final long propertyId = createProperty(ownerToken, "소유 매물");
        final long otherPropertyId = createProperty(ownerToken, "다른 매물");
        final long photoId = upload(
                ownerToken,
                propertyId,
                "photo.png",
                MediaType.IMAGE_PNG,
                TestImages.png()
        ).getBody().path("data").path("photoId").asLong();

        assertError(
                upload(otherToken, propertyId, "attack.png", MediaType.IMAGE_PNG, TestImages.png()),
                HttpStatus.NOT_FOUND,
                "PROPERTY_NOT_FOUND"
        );
        assertError(
                exchangeJson(
                        "/api/properties/%d/photos/%d/content".formatted(otherPropertyId, photoId),
                        HttpMethod.GET,
                        ownerToken,
                        null
                ),
                HttpStatus.NOT_FOUND,
                "PHOTO_NOT_FOUND"
        );
        assertError(
                exchangeJson(
                        "/api/properties/%d/photos/%d".formatted(propertyId, photoId),
                        HttpMethod.DELETE,
                        otherToken,
                        null
                ),
                HttpStatus.NOT_FOUND,
                "PROPERTY_NOT_FOUND"
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(1L);
    }

    @DisplayName("빈 파일·위장 MIME·손상 이미지와 저장소 실패를 계약된 오류로 반환한다")
    @Test
    void rejectInvalidAndFailedUploads() {
        final String token = login("photo-api-validation");
        final long propertyId = createProperty(token, "검증 매물");

        assertError(
                upload(token, propertyId, "empty.png", MediaType.IMAGE_PNG, new byte[0]),
                HttpStatus.BAD_REQUEST,
                "PHOTO_FORMAT_UNSUPPORTED"
        );
        assertError(
                upload(token, propertyId, "fake.jpg", MediaType.IMAGE_JPEG, TestImages.png()),
                HttpStatus.BAD_REQUEST,
                "PHOTO_FORMAT_UNSUPPORTED"
        );
        assertError(
                upload(token, propertyId, "broken.png", MediaType.IMAGE_PNG, new byte[] {
                        (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
                }),
                HttpStatus.BAD_REQUEST,
                "PHOTO_FORMAT_UNSUPPORTED"
        );
        photoStorage.failNextUpload();
        assertError(
                upload(token, propertyId, "failure.png", MediaType.IMAGE_PNG, TestImages.png()),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PHOTO_UPLOAD_FAILED"
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isZero();
        assertThat(photoStorage.size()).isZero();
    }

    @DisplayName("사진 본문·삭제 저장소 실패와 인증 실패를 계약된 오류로 반환하고 DB를 유지한다")
    @Test
    void handleStorageAndAuthenticationFailures() {
        final String token = login("photo-api-storage-failure");
        final long propertyId = createProperty(token, "저장소 실패 매물");
        final long photoId = upload(
                token,
                propertyId,
                "photo.png",
                MediaType.IMAGE_PNG,
                TestImages.png()
        ).getBody().path("data").path("photoId").asLong();
        final String contentUrl = "/api/properties/%d/photos/%d/content".formatted(propertyId, photoId);
        final String deleteUrl = "/api/properties/%d/photos/%d".formatted(propertyId, photoId);

        photoStorage.failNextOpen();
        assertError(
                exchangeJson(contentUrl, HttpMethod.GET, token, null),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PHOTO_READ_FAILED"
        );
        photoStorage.failNextDelete();
        assertError(
                exchangeJson(deleteUrl, HttpMethod.DELETE, token, null),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "PHOTO_DELETE_FAILED"
        );
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(1L);

        assertError(
                exchangeJson("/api/properties/%d/photos".formatted(propertyId), HttpMethod.GET, null, null),
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED"
        );
        assertError(
                exchangeJson("/api/properties/%d/photos".formatted(propertyId), HttpMethod.GET, "not-a-jwt", null),
                HttpStatus.UNAUTHORIZED,
                "ACCESS_TOKEN_INVALID"
        );
    }

    @DisplayName("OpenAPI에 API-201부터 API-204까지의 multipart와 바이너리 계약을 공개한다")
    @Test
    void openApiContainsPhotoContract() {
        final ResponseEntity<JsonNode> response = restTemplate.getForEntity("/v3/api-docs", JsonNode.class);
        final JsonNode paths = response.getBody().path("paths");
        final String photosPath = "/api/properties/{propertyId}/photos";
        final String contentPath = "/api/properties/{propertyId}/photos/{photoId}/content";
        final String photoPath = "/api/properties/{propertyId}/photos/{photoId}";

        assertThat(paths.path(photosPath).has("get")).isTrue();
        assertThat(paths.path(photosPath).has("post")).isTrue();
        assertThat(paths.path(contentPath).has("get")).isTrue();
        assertThat(paths.path(photoPath).has("delete")).isTrue();
        JsonNode multipartSchema = paths.path(photosPath)
                .path("post")
                .path("requestBody")
                .path("content")
                .path(MediaType.MULTIPART_FORM_DATA_VALUE)
                .path("schema");
        if (multipartSchema.has("$ref")) {
            final String schemaName = multipartSchema.path("$ref").asText()
                    .substring("#/components/schemas/".length());
            multipartSchema = response.getBody().path("components").path("schemas").path(schemaName);
        }
        final JsonNode fileSchema = multipartSchema.path("properties").path("file");
        assertThat(fileSchema.path("type").asText())
                .withFailMessage("multipart schema: %s", multipartSchema)
                .isEqualTo("string");
        assertThat(fileSchema.path("format").asText()).isEqualTo("binary");
        assertThat(multipartSchema.path("required")).anySatisfy(
                required -> assertThat(required.asText()).isEqualTo("file")
        );
        assertThat(paths.path(contentPath).path("get").path("responses").path("200")
                .path("content").has("image/*")).isTrue();
    }

    private String login(final String subject) {
        final Map<String, String> request = Map.of(
                "authorizationCode", "valid-code:" + subject,
                "codeVerifier", CODE_VERIFIER,
                "nonce", "valid-nonce",
                "redirectUri", REDIRECT_URI
        );
        return exchangeJson(LOGIN_URL, HttpMethod.POST, null, request)
                .getBody()
                .path("data")
                .path("accessToken")
                .asText();
    }

    private long createProperty(final String token, final String name) {
        return exchangeJson(
                PROPERTIES_URL,
                HttpMethod.POST,
                token,
                Map.of(
                        "name", name,
                        "depositAmount", 10_000_000,
                        "monthlyRentAmount", 500_000,
                        "discoverySource", "앱"
                )
        ).getBody().path("data").path("propertyId").asLong();
    }

    private ResponseEntity<JsonNode> upload(
            final String token,
            final long propertyId,
            final String filename,
            final MediaType contentType,
            final byte[] content
    ) {
        final HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(contentType);
        final ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        final MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, partHeaders));
        return restTemplate.exchange(
                "/api/properties/%d/photos".formatted(propertyId),
                HttpMethod.POST,
                new HttpEntity<>(body, headers(token, MediaType.MULTIPART_FORM_DATA)),
                JsonNode.class
        );
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
        if (contentType != null) {
            headers.setContentType(contentType);
        }
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
        assertThat(response.getBody().toString()).doesNotContain("storageKey", "../../");
    }
}
