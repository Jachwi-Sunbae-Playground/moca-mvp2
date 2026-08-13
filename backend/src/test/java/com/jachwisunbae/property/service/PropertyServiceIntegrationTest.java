package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.exception.client.ResourceNotFoundException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.exception.server.DataInconsistencyException;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.property.service.dto.command.PropertySearchCondition;
import com.jachwisunbae.property.service.dto.command.SavePropertyMemoCommand;
import com.jachwisunbae.property.service.dto.command.UpdatePropertyCommand;
import com.jachwisunbae.property.service.dto.result.PropertyDetailResult;
import com.jachwisunbae.property.service.dto.result.PropertyMemoResult;
import com.jachwisunbae.property.service.dto.result.PropertyResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class PropertyServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private PropertyQueryService propertyQueryService;

    @Autowired
    private PropertyDeletionService propertyDeletionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void deleteData() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
    }

    @DisplayName("한 회원이 여러 매물을 생성하고 기본 정보와 메모를 변경한 뒤 삭제한다")
    @Test
    void manageMultipleProperties() {
        final long memberId = saveMember("service-owner");
        final PropertyResult first = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("첫 매물", 10_000_000, 500_000, "https://example.com/1")
        );
        final PropertyResult second = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("둘째 매물", 20_000_000, 600_000, "부동산에서 발견")
        );

        assertThat(first.propertyId()).isNotEqualTo(second.propertyId());
        assertThat(propertyQueryService.getProperties(
                memberId,
                new PropertySearchCondition("", PageQuery.of(0, 20))
        ).totalElements()).isEqualTo(2L);

        propertyCommandService.updateProperty(
                memberId,
                first.propertyId(),
                new UpdatePropertyCommand(
                        Optional.of("첫 매물 수정"),
                        Optional.empty(),
                        Optional.of(450_000L),
                        Optional.empty()
                )
        );
        propertyCommandService.saveMemo(
                memberId,
                first.propertyId(),
                SavePropertyMemoCommand.legacy("첫 메모")
        );
        propertyCommandService.saveMemo(
                memberId,
                first.propertyId(),
                SavePropertyMemoCommand.legacy("수정한 메모")
        );

        final PropertyDetailResult detail = propertyQueryService.getProperty(memberId, first.propertyId());
        assertThat(detail.name()).isEqualTo("첫 매물 수정");
        assertThat(detail.monthlyRentAmount()).isEqualTo(450_000L);
        assertThat(detail.memo().additionalMemo()).isEqualTo("수정한 메모");
        assertThat(detail.memo().savedAt()).isNotNull();

        propertyDeletionService.deleteProperty(memberId, first.propertyId());

        assertThatThrownBy(() -> propertyQueryService.getProperty(memberId, first.propertyId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(exception -> ((ResourceNotFoundException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);
        assertThat(propertyQueryService.getProperty(memberId, second.propertyId()).name()).isEqualTo("둘째 매물");
    }

    @DisplayName("타인 매물의 조회·수정·메모·삭제는 같은 찾을 수 없음 오류이며 데이터가 바뀌지 않는다")
    @Test
    void protectPropertyOwnership() {
        final long ownerId = saveMember("service-real-owner");
        final long otherId = saveMember("service-other");
        final PropertyResult property = propertyCommandService.createProperty(
                ownerId,
                new CreatePropertyCommand("소유 매물", 10_000_000, 500_000, "원래 경로")
        );

        assertPropertyNotFound(() -> propertyQueryService.getProperty(otherId, property.propertyId()));
        assertPropertyNotFound(() -> propertyCommandService.updateProperty(
                otherId,
                property.propertyId(),
                new UpdatePropertyCommand(
                        Optional.of("변조 이름"),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        ));
        assertPropertyNotFound(() -> propertyCommandService.saveMemo(
                otherId,
                property.propertyId(),
                SavePropertyMemoCommand.legacy("변조 메모")
        ));
        assertPropertyNotFound(() -> propertyDeletionService.deleteProperty(otherId, property.propertyId()));

        final PropertyDetailResult detail = propertyQueryService.getProperty(ownerId, property.propertyId());
        assertThat(detail.name()).isEqualTo("소유 매물");
        assertThat(detail.memo().additionalMemo()).isEmpty();
        assertThat(propertyQueryService.getProperties(
                otherId,
                new PropertySearchCondition("", PageQuery.of(0, 20))
        ).content()).isEmpty();
    }

    @DisplayName("구조화 메모 전체 저장은 legacy 컬럼을 함께 갱신하고 content 저장은 구조화 필드를 보존한다")
    @Test
    void saveStructuredAndLegacyMemoCompatibly() {
        final long memberId = saveMember("service-structured-memo-owner");
        final PropertyResult property = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("구조화 메모 매물", 10_000_000, 500_000, "발견 경로")
        );

        final PropertyMemoResult structured = propertyCommandService.saveMemo(
                memberId,
                property.propertyId(),
                structuredMemo("방문 일정", "구조화 추가 메모")
        );
        final PropertyMemoResult legacy = propertyCommandService.saveMemo(
                memberId,
                property.propertyId(),
                SavePropertyMemoCommand.legacy("legacy 수정 메모")
        );

        assertThat(structured.additionalMemo()).isEqualTo("구조화 추가 메모");
        assertThat(legacy.viewingSchedule()).isEqualTo("방문 일정");
        assertThat(legacy.moveInAvailability()).isEqualTo("입주 가능일");
        assertThat(legacy.additionalMemo()).isEqualTo("legacy 수정 메모");
        assertThat(propertyQueryService.getProperty(memberId, property.propertyId()).memo())
                .isEqualTo(legacy);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT memo FROM properties WHERE id = ?",
                String.class,
                property.propertyId()
        )).isEqualTo("legacy 수정 메모");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT additional_memo FROM property_pre_visit_memos WHERE property_id = ?",
                String.class,
                property.propertyId()
        )).isEqualTo("legacy 수정 메모");
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT saved_at = (
                    SELECT memo_updated_at FROM properties WHERE id = ?
                )
                FROM property_pre_visit_memos
                WHERE property_id = ?
                """,
                Boolean.class,
                property.propertyId(),
                property.propertyId()
        )).isTrue();
    }

    @DisplayName("구조화 메모 저장이 실패하면 legacy 메모를 변경하지 않는다")
    @Test
    void rollbackLegacyMemoWhenStructuredSaveFails() {
        final long memberId = saveMember("service-structured-failure-owner");
        final PropertyResult property = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("구조화 실패 매물", 1, 2, "발견 경로")
        );
        jdbcTemplate.execute("""
                ALTER TABLE property_pre_visit_memos
                ADD CONSTRAINT ck_test_reject_structured_memo
                CHECK (additional_memo <> '구조화 저장 실패')
                """);

        try {
            assertThatThrownBy(() -> propertyCommandService.saveMemo(
                    memberId,
                    property.propertyId(),
                    structuredMemo("방문 일정", "구조화 저장 실패")
            )).isInstanceOf(DataInconsistencyException.class);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT memo FROM properties WHERE id = ?",
                    String.class,
                    property.propertyId()
            )).isEmpty();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_pre_visit_memos WHERE property_id = ?",
                    Long.class,
                    property.propertyId()
            )).isZero();
        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE property_pre_visit_memos
                    DROP CHECK ck_test_reject_structured_memo
                    """);
        }
    }

    @DisplayName("legacy 컬럼 저장이 실패하면 같은 트랜잭션의 구조화 메모도 롤백한다")
    @Test
    void rollbackStructuredMemoWhenLegacySaveFails() {
        final long memberId = saveMember("service-legacy-failure-owner");
        final PropertyResult property = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("legacy 실패 매물", 1, 2, "발견 경로")
        );
        jdbcTemplate.execute("""
                ALTER TABLE properties
                ADD CONSTRAINT ck_test_reject_legacy_memo
                CHECK (memo <> 'legacy 저장 실패')
                """);

        try {
            assertThatThrownBy(() -> propertyCommandService.saveMemo(
                    memberId,
                    property.propertyId(),
                    structuredMemo("방문 일정", "legacy 저장 실패")
            )).isInstanceOf(DataInconsistencyException.class);

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT memo FROM properties WHERE id = ?",
                    String.class,
                    property.propertyId()
            )).isEmpty();
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM property_pre_visit_memos WHERE property_id = ?",
                    Long.class,
                    property.propertyId()
            )).isZero();
        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE properties
                    DROP CHECK ck_test_reject_legacy_memo
                    """);
        }
    }

    @DisplayName("수정 도중 값 객체 검증이 실패하면 기존 매물 값은 유지된다")
    @Test
    void keepPropertyWhenUpdateValidationFails() {
        final long memberId = saveMember("service-rollback");
        final PropertyResult property = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("원래 매물", 10_000_000, 500_000, "원래 경로")
        );

        assertThatThrownBy(() -> propertyCommandService.updateProperty(
                memberId,
                property.propertyId(),
                new UpdatePropertyCommand(
                        Optional.of(" "),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty()
                )
        )).isInstanceOf(IllegalArgumentException.class);

        final PropertyDetailResult detail = propertyQueryService.getProperty(memberId, property.propertyId());
        assertThat(detail.name()).isEqualTo("원래 매물");
        assertThat(detail.updatedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS))
                .isEqualTo(property.updatedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
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

    private SavePropertyMemoCommand structuredMemo(
            final String viewingSchedule,
            final String additionalMemo
    ) {
        return SavePropertyMemoCommand.replace(
                viewingSchedule,
                "입주 가능일",
                "가계약금",
                "방 옵션",
                "관리비와 공과금",
                "통학 시간",
                "정부 지원",
                additionalMemo
        );
    }

    private void assertPropertyNotFound(final Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(exception -> ((ResourceNotFoundException) exception).getErrorCode())
                .isEqualTo(ErrorCode.PROPERTY_NOT_FOUND);
    }
}
