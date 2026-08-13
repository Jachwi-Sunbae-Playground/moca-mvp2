package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.ChecklistCommandService;
import com.jachwisunbae.checklist.service.ChecklistQueryService;
import com.jachwisunbae.checklist.service.dto.command.ChecklistItemCommand;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.checklist.service.dto.command.ReplaceChecklistCommand;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ActiveChecklistServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private ActiveChecklistService activeChecklistService;

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private PropertyQueryService propertyQueryService;

    @Autowired
    private PropertyDeletionService propertyDeletionService;

    @Autowired
    private ChecklistCommandService checklistCommandService;

    @Autowired
    private ChecklistQueryService checklistQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("UPDATE check_items SET is_active = TRUE");
    }

    @DisplayName("회원은 매물의 세 단계에 연결하고 같은 체크리스트를 여러 매물에서 재사용·교체·해제한다")
    @Test
    void manageActiveChecklists() {
        final long memberId = saveMember("active-service-owner");
        final long firstPropertyId = createProperty(memberId, "첫 매물");
        final long secondPropertyId = createProperty(memberId, "둘째 매물");
        final long phoneId = createChecklist(memberId, "전화", CheckStage.ONLINE_PHONE, 201L);
        final long siteId = createChecklist(memberId, "현장", CheckStage.ON_SITE, 101L, 102L);
        final long otherSiteId = createChecklist(memberId, "현장 교체", CheckStage.ON_SITE, 103L);
        final long contractId = createChecklist(memberId, "계약", CheckStage.PRE_CONTRACT, 301L);

        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.ONLINE_PHONE, phoneId);
        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.ON_SITE, siteId);
        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.PRE_CONTRACT, contractId);
        activeChecklistService.assign(memberId, secondPropertyId, CheckStage.ON_SITE, siteId);
        final Timestamp beforeSameAssignment = lastActivityAt(firstPropertyId);

        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.ON_SITE, siteId);

        assertThat(lastActivityAt(firstPropertyId)).isEqualTo(beforeSameAssignment);
        assertThat(propertyQueryService.getProperty(memberId, firstPropertyId).activeChecklists())
                .extracting(result -> result.stage().name())
                .containsExactly("ONLINE_PHONE", "ON_SITE", "PRE_CONTRACT");
        assertThat(checklistQueryService.getChecklist(memberId, siteId).assignedPropertyCount()).isEqualTo(2);

        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.ON_SITE, otherSiteId);

        assertThat(propertyQueryService.getProperty(memberId, firstPropertyId).activeChecklists())
                .extracting(result -> result.checklistId())
                .containsExactly(phoneId, otherSiteId, contractId);
        assertThat(checklistQueryService.getChecklist(memberId, siteId).assignedPropertyCount()).isOne();
        assertThat(checklistQueryService.getChecklist(memberId, otherSiteId).assignedPropertyCount()).isOne();
        assertThat(checklistQueryService.getChecklists(
                memberId,
                CheckStage.ON_SITE,
                PageQuery.of(0, 20)
        ).content()).allSatisfy(result -> assertThat(result.assignedPropertyCount()).isOne());

        activeChecklistService.unassign(memberId, firstPropertyId, CheckStage.ON_SITE);
        activeChecklistService.unassign(memberId, firstPropertyId, CheckStage.ON_SITE);

        assertThat(propertyQueryService.getProperty(memberId, firstPropertyId).activeChecklists())
                .extracting(result -> result.stage())
                .containsExactly(CheckStage.ONLINE_PHONE, CheckStage.PRE_CONTRACT);
        assertThat(checklistQueryService.getChecklist(memberId, otherSiteId).assignedPropertyCount()).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklists WHERE id = ?",
                Long.class,
                otherSiteId
        )).isOne();
    }

    @DisplayName("연결된 체크리스트 변경은 매물 상세에 즉시 반영되고 삭제는 현재 연결만 제거한다")
    @Test
    void reflectLiveChecklistAndCascadeDeletion() {
        final long memberId = saveMember("active-live-owner");
        final long propertyId = createProperty(memberId, "라이브 매물");
        final long checklistId = createChecklist(memberId, "변경 전", CheckStage.ON_SITE, 101L);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, checklistId);

        checklistCommandService.replaceChecklist(
                memberId,
                checklistId,
                new ReplaceChecklistCommand(
                        "변경 후",
                        List.of(
                                ChecklistItemCommand.provided(102L),
                                ChecklistItemCommand.custom(null, "창틀 곰팡이는 괜찮은가?")
                        ),
                        ChecklistRequestMode.V11
                )
        );

        assertThat(propertyQueryService.getProperty(memberId, propertyId).activeChecklists())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.name()).isEqualTo("변경 후");
                    assertThat(result.itemCount()).isEqualTo(2);
                });

        checklistCommandService.deleteChecklist(memberId, checklistId);

        assertThat(propertyQueryService.getProperty(memberId, propertyId).activeChecklists()).isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists WHERE property_id = ?",
                Long.class,
                propertyId
        )).isZero();
    }

    @DisplayName("기존 체크리스트의 기준 항목이 비활성화돼도 현재 템플릿을 활성 연결할 수 있다")
    @Test
    void assignChecklistContainingInactiveItem() {
        final long memberId = saveMember("active-inactive-item-owner");
        final long propertyId = createProperty(memberId, "비활성 항목 매물");
        final long checklistId = createChecklist(memberId, "기존 현장", CheckStage.ON_SITE, 101L);
        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id = 101");

        final var result = activeChecklistService.assign(
                memberId,
                propertyId,
                CheckStage.ON_SITE,
                checklistId
        );

        assertThat(result.itemCount()).isOne();
        assertThat(propertyQueryService.getProperty(memberId, propertyId).activeChecklists())
                .singleElement()
                .extracting(active -> active.checklistId())
                .isEqualTo(checklistId);
    }

    @DisplayName("매물 삭제는 연결만 제거해 공유 체크리스트의 할당 수를 줄이고 다른 매물 연결은 유지한다")
    @Test
    void deletePropertyAndConnection() {
        final long memberId = saveMember("active-property-delete-owner");
        final long firstPropertyId = createProperty(memberId, "삭제 매물");
        final long secondPropertyId = createProperty(memberId, "유지 매물");
        final long checklistId = createChecklist(memberId, "공유 현장", CheckStage.ON_SITE, 101L);
        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.ON_SITE, checklistId);
        activeChecklistService.assign(memberId, secondPropertyId, CheckStage.ON_SITE, checklistId);

        propertyDeletionService.deleteProperty(memberId, firstPropertyId);

        assertThat(checklistQueryService.getChecklist(memberId, checklistId).assignedPropertyCount()).isOne();
        assertThat(propertyQueryService.getProperty(memberId, secondPropertyId).activeChecklists())
                .singleElement()
                .extracting(result -> result.checklistId())
                .isEqualTo(checklistId);
    }

    @DisplayName("매물과 체크리스트 소유권 및 단계 검증 실패는 기존 연결을 변경하지 않는다")
    @Test
    void protectOwnershipAndStage() {
        final long ownerId = saveMember("active-real-owner");
        final long otherId = saveMember("active-other-owner");
        final long propertyId = createProperty(ownerId, "소유 매물");
        final long otherPropertyId = createProperty(otherId, "타인 매물");
        final long checklistId = createChecklist(ownerId, "소유 현장", CheckStage.ON_SITE, 101L);
        final long replacementId = createChecklist(ownerId, "교체 현장", CheckStage.ON_SITE, 102L);
        final long otherChecklistId = createChecklist(otherId, "타인 현장", CheckStage.ON_SITE, 103L);
        final long phoneId = createChecklist(ownerId, "전화", CheckStage.ONLINE_PHONE, 201L);
        activeChecklistService.assign(ownerId, propertyId, CheckStage.ON_SITE, checklistId);

        assertError(
                () -> activeChecklistService.assign(ownerId, propertyId, CheckStage.ON_SITE, otherChecklistId),
                ErrorCode.CHECKLIST_NOT_FOUND
        );
        assertError(
                () -> activeChecklistService.assign(ownerId, otherPropertyId, CheckStage.ON_SITE, replacementId),
                ErrorCode.PROPERTY_NOT_FOUND
        );
        assertError(
                () -> activeChecklistService.assign(ownerId, propertyId, CheckStage.ON_SITE, phoneId),
                ErrorCode.CHECKLIST_STAGE_MISMATCH
        );
        assertError(
                () -> activeChecklistService.assign(ownerId, 999_999L, CheckStage.ON_SITE, replacementId),
                ErrorCode.PROPERTY_NOT_FOUND
        );
        assertError(
                () -> activeChecklistService.assign(ownerId, propertyId, CheckStage.ON_SITE, 999_999L),
                ErrorCode.CHECKLIST_NOT_FOUND
        );
        assertError(
                () -> activeChecklistService.unassign(ownerId, otherPropertyId, CheckStage.ON_SITE),
                ErrorCode.PROPERTY_NOT_FOUND
        );

        assertThat(propertyQueryService.getProperty(ownerId, propertyId).activeChecklists())
                .singleElement()
                .extracting(result -> result.checklistId())
                .isEqualTo(checklistId);
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

    private long createProperty(final long memberId, final String name) {
        return propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand(name, 0, 0, "직접 발견")
        ).propertyId();
    }

    private long createChecklist(
            final long memberId,
            final String name,
            final CheckStage stage,
            final Long... checkItemIds
    ) {
        return checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand(name, stage, List.of(checkItemIds))
        ).checklistId();
    }

    private Timestamp lastActivityAt(final long propertyId) {
        return jdbcTemplate.queryForObject(
                "SELECT last_activity_at FROM properties WHERE id = ?",
                Timestamp.class,
                propertyId
        );
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
