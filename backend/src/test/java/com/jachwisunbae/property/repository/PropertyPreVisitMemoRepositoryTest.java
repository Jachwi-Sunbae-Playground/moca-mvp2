package com.jachwisunbae.property.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.common.RepositoryTest;
import com.jachwisunbae.property.domain.DiscoverySource;
import com.jachwisunbae.property.domain.Money;
import com.jachwisunbae.property.domain.PreVisitMemoField;
import com.jachwisunbae.property.domain.Property;
import com.jachwisunbae.property.domain.PropertyMemo;
import com.jachwisunbae.property.domain.PropertyName;
import com.jachwisunbae.property.domain.PropertyPreVisitMemo;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PropertyPreVisitMemoRepositoryTest extends RepositoryTest {

    private static final Instant SAVED_AT = Instant.parse("2026-08-12T06:00:00.123456Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private PropertyRepository propertyRepository;
    private PropertyQueryRepository propertyQueryRepository;
    private PropertyPreVisitMemoRepository memoRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        propertyRepository = new PropertyRepository(jdbcTemplate);
        propertyQueryRepository = new PropertyQueryRepository(jdbcTemplate);
        memoRepository = new PropertyPreVisitMemoRepository(jdbcTemplate);
    }

    @DisplayName("구조화 사전 메모를 매물마다 한 행으로 생성하고 전체 교체한다")
    @Test
    void upsertOwnedMemo() {
        final long memberId = saveMember("memo-upsert-owner");
        final Property property = saveProperty(memberId, "메모 매물");
        final PropertyPreVisitMemo first = memo("첫 일정", "첫 추가 메모", SAVED_AT);
        final PropertyPreVisitMemo second = memo("둘째 일정", "둘째 추가 메모", SAVED_AT.plusSeconds(1));

        assertThat(memoRepository.upsertOwned(memberId, property.id(), first)).isTrue();
        assertThat(memoRepository.upsertOwned(memberId, property.id(), second)).isTrue();

        assertThat(memoRepository.findOwned(memberId, property.id())).contains(second);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_pre_visit_memos WHERE property_id = ?",
                Long.class,
                property.id()
        )).isOne();
    }

    @DisplayName("구조화 사전 메모 조회와 저장은 매물 소유권을 함께 검증한다")
    @Test
    void isolateMemoOwnership() {
        final long ownerId = saveMember("memo-real-owner");
        final long otherId = saveMember("memo-other");
        final Property property = saveProperty(ownerId, "소유 매물");

        assertThat(memoRepository.upsertOwned(otherId, property.id(), memo("일정", "메모", SAVED_AT)))
                .isFalse();
        assertThat(memoRepository.findOwned(otherId, property.id())).isEmpty();
        assertThat(memoRepository.findOwned(ownerId, property.id())).isEmpty();
    }

    @DisplayName("구조화 행이 없으면 상세 조회는 legacy 메모와 시각을 복구하고 구조화 필드는 비운다")
    @Test
    void fallbackToLegacyMemo() {
        final long memberId = saveMember("memo-fallback-owner");
        final Property property = saveProperty(memberId, "호환 매물");
        jdbcTemplate.update(
                "UPDATE properties SET memo = ?, memo_updated_at = ? WHERE id = ? AND member_id = ?",
                "기존 메모",
                Timestamp.from(SAVED_AT),
                property.id(),
                memberId
        );

        final PropertyPreVisitMemo memo = propertyQueryRepository.findOwnedDetail(memberId, property.id())
                .orElseThrow()
                .memo();

        assertThat(memo.viewingSchedule().value()).isEmpty();
        assertThat(memo.governmentSupport().value()).isEmpty();
        assertThat(memo.additionalMemo().content()).isEqualTo("기존 메모");
        assertThat(memo.savedAt()).isEqualTo(SAVED_AT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_pre_visit_memos WHERE property_id = ?",
                Long.class,
                property.id()
        )).isZero();
    }

    @DisplayName("매물을 삭제하면 구조화 사전 메모도 FK cascade로 삭제한다")
    @Test
    void cascadeMemoDeletion() {
        final long memberId = saveMember("memo-delete-owner");
        final Property property = saveProperty(memberId, "삭제 매물");
        memoRepository.upsertOwned(memberId, property.id(), memo("일정", "메모", SAVED_AT));

        propertyRepository.deleteOwned(memberId, property.id());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_pre_visit_memos WHERE property_id = ?",
                Long.class,
                property.id()
        )).isZero();
    }

    private PropertyPreVisitMemo memo(
            final String viewingSchedule,
            final String additionalMemo,
            final Instant savedAt
    ) {
        return new PropertyPreVisitMemo(
                new PreVisitMemoField(viewingSchedule),
                new PreVisitMemoField("입주 가능일"),
                new PreVisitMemoField("가계약금"),
                new PreVisitMemoField("방 옵션"),
                new PreVisitMemoField("관리비와 공과금"),
                new PreVisitMemoField("통학 시간"),
                new PreVisitMemoField("정부 지원"),
                new PropertyMemo(additionalMemo),
                savedAt
        );
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
                DiscoverySource.from("직접 발견"),
                SAVED_AT.minusSeconds(60)
        ));
    }
}
