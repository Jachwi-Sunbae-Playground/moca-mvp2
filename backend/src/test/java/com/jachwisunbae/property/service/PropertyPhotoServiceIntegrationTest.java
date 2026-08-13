package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.FakePhotoStorage;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.TestImages;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.property.service.dto.command.PropertySearchCondition;
import com.jachwisunbae.property.service.dto.command.UploadPhotoCommand;
import com.jachwisunbae.property.service.dto.result.PropertyDetailResult;
import com.jachwisunbae.property.service.dto.result.PropertyPhotoResult;
import com.jachwisunbae.property.service.dto.result.PropertyResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PropertyPhotoServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private PropertyQueryService propertyQueryService;

    @Autowired
    private PropertyPhotoService propertyPhotoService;

    @Autowired
    private PropertyDeletionService propertyDeletionService;

    @Autowired
    private FakePhotoStorage photoStorage;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        photoStorage.reset();
    }

    @DisplayName("사진 등록·목록·본문·삭제 결과가 DB와 객체 저장소와 매물 요약에 함께 반영된다")
    @Test
    void managePropertyPhotos() throws Exception {
        final long memberId = saveMember("photo-service-owner");
        final PropertyResult property = saveProperty(memberId, "사진 매물");
        saveProperty(memberId, "나중에 만든 매물");
        final byte[] png = TestImages.png();

        final PropertyPhotoResult first = propertyPhotoService.uploadPhoto(
                memberId,
                property.propertyId(),
                new UploadPhotoCommand("image/png", png)
        );
        final PropertyPhotoResult second = propertyPhotoService.uploadPhoto(
                memberId,
                property.propertyId(),
                new UploadPhotoCommand("image/jpeg", TestImages.jpeg())
        );

        assertThat(propertyPhotoService.getPhotos(memberId, property.propertyId()).photos())
                .extracting(PropertyPhotoResult::photoId)
                .containsExactly(first.photoId(), second.photoId());
        assertThat(propertyPhotoService.getPhotoContent(
                memberId,
                property.propertyId(),
                first.photoId()
        ).content().readAllBytes()).isEqualTo(png);
        final PropertyDetailResult detail = propertyQueryService.getProperty(memberId, property.propertyId());
        assertThat(detail.photoCount()).isEqualTo(2);
        assertThat(detail.photoPreview()).extracting(PropertyPhotoResult::photoId).containsExactly(first.photoId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(2L);
        assertThat(photoStorage.size()).isEqualTo(2);
        assertThat(propertyQueryService.getProperties(
                memberId,
                new PropertySearchCondition("", PageQuery.of(0, 20))
        ).content().getFirst().propertyId()).isEqualTo(property.propertyId());

        propertyPhotoService.deletePhoto(memberId, property.propertyId(), first.photoId());

        assertThat(propertyPhotoService.getPhotos(memberId, property.propertyId()).photos())
                .extracting(PropertyPhotoResult::photoId)
                .containsExactly(second.photoId());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(1L);
        assertThat(photoStorage.size()).isEqualTo(1);
    }

    @DisplayName("사진이 있는 매물은 모든 외부 객체를 지운 뒤 DB 메타데이터와 함께 삭제한다")
    @Test
    void deletePropertyWithPhotos() {
        final long memberId = saveMember("property-delete-owner");
        final PropertyResult property = saveProperty(memberId, "삭제 매물");
        propertyPhotoService.uploadPhoto(
                memberId,
                property.propertyId(),
                new UploadPhotoCommand("image/png", TestImages.png())
        );
        propertyPhotoService.uploadPhoto(
                memberId,
                property.propertyId(),
                new UploadPhotoCommand("image/jpeg", TestImages.jpeg())
        );

        propertyDeletionService.deleteProperty(memberId, property.propertyId());

        assertThat(photoStorage.size()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM properties", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isZero();
    }

    @DisplayName("외부 사진 삭제가 실패하면 사진과 매물의 DB 메타데이터를 유지한다")
    @Test
    void keepDatabaseWhenStorageDeleteFails() {
        final long memberId = saveMember("delete-failure-owner");
        final PropertyResult property = saveProperty(memberId, "실패 매물");
        final PropertyPhotoResult photo = propertyPhotoService.uploadPhoto(
                memberId,
                property.propertyId(),
                new UploadPhotoCommand("image/png", TestImages.png())
        );
        photoStorage.failNextDelete();

        assertError(
                () -> propertyDeletionService.deleteProperty(memberId, property.propertyId()),
                ErrorCode.PHOTO_DELETE_FAILED
        );

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM properties", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(1L);
        assertThat(propertyPhotoService.getPhotos(memberId, property.propertyId()).photos())
                .extracting(PropertyPhotoResult::photoId)
                .containsExactly(photo.photoId());
    }

    @DisplayName("다른 회원과 매물·사진 ID 불일치는 사진 존재를 숨긴다")
    @Test
    void protectPhotoOwnership() {
        final long ownerId = saveMember("photo-real-owner");
        final long otherId = saveMember("photo-other-member");
        final PropertyResult ownerProperty = saveProperty(ownerId, "소유 매물");
        final PropertyResult otherProperty = saveProperty(ownerId, "다른 매물");
        final PropertyPhotoResult photo = propertyPhotoService.uploadPhoto(
                ownerId,
                ownerProperty.propertyId(),
                new UploadPhotoCommand("image/png", TestImages.png())
        );

        assertError(
                () -> propertyPhotoService.getPhotos(otherId, ownerProperty.propertyId()),
                ErrorCode.PROPERTY_NOT_FOUND
        );
        assertError(
                () -> propertyPhotoService.getPhotoContent(
                        ownerId,
                        otherProperty.propertyId(),
                        photo.photoId()
                ),
                ErrorCode.PHOTO_NOT_FOUND
        );
        assertError(
                () -> propertyPhotoService.deletePhoto(ownerId, ownerProperty.propertyId(), photo.photoId() + 1),
                ErrorCode.PHOTO_NOT_FOUND
        );
    }

    @DisplayName("동시 등록도 매물당 사진 30장 제한을 넘지 않고 실패 업로드 객체를 보상 삭제한다")
    @Test
    void limitConcurrentPhotoUploads() throws Exception {
        final long memberId = saveMember("concurrent-photo-owner");
        final PropertyResult property = saveProperty(memberId, "동시 등록 매물");
        final byte[] png = TestImages.png();
        final List<Callable<ErrorCode>> tasks = new ArrayList<>();
        for (int index = 0; index < 31; index++) {
            tasks.add(() -> {
                try {
                    propertyPhotoService.uploadPhoto(
                            memberId,
                            property.propertyId(),
                            new UploadPhotoCommand("image/png", png)
                    );
                    return null;
                } catch (JachwiException exception) {
                    return exception.getErrorCode();
                }
            });
        }

        final List<Future<ErrorCode>> futures;
        try (var executor = Executors.newFixedThreadPool(8)) {
            futures = executor.invokeAll(tasks);
        }
        final List<ErrorCode> results = new ArrayList<>();
        for (final Future<ErrorCode> future : futures) {
            results.add(future.get());
        }

        assertThat(results).filteredOn(result -> result == null).hasSize(30);
        assertThat(results).filteredOn(ErrorCode.PHOTO_COUNT_EXCEEDED::equals).hasSize(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM property_photos", Long.class)).isEqualTo(30L);
        assertThat(photoStorage.size()).isEqualTo(30);
    }

    private long saveMember(final String subject) {
        jdbcTemplate.update(
                """
                INSERT INTO members (
                    oauth_provider, oauth_subject, email, display_name, last_login_at
                ) VALUES ('GOOGLE', ?, ?, '회원', CURRENT_TIMESTAMP(6))
                """,
                subject,
                subject + "@example.com"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE oauth_subject = ?",
                Long.class,
                subject
        );
    }

    private PropertyResult saveProperty(final long memberId, final String name) {
        return propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand(name, 10_000_000, 500_000, "앱")
        );
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
