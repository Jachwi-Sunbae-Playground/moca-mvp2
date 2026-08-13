package com.jachwisunbae.property.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.Property;
import com.jachwisunbae.property.domain.PropertyPhoto;
import com.jachwisunbae.property.domain.PropertyName;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class PropertyPhotoRepositoryTest extends RepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PropertyRepository propertyRepository;
    private PropertyPhotoRepository propertyPhotoRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        propertyRepository = new PropertyRepository(jdbcTemplate);
        propertyPhotoRepository = new PropertyPhotoRepository(jdbcTemplate);
    }

    @DisplayName("사진은 매물과 회원 소유권 범위에서 생성 시각과 ID 오름차순으로 조회한다")
    @Test
    void findOwnedPhotosInCreationOrder() {
        final long ownerId = saveMember("photo-owner");
        final long otherId = saveMember("photo-other");
        final Property property = saveProperty(ownerId, "소유 매물");
        final Instant sameTime = Instant.parse("2026-08-10T01:00:00Z");
        final PropertyPhoto first = savePhoto(property, "first", sameTime);
        final PropertyPhoto second = savePhoto(property, "second", sameTime);

        assertThat(propertyPhotoRepository.findAllOwned(ownerId, property.id()))
                .extracting(PropertyPhoto::id)
                .containsExactly(first.id(), second.id());
        assertThat(propertyPhotoRepository.findPreviewOwned(ownerId, property.id()))
                .extracting(PropertyPhoto::id)
                .containsExactly(first.id());
        assertThat(propertyPhotoRepository.findOwned(otherId, property.id(), first.id())).isEmpty();
        assertThat(propertyPhotoRepository.findOwned(ownerId, property.id() + 1, first.id())).isEmpty();
    }

    @DisplayName("사진 저장 키 UNIQUE와 형식·크기 CHECK와 매물 소유자 복합 FK를 강제한다")
    @Test
    void enforcePhotoConstraints() {
        final long ownerId = saveMember("constraint-owner");
        final long otherId = saveMember("constraint-other");
        final Property property = saveProperty(ownerId, "제약 매물");
        final PropertyPhoto existingPhoto = savePhoto(
                property,
                "duplicate",
                Instant.parse("2026-08-10T01:00:00Z")
        );
        final String insertSql = """
                INSERT INTO property_photos (
                    property_id, member_id, storage_key, content_type,
                    size_bytes, checksum_sha256, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(6))
                """;

        assertThatThrownBy(() -> jdbcTemplate.update(
                insertSql, property.id(), ownerId, existingPhoto.storageKey(), "image/png", 1, "0".repeat(64)
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                insertSql, property.id(), ownerId, "bad-type", "image/gif", 1, "0".repeat(64)
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                insertSql, property.id(), ownerId, "bad-size", "image/png", 10_485_761, "0".repeat(64)
        )).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                insertSql, property.id(), otherId, "bad-owner", "image/png", 1, "0".repeat(64)
        )).isInstanceOf(DataAccessException.class);
    }

    @DisplayName("사진 조회 인덱스가 매물·생성 시각·ID 순서로 생성된다")
    @Test
    void createPhotoOrderingIndex() {
        final long memberId = saveMember("photo-explain-owner");
        final Property property = saveProperty(memberId, "대표 사진 매물");
        savePhoto(property, "first", Instant.parse("2026-08-10T01:00:00Z"));
        savePhoto(property, "second", Instant.parse("2026-08-10T02:00:00Z"));

        final List<String> columns = jdbcTemplate.query(
                """
                SELECT COLUMN_NAME
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'property_photos'
                  AND INDEX_NAME = 'idx_property_photos_property_created'
                ORDER BY SEQ_IN_INDEX
                """,
                (resultSet, rowNumber) -> resultSet.getString("COLUMN_NAME")
        );
        final QueryPlan queryPlan = jdbcTemplate.queryForObject(
                """
                EXPLAIN
                SELECT id, property_id, member_id, storage_key, content_type,
                       size_bytes, checksum_sha256, created_at
                FROM property_photos
                WHERE property_id = ?
                  AND member_id = ?
                ORDER BY created_at, id
                """,
                (resultSet, rowNumber) -> new QueryPlan(
                        resultSet.getString("key"),
                        resultSet.getString("possible_keys"),
                        resultSet.getString("Extra"),
                        resultSet.getLong("rows")
                ),
                property.id(),
                memberId
        );

        assertThat(columns).containsExactly("property_id", "created_at", "id");
        assertThat(queryPlan.key() + "," + queryPlan.possibleKeys())
                .contains("idx_property_photos_property_created");
        assertThat(!queryPlan.extra().contains("Using filesort") || queryPlan.estimatedRows() <= 30)
                .as("최대 30장 범위의 정렬만 허용한다: %s", queryPlan)
                .isTrue();
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

    private Property saveProperty(final long memberId, final String name) {
        return propertyRepository.save(Property.create(
                memberId,
                new PropertyName(name),
                new Money(10_000_000),
                new Money(500_000),
                DiscoverySource.from("앱"),
                Instant.parse("2026-08-10T00:00:00Z")
        ));
    }

    private PropertyPhoto savePhoto(final Property property, final String suffix, final Instant createdAt) {
        return propertyPhotoRepository.save(new PropertyPhoto(
                0,
                property.id(),
                property.memberId(),
                "members/%d/properties/%d/%s".formatted(property.memberId(), property.id(), suffix),
                "image/png",
                128,
                "0".repeat(64),
                createdAt
        ));
    }

    private record QueryPlan(String key, String possibleKeys, String extra, long estimatedRows) {
    }
}
