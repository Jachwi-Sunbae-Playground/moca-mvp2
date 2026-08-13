package com.jachwisunbae.visit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import com.jachwisunbae.checklist.service.ChecklistCommandService;
import com.jachwisunbae.checklist.service.dto.command.ChecklistItemCommand;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.checklist.service.dto.command.ReplaceChecklistCommand;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.common.page.PageQuery;
import com.jachwisunbae.property.service.ActiveChecklistService;
import com.jachwisunbae.property.service.PropertyCommandService;
import com.jachwisunbae.property.service.PropertyDeletionService;
import com.jachwisunbae.property.service.PropertyQueryService;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.visit.service.dto.command.CompleteVisitCommand;
import com.jachwisunbae.visit.domain.InlineMemo;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemMemoCommand;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemStatusCommand;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class VisitServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private VisitCommandService visitCommandService;

    @Autowired
    private VisitQueryService visitQueryService;

    @Autowired
    private PropertyCommandService propertyCommandService;

    @Autowired
    private PropertyQueryService propertyQueryService;

    @Autowired
    private PropertyDeletionService propertyDeletionService;

    @Autowired
    private ChecklistCommandService checklistCommandService;

    @Autowired
    private ActiveChecklistService activeChecklistService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM members");
        jdbcTemplate.update("UPDATE check_items SET is_active = TRUE");
    }

    @DisplayName("복수 방문은 각 시점의 모든 활성 체크리스트를 독립 스냅샷으로 보존하고 최근 방문을 연동한다")
    @Test
    void manageMultipleVisitsAndImmutableSnapshots() {
        final long memberId = saveMember("visit-lifecycle-owner");
        final long propertyId = createProperty(memberId, "방문 매물");
        final long phoneId = createChecklist(memberId, "전화 원본", CheckStage.ONLINE_PHONE, 201L, 202L);
        final long siteId = createChecklist(memberId, "현장 원본", CheckStage.ON_SITE, 101L, 102L);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ONLINE_PHONE, phoneId);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, siteId);

        final var first = visitCommandService.startVisit(memberId, propertyId);

        assertThat(first.stages()).hasSize(2);
        assertThat(first.stages()).extracting(stage -> stage.stage())
                .containsExactly(CheckStage.ONLINE_PHONE, CheckStage.ON_SITE);
        assertThat(first.summary().totalCount()).isEqualTo(4);
        assertThat(first.summary().unconfirmedCount()).isEqualTo(4);
        assertThat(first.stages()).allSatisfy(stage ->
                assertThat(stage.items()).allSatisfy(item -> {
                    assertThat(item.status()).isEqualTo("UNCONFIRMED");
                    assertThat(item.statusVersion()).isZero();
                    assertThat(item.inlineMemo()).isEmpty();
                    assertThat(item.memoVersion()).isZero();
                    assertThat(item.memoSavedAt()).isNull();
                })
        );

        final long firstItemId = first.stages().getFirst().items().getFirst().visitItemId();
        final var saved = visitCommandService.updateItemStatus(
                memberId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemStatusCommand("GOOD", 0)
        );
        assertThat(saved.item().statusVersion()).isOne();
        assertThat(saved.visitSummary().checkedCount()).isOne();

        final var completed = visitCommandService.completeVisit(
                memberId,
                first.visitId(),
                new CompleteVisitCommand("COMPLETED")
        );
        final var repeated = visitCommandService.completeVisit(
                memberId,
                first.visitId(),
                new CompleteVisitCommand("COMPLETED")
        );
        assertThat(repeated.completedAt()).isEqualTo(completed.completedAt());

        final var afterCompletion = visitCommandService.updateItemStatus(
                memberId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemStatusCommand("CAUTION", 1)
        );
        assertThat(afterCompletion.item().status()).isEqualTo("CAUTION");
        assertThat(visitQueryService.getVisit(memberId, first.visitId()).completedAt())
                .isEqualTo(completed.completedAt());

        final var memoAfterCompletion = visitCommandService.updateItemMemo(
                memberId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemMemoCommand(new InlineMemo("완료 뒤 메모"), 0)
        );
        assertThat(memoAfterCompletion.memoVersion()).isOne();
        assertThat(visitQueryService.getVisit(memberId, first.visitId()).completedAt())
                .isEqualTo(completed.completedAt());

        checklistCommandService.replaceChecklist(
                memberId,
                siteId,
                new ReplaceChecklistCommand("현장 변경", List.of(103L))
        );
        checklistCommandService.deleteChecklist(memberId, phoneId);
        jdbcTemplate.update("UPDATE check_items SET is_active = FALSE WHERE id IN (101, 201)");

        final var oldVisit = visitQueryService.getVisit(memberId, first.visitId());
        assertThat(oldVisit.stages().getFirst().sourceChecklistId()).isNull();
        assertThat(oldVisit.stages().getFirst().checklistName()).isEqualTo("전화 원본");
        assertThat(oldVisit.stages().getFirst().items()).extracting(item -> item.sourceCheckItemId())
                .containsExactly(201L, 202L);
        assertThat(oldVisit.stages().get(1).checklistName()).isEqualTo("현장 원본");
        assertThat(oldVisit.stages().get(1).items()).extracting(item -> item.sourceCheckItemId())
                .containsExactly(101L, 102L);

        final var second = visitCommandService.startVisit(memberId, propertyId);
        assertThat(second.stages()).singleElement().satisfies(stage -> {
            assertThat(stage.checklistName()).isEqualTo("현장 변경");
            assertThat(stage.items()).extracting(item -> item.sourceCheckItemId()).containsExactly(103L);
        });

        final var visits = visitQueryService.getVisits(memberId, propertyId, PageQuery.of(0, 20));
        assertThat(visits.content()).extracting(result -> result.visitId())
                .containsExactly(second.visitId(), first.visitId());
        final var property = propertyQueryService.getProperty(memberId, propertyId);
        assertThat(property.visitCount()).isEqualTo(2);
        assertThat(property.recentVisit().visitId()).isEqualTo(second.visitId());
        assertThat(property.recentVisit().totalCount()).isOne();

        propertyDeletionService.deleteProperty(memberId, propertyId);

        assertError(() -> visitQueryService.getVisit(memberId, first.visitId()), ErrorCode.VISIT_NOT_FOUND);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visits", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_check_items", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklists WHERE id = ?",
                Long.class,
                siteId
        )).isOne();
    }

    @DisplayName("CUSTOM은 PROVIDED와 함께 방문에 복사되고 원본 수정·삭제 뒤에도 질문·순서·origin을 보존한다")
    @Test
    void snapshotCustomItemsImmutably() {
        final long memberId = saveMember("custom-visit-owner");
        final long propertyId = createProperty(memberId, "CUSTOM 방문 매물");
        final var checklist = checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand(
                        "혼합 현장",
                        CheckStage.ON_SITE,
                        List.of(
                                ChecklistItemCommand.provided(101),
                                ChecklistItemCommand.custom(null, "창틀 곰팡이는 괜찮은가?")
                        ),
                        ChecklistRequestMode.V11
                )
        );
        final long providedChecklistItemId = checklist.items().getFirst().checklistItemId();
        final long customChecklistItemId = checklist.items().get(1).checklistItemId();
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, checklist.checklistId());

        final var first = visitCommandService.startVisit(memberId, propertyId);
        assertThat(first.stages().getFirst().items()).extracting(item -> item.origin())
                .containsExactly(ChecklistItemOrigin.PROVIDED, ChecklistItemOrigin.CUSTOM);
        assertThat(first.stages().getFirst().items()).extracting(item -> item.sourceChecklistItemId())
                .containsExactly(providedChecklistItemId, customChecklistItemId);
        assertThat(first.stages().getFirst().items()).extracting(item -> item.sourceCheckItemId())
                .containsExactly(101L, null);
        assertThat(first.stages().getFirst().items().get(1).guide()).isNull();

        checklistCommandService.replaceChecklist(
                memberId,
                checklist.checklistId(),
                new ReplaceChecklistCommand(
                        "혼합 현장 변경",
                        List.of(
                                ChecklistItemCommand.custom(customChecklistItemId, "환기 상태는 괜찮은가?"),
                                ChecklistItemCommand.provided(101)
                        ),
                        ChecklistRequestMode.V11
                )
        );
        final var second = visitCommandService.startVisit(memberId, propertyId);
        assertThat(second.stages().getFirst().items()).extracting(item -> item.question())
                .containsExactly("환기 상태는 괜찮은가?", checklist.items().getFirst().question());
        assertThat(first.stages().getFirst().items()).extracting(item -> item.question())
                .containsExactly(checklist.items().getFirst().question(), "창틀 곰팡이는 괜찮은가?");

        checklistCommandService.replaceChecklist(
                memberId,
                checklist.checklistId(),
                new ReplaceChecklistCommand(
                        "CUSTOM 제거",
                        List.of(ChecklistItemCommand.provided(101)),
                        ChecklistRequestMode.V11
                )
        );
        final var firstAfterDeletion = visitQueryService.getVisit(memberId, first.visitId());
        final var secondAfterDeletion = visitQueryService.getVisit(memberId, second.visitId());
        assertThat(firstAfterDeletion.stages().getFirst().items().get(1).sourceChecklistItemId()).isNull();
        assertThat(firstAfterDeletion.stages().getFirst().items().get(1).question())
                .isEqualTo("창틀 곰팡이는 괜찮은가?");
        assertThat(firstAfterDeletion.stages().getFirst().items().get(1).origin())
                .isEqualTo(ChecklistItemOrigin.CUSTOM);
        assertThat(secondAfterDeletion.stages().getFirst().items().getFirst().sourceChecklistItemId()).isNull();
        assertThat(secondAfterDeletion.stages().getFirst().items().getFirst().question())
                .isEqualTo("환기 상태는 괜찮은가?");

        checklistCommandService.deleteChecklist(memberId, checklist.checklistId());
        final var afterChecklistDeletion = visitQueryService.getVisit(memberId, first.visitId());
        assertThat(afterChecklistDeletion.stages().getFirst().sourceChecklistId()).isNull();
        assertThat(afterChecklistDeletion.stages().getFirst().items()).extracting(item -> item.sourceChecklistItemId())
                .containsOnlyNulls();
        assertThat(afterChecklistDeletion.stages().getFirst().items()).extracting(item -> item.question())
                .containsExactly(checklist.items().getFirst().question(), "창틀 곰팡이는 괜찮은가?");
    }

    @DisplayName("상태와 인라인 메모 CAS는 값·버전·저장 시각을 독립 변경하고 같은 메모도 버전을 증가시킨다")
    @Test
    void updateStatusAndMemoIndependently() {
        final long memberId = saveMember("visit-independent-cas-owner");
        final long propertyId = createProperty(memberId, "독립 CAS 매물");
        final long checklistId = createChecklist(memberId, "현장", CheckStage.ON_SITE, 101L);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, checklistId);
        final var visit = visitCommandService.startVisit(memberId, propertyId);
        final long itemId = visit.stages().getFirst().items().getFirst().visitItemId();
        final var initial = visit.stages().getFirst().items().getFirst();

        final var memoSaved = visitCommandService.updateItemMemo(
                memberId,
                visit.visitId(),
                itemId,
                new UpdateVisitItemMemoCommand(new InlineMemo("  현관 소음 확인  "), 0)
        );
        final var afterMemo = visitQueryService.getVisit(memberId, visit.visitId())
                .stages().getFirst().items().getFirst();

        assertThat(memoSaved.memo()).isEqualTo("  현관 소음 확인  ");
        assertThat(afterMemo.memoVersion()).isOne();
        assertThat(afterMemo.memoSavedAt()).isNotNull();
        assertThat(afterMemo.status()).isEqualTo(initial.status());
        assertThat(afterMemo.statusVersion()).isEqualTo(initial.statusVersion());
        assertThat(afterMemo.statusSavedAt()).isEqualTo(initial.statusSavedAt());
        assertThat(visitQueryService.getVisit(memberId, visit.visitId()).summary())
                .isEqualTo(visit.summary());

        visitCommandService.updateItemStatus(
                memberId,
                visit.visitId(),
                itemId,
                new UpdateVisitItemStatusCommand("CAUTION", 0)
        );
        final var afterStatus = visitQueryService.getVisit(memberId, visit.visitId())
                .stages().getFirst().items().getFirst();

        assertThat(afterStatus.status()).isEqualTo("CAUTION");
        assertThat(afterStatus.statusVersion()).isOne();
        assertThat(afterStatus.statusSavedAt()).isAfterOrEqualTo(initial.statusSavedAt());
        assertThat(afterStatus.inlineMemo()).isEqualTo(afterMemo.inlineMemo());
        assertThat(afterStatus.memoVersion()).isEqualTo(afterMemo.memoVersion());
        assertThat(afterStatus.memoSavedAt()).isEqualTo(afterMemo.memoSavedAt());

        visitCommandService.updateItemMemo(
                memberId,
                visit.visitId(),
                itemId,
                new UpdateVisitItemMemoCommand(new InlineMemo("  현관 소음 확인  "), 1)
        );
        final var afterSameMemo = visitQueryService.getVisit(memberId, visit.visitId())
                .stages().getFirst().items().getFirst();
        assertThat(afterSameMemo.memoVersion()).isEqualTo(2);

        visitCommandService.updateItemMemo(
                memberId,
                visit.visitId(),
                itemId,
                new UpdateVisitItemMemoCommand(new InlineMemo(""), 2)
        );
        final var afterDelete = visitQueryService.getVisit(memberId, visit.visitId())
                .stages().getFirst().items().getFirst();
        assertThat(afterDelete.inlineMemo()).isEmpty();
        assertThat(afterDelete.memoVersion()).isEqualTo(3);
        assertThat(afterDelete.status()).isEqualTo("CAUTION");
        assertThat(afterDelete.statusVersion()).isOne();
    }

    @DisplayName("메모 저장 뒤 방문 활동 시각 갱신이 실패하면 메모 변경도 함께 롤백한다")
    @Test
    void rollbackMemoWhenVisitActivityUpdateFails() {
        final long memberId = saveMember("visit-memo-rollback-owner");
        final long propertyId = createProperty(memberId, "메모 롤백 매물");
        final long checklistId = createChecklist(memberId, "현장", CheckStage.ON_SITE, 101L);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, checklistId);
        final var visit = visitCommandService.startVisit(memberId, propertyId);
        final long itemId = visit.stages().getFirst().items().getFirst().visitItemId();
        jdbcTemplate.execute("""
                ALTER TABLE visits
                ADD CONSTRAINT ck_test_visit_activity_unchanged CHECK (updated_at = started_at)
                """);

        try {
            assertError(() -> visitCommandService.updateItemMemo(
                    memberId,
                    visit.visitId(),
                    itemId,
                    new UpdateVisitItemMemoCommand(new InlineMemo("롤백 대상"), 0)
            ), ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            jdbcTemplate.execute("ALTER TABLE visits DROP CHECK ck_test_visit_activity_unchanged");
        }

        final var item = visitQueryService.getVisit(memberId, visit.visitId())
                .stages().getFirst().items().getFirst();
        assertThat(item.inlineMemo()).isEmpty();
        assertThat(item.memoVersion()).isZero();
        assertThat(item.memoSavedAt()).isNull();
    }

    @DisplayName("방문·항목 소유권과 visitId-itemId 관계 및 버전 충돌을 구분한다")
    @Test
    void protectOwnershipRelationshipAndVersion() {
        final long ownerId = saveMember("visit-owner");
        final long otherId = saveMember("visit-other");
        final long propertyId = createProperty(ownerId, "소유 매물");
        final long otherPropertyId = createProperty(otherId, "타인 매물");
        final long checklistId = createChecklist(ownerId, "현장", CheckStage.ON_SITE, 101L, 102L);
        final long otherChecklistId = createChecklist(otherId, "타인 현장", CheckStage.ON_SITE, 103L);
        activeChecklistService.assign(ownerId, propertyId, CheckStage.ON_SITE, checklistId);
        activeChecklistService.assign(otherId, otherPropertyId, CheckStage.ON_SITE, otherChecklistId);
        final var first = visitCommandService.startVisit(ownerId, propertyId);
        final var second = visitCommandService.startVisit(ownerId, propertyId);
        final var other = visitCommandService.startVisit(otherId, otherPropertyId);
        final long firstItemId = first.stages().getFirst().items().getFirst().visitItemId();
        final long secondItemId = second.stages().getFirst().items().getFirst().visitItemId();
        final long otherItemId = other.stages().getFirst().items().getFirst().visitItemId();

        assertError(() -> visitQueryService.getVisits(ownerId, otherPropertyId, PageQuery.of(0, 20)),
                ErrorCode.PROPERTY_NOT_FOUND);
        assertError(() -> visitCommandService.startVisit(ownerId, otherPropertyId), ErrorCode.PROPERTY_NOT_FOUND);
        assertError(() -> visitQueryService.getVisit(ownerId, other.visitId()), ErrorCode.VISIT_NOT_FOUND);
        assertError(() -> visitCommandService.updateItemStatus(
                ownerId,
                first.visitId(),
                secondItemId,
                new UpdateVisitItemStatusCommand("GOOD", 0)
        ), ErrorCode.VISIT_ITEM_NOT_FOUND);
        assertError(() -> visitCommandService.updateItemStatus(
                ownerId,
                first.visitId(),
                otherItemId,
                new UpdateVisitItemStatusCommand("GOOD", 0)
        ), ErrorCode.VISIT_ITEM_NOT_FOUND);

        visitCommandService.updateItemStatus(
                ownerId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemStatusCommand("GOOD", 0)
        );
        assertError(() -> visitCommandService.updateItemStatus(
                ownerId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemStatusCommand("CAUTION", 0)
        ), ErrorCode.VISIT_ITEM_STATUS_VERSION_CONFLICT);
        assertThat(visitQueryService.getVisit(ownerId, first.visitId())
                .stages().getFirst().items().getFirst().status()).isEqualTo("GOOD");

        assertError(() -> visitCommandService.updateItemMemo(
                ownerId,
                first.visitId(),
                secondItemId,
                new UpdateVisitItemMemoCommand(new InlineMemo("다른 방문"), 0)
        ), ErrorCode.VISIT_ITEM_NOT_FOUND);
        visitCommandService.updateItemMemo(
                ownerId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemMemoCommand(new InlineMemo("메모"), 0)
        );
        assertError(() -> visitCommandService.updateItemMemo(
                ownerId,
                first.visitId(),
                firstItemId,
                new UpdateVisitItemMemoCommand(new InlineMemo("충돌"), 0)
        ), ErrorCode.VISIT_ITEM_MEMO_VERSION_CONFLICT);
    }

    @DisplayName("활성 체크리스트가 없으면 방문을 만들지 않고 스냅샷 중간 실패는 루트까지 롤백한다")
    @Test
    void rollbackWholeVisitCreation() {
        final long memberId = saveMember("visit-rollback-owner");
        final long propertyId = createProperty(memberId, "롤백 매물");

        assertError(() -> visitCommandService.startVisit(memberId, propertyId),
                ErrorCode.ACTIVE_CHECKLIST_REQUIRED);

        final long checklistId = createChecklist(memberId, "현장", CheckStage.ON_SITE, 101L);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, checklistId);
        jdbcTemplate.execute("""
                ALTER TABLE visit_check_items
                MODIFY question_snapshot VARCHAR(5) NOT NULL
                """);

        try {
            assertError(() -> visitCommandService.startVisit(memberId, propertyId),
                    ErrorCode.CHECKLIST_SNAPSHOT_FAILED);
        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE visit_check_items
                    MODIFY question_snapshot VARCHAR(500) NOT NULL
                    """);
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visits", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_stage_snapshots", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM visit_check_items", Long.class)).isZero();
    }

    @DisplayName("반복 사용은 같은 매물 복수 방문이 아니라 완료 방문이 있는 서로 다른 매물 수로 판정한다")
    @Test
    void countRepeatUsageByDistinctProperties() {
        final long memberId = saveMember("visit-repeat-owner");
        final long firstPropertyId = createProperty(memberId, "첫 매물");
        final long secondPropertyId = createProperty(memberId, "둘째 매물");
        final long checklistId = createChecklist(memberId, "공유 현장", CheckStage.ON_SITE, 101L);
        activeChecklistService.assign(memberId, firstPropertyId, CheckStage.ON_SITE, checklistId);
        activeChecklistService.assign(memberId, secondPropertyId, CheckStage.ON_SITE, checklistId);
        final var firstVisit = visitCommandService.startVisit(memberId, firstPropertyId);
        final var repeatedFirstPropertyVisit = visitCommandService.startVisit(memberId, firstPropertyId);
        visitCommandService.completeVisit(
                memberId,
                firstVisit.visitId(),
                new CompleteVisitCommand("COMPLETED")
        );
        visitCommandService.completeVisit(
                memberId,
                repeatedFirstPropertyVisit.visitId(),
                new CompleteVisitCommand("COMPLETED")
        );

        assertThat(completedPropertyCount(memberId)).isOne();

        final var secondVisit = visitCommandService.startVisit(memberId, secondPropertyId);
        visitCommandService.completeVisit(
                memberId,
                secondVisit.visitId(),
                new CompleteVisitCommand("COMPLETED")
        );

        assertThat(completedPropertyCount(memberId)).isEqualTo(2L);
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

    private long completedPropertyCount(final long memberId) {
        return jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT property_id)
                FROM visits
                WHERE member_id = ?
                  AND status = 'COMPLETED'
                """,
                Long.class,
                memberId
        );
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
