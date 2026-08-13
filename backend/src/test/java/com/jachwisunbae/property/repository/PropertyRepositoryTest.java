package com.jachwisunbae.property.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.Property;
import com.jachwisunbae.property.domain.PropertyName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class PropertyRepositoryTest extends RepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PropertyRepository propertyRepository;
    private PropertyQueryRepository propertyQueryRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        propertyRepository = new PropertyRepository(jdbcTemplate);
        propertyQueryRepository = new PropertyQueryRepository(jdbcTemplate);
    }

    @DisplayName("한 회원은 서로 다른 매물을 여러 개 등록할 수 있다")
    @Test
    void saveMultiplePropertiesForMember() {
        final long memberId = saveMember("member-multiple");

        final Property first = saveProperty(memberId, "첫 번째 매물", "직방", "2026-08-10T01:00:00Z");
        final Property second = saveProperty(memberId, "두 번째 매물", "다방", "2026-08-10T02:00:00Z");

        assertThat(first.id()).isNotEqualTo(second.id());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM properties WHERE member_id = ?",
                Long.class,
                memberId
        )).isEqualTo(2L);
    }

    @DisplayName("목록은 회원과 검색어 범위에서 최근 활동 순으로 페이징한다")
    @Test
    void findOwnedPropertiesByActivityAndQuery() {
        final long ownerId = saveMember("owner-list");
        final long otherId = saveMember("other-list");
        final Property oldProperty = saveProperty(
                ownerId,
                "신림역 원룸",
                "https://example.com/old",
                "2026-08-10T01:00:00Z"
        );
        final Property recentProperty = saveProperty(
                ownerId,
                "봉천역 원룸",
                "앱에서 발견",
                "2026-08-10T03:00:00Z"
        );
        saveProperty(otherId, "신림 타인 매물", "타인", "2026-08-10T04:00:00Z");

        final List<PropertySummaryProjection> all = propertyQueryRepository.findAllOwned(
                ownerId,
                "",
                PageQuery.of(0, 20)
        );
        final List<PropertySummaryProjection> searched = propertyQueryRepository.findAllOwned(
                ownerId,
                "신림",
                PageQuery.of(0, 20)
        );

        assertThat(all).extracting(PropertySummaryProjection::propertyId)
                .containsExactly(recentProperty.id(), oldProperty.id());
        assertThat(searched).extracting(PropertySummaryProjection::propertyId)
                .containsExactly(oldProperty.id());
        assertThat(propertyQueryRepository.countAllOwned(ownerId, "신림")).isEqualTo(1L);
    }

    @DisplayName("검색어의 SQL 와일드카드는 일반 문자로 취급한다")
    @Test
    void escapeLikeWildcards() {
        final long memberId = saveMember("member-like");
        saveProperty(memberId, "할인 10% 매물", "텍스트", "2026-08-10T01:00:00Z");
        saveProperty(memberId, "할인 100 매물", "텍스트", "2026-08-10T02:00:00Z");

        final List<PropertySummaryProjection> result = propertyQueryRepository.findAllOwned(
                memberId,
                "10%",
                PageQuery.of(0, 20)
        );

        assertThat(result).extracting(projection -> projection.name().value())
                .containsExactly("할인 10% 매물");
    }

    @DisplayName("상세 조회와 행 잠금 조회는 소유권 조건을 함께 적용한다")
    @Test
    void findOwnedProperty() {
        final long ownerId = saveMember("owner-detail");
        final long otherId = saveMember("other-detail");
        final Property property = saveProperty(ownerId, "소유 매물", "텍스트", "2026-08-10T01:00:00Z");

        assertThat(propertyQueryRepository.findOwnedDetail(ownerId, property.id())).isPresent();
        assertThat(propertyQueryRepository.findOwnedDetail(otherId, property.id())).isEmpty();
        assertThat(propertyRepository.findOwnedByIdForUpdate(ownerId, property.id())).isPresent();
        assertThat(propertyRepository.findOwnedByIdForUpdate(otherId, property.id())).isEmpty();
    }

    @DisplayName("소유권이 다른 수정과 삭제 요청은 매물 데이터를 변경하지 않는다")
    @Test
    void rejectOtherMembersMutation() {
        final long ownerId = saveMember("owner-mutation");
        final long otherId = saveMember("other-mutation");
        final Property property = saveProperty(ownerId, "원래 이름", "원래 경로", "2026-08-10T01:00:00Z");
        final Property forgedProperty = new Property(
                property.id(),
                otherId,
                new PropertyName("변조 이름"),
                property.depositAmount(),
                property.monthlyRentAmount(),
                property.discoverySource(),
                property.memo(),
                property.memoUpdatedAt(),
                Instant.parse("2026-08-10T02:00:00Z"),
                property.createdAt(),
                Instant.parse("2026-08-10T02:00:00Z")
        );

        assertThat(propertyRepository.updateBasicInfo(forgedProperty)).isFalse();
        assertThat(propertyRepository.deleteOwned(otherId, property.id())).isFalse();
        assertThat(propertyQueryRepository.findOwnedDetail(ownerId, property.id()))
                .get()
                .extracting(projection -> projection.name().value())
                .isEqualTo("원래 이름");
    }

    @DisplayName("DB는 매물의 회원 FK와 금액·발견 경로 유형 CHECK를 강제한다")
    @Test
    void enforcePropertyConstraints() {
        final long memberId = saveMember("member-constraint");
        final String insertSql = """
                INSERT INTO properties (
                    member_id, name, deposit_amount, monthly_rent_amount,
                    discovery_source_type, discovery_source,
                    last_activity_at, created_at, updated_at
                ) VALUES (?, '매물', ?, 0, ?, '출처', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """;

        assertThatThrownBy(() -> jdbcTemplate.update(insertSql, 999_999L, 0, "TEXT"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(insertSql, memberId, -1, "TEXT"))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(insertSql, memberId, 0, "UNKNOWN"))
                .isInstanceOf(DataAccessException.class);
    }

    @DisplayName("매물 목록·검색은 회원별 최근 활동 인덱스를 후보로 사용한다")
    @Test
    void explainPropertyListAndSearchIndex() {
        final long memberId = saveMember("property-explain-owner");
        for (int index = 0; index < 120; index++) {
            saveProperty(
                    memberId,
                    "대표 원룸 " + index,
                    "직접 발견",
                    Instant.parse("2026-08-10T00:00:00Z").plusSeconds(index).toString()
            );
        }

        final List<Map<String, Object>> listPlan = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT p.id
                FROM properties p
                WHERE p.member_id = ?
                ORDER BY p.last_activity_at DESC, p.id DESC
                LIMIT 20
                """,
                memberId
        );
        final List<Map<String, Object>> searchPlan = jdbcTemplate.queryForList(
                """
                EXPLAIN SELECT p.id
                FROM properties p
                WHERE p.member_id = ?
                  AND p.name LIKE '%원룸%'
                ORDER BY p.last_activity_at DESC, p.id DESC
                LIMIT 20
                """,
                memberId
        );

        assertThat(listPlan).anySatisfy(this::assertPropertyActivityIndex);
        assertThat(searchPlan).anySatisfy(this::assertPropertyActivityIndex);
    }

    private void assertPropertyActivityIndex(final Map<String, Object> row) {
        assertThat(row.get("table")).isEqualTo("p");
        assertThat(row.get("key") + "," + row.get("possible_keys"))
                .contains("idx_properties_member_activity");
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

    private Property saveProperty(
            final long memberId,
            final String name,
            final String source,
            final String now
    ) {
        final Instant timestamp = Instant.parse(now);
        return propertyRepository.save(Property.create(
                memberId,
                new PropertyName(name),
                new Money(10_000_000),
                new Money(500_000),
                DiscoverySource.from(source),
                timestamp
        ));
    }
}
