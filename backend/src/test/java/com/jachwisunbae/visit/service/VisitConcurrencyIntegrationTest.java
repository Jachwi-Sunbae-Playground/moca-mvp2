package com.jachwisunbae.visit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.ChecklistCommandService;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.property.service.ActiveChecklistService;
import com.jachwisunbae.property.service.PropertyCommandService;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import com.jachwisunbae.visit.service.dto.command.CompleteVisitCommand;
import com.jachwisunbae.visit.domain.InlineMemo;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemMemoCommand;
import com.jachwisunbae.visit.service.dto.command.UpdateVisitItemStatusCommand;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class VisitConcurrencyIntegrationTest extends IntegrationTest {

    @Autowired
    private VisitCommandService visitCommandService;

    @Autowired
    private VisitQueryService visitQueryService;

    @Autowired
    private PropertyCommandService propertyCommandService;

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
    }

    @DisplayName("같은 항목의 같은 버전 동시 저장은 하나만 성공하고 서로 다른 항목 저장은 모두 보존한다")
    @Test
    void saveItemsConcurrently() throws Exception {
        final Fixture fixture = fixture("visit-concurrency-items");
        final var visit = visitCommandService.startVisit(fixture.memberId(), fixture.propertyId());
        final long firstItemId = visit.stages().getFirst().items().getFirst().visitItemId();
        final long secondItemId = visit.stages().getFirst().items().get(1).visitItemId();

        final List<String> sameItemResults = runTogether(
                () -> saveResult(fixture.memberId(), visit.visitId(), firstItemId, "GOOD", 0),
                () -> saveResult(fixture.memberId(), visit.visitId(), firstItemId, "CAUTION", 0)
        );

        assertThat(sameItemResults).containsExactlyInAnyOrder("SUCCESS", "VISIT_ITEM_STATUS_VERSION_CONFLICT");
        assertThat(visitQueryService.getVisit(fixture.memberId(), visit.visitId())
                .stages().getFirst().items().getFirst().statusVersion()).isOne();

        final List<String> differentItemResults = runTogether(
                () -> saveResult(fixture.memberId(), visit.visitId(), firstItemId, "UNCONFIRMED", 1),
                () -> saveResult(fixture.memberId(), visit.visitId(), secondItemId, "GOOD", 0)
        );

        assertThat(differentItemResults).containsOnly("SUCCESS");
        final var items = visitQueryService.getVisit(fixture.memberId(), visit.visitId())
                .stages().getFirst().items();
        assertThat(items.getFirst().status()).isEqualTo("UNCONFIRMED");
        assertThat(items.getFirst().statusVersion()).isEqualTo(2);
        assertThat(items.get(1).status()).isEqualTo("GOOD");
        assertThat(items.get(1).statusVersion()).isOne();
    }

    @DisplayName("같은 메모 버전 동시 저장은 하나만 성공한다")
    @Test
    void saveSameMemoChannelConcurrently() throws Exception {
        final Fixture fixture = fixture("visit-concurrency-memo");
        final var visit = visitCommandService.startVisit(fixture.memberId(), fixture.propertyId());
        final long itemId = visit.stages().getFirst().items().getFirst().visitItemId();

        final List<String> results = runTogether(
                () -> memoSaveResult(fixture.memberId(), visit.visitId(), itemId, "첫 메모", 0),
                () -> memoSaveResult(fixture.memberId(), visit.visitId(), itemId, "둘째 메모", 0)
        );

        assertThat(results).containsExactlyInAnyOrder("SUCCESS", "VISIT_ITEM_MEMO_VERSION_CONFLICT");
        final var item = visitQueryService.getVisit(fixture.memberId(), visit.visitId())
                .stages().getFirst().items().getFirst();
        assertThat(item.inlineMemo()).isIn("첫 메모", "둘째 메모");
        assertThat(item.memoVersion()).isOne();
        assertThat(item.statusVersion()).isZero();
    }

    @DisplayName("같은 항목의 상태와 메모 동시 저장은 둘 다 성공하고 독립 결과를 보존한다")
    @Test
    void saveStatusAndMemoConcurrently() throws Exception {
        final Fixture fixture = fixture("visit-concurrency-independent-channels");
        final var visit = visitCommandService.startVisit(fixture.memberId(), fixture.propertyId());
        final long itemId = visit.stages().getFirst().items().getFirst().visitItemId();

        final List<String> results = runTogether(
                () -> saveResult(fixture.memberId(), visit.visitId(), itemId, "GOOD", 0),
                () -> memoSaveResult(fixture.memberId(), visit.visitId(), itemId, "동시 메모", 0)
        );

        assertThat(results).containsOnly("SUCCESS");
        final var item = visitQueryService.getVisit(fixture.memberId(), visit.visitId())
                .stages().getFirst().items().getFirst();
        assertThat(item.status()).isEqualTo("GOOD");
        assertThat(item.statusVersion()).isOne();
        assertThat(item.inlineMemo()).isEqualTo("동시 메모");
        assertThat(item.memoVersion()).isOne();
    }

    @DisplayName("같은 매물의 동시 방문 시작은 서로 독립된 방문과 완전한 스냅샷을 각각 만든다")
    @Test
    void startMultipleVisitsConcurrently() throws Exception {
        final Fixture fixture = fixture("visit-concurrency-start");

        final List<String> visitIds = runTogether(
                () -> String.valueOf(visitCommandService.startVisit(
                        fixture.memberId(),
                        fixture.propertyId()
                ).visitId()),
                () -> String.valueOf(visitCommandService.startVisit(
                        fixture.memberId(),
                        fixture.propertyId()
                ).visitId())
        );

        assertThat(visitIds).doesNotHaveDuplicates();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM visits WHERE property_id = ?",
                Long.class,
                fixture.propertyId()
        )).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM visit_check_items vci
                JOIN visit_stage_snapshots vss ON vss.id = vci.visit_stage_snapshot_id
                JOIN visits v ON v.id = vss.visit_id
                WHERE v.property_id = ?
                """,
                Long.class,
                fixture.propertyId()
        )).isEqualTo(4L);
    }

    @DisplayName("항목 저장과 방문 완료 동시 요청은 직렬화되어 완료 상태와 항목 변경을 모두 보존한다")
    @Test
    void saveItemAndCompleteConcurrently() throws Exception {
        final Fixture fixture = fixture("visit-concurrency-complete");
        final var visit = visitCommandService.startVisit(fixture.memberId(), fixture.propertyId());
        final long itemId = visit.stages().getFirst().items().getFirst().visitItemId();

        final List<String> results = runTogether(
                () -> saveResult(fixture.memberId(), visit.visitId(), itemId, "CAUTION", 0),
                () -> {
                    visitCommandService.completeVisit(
                            fixture.memberId(),
                            visit.visitId(),
                            new CompleteVisitCommand("COMPLETED")
                    );
                    return "SUCCESS";
                }
        );

        assertThat(results).containsOnly("SUCCESS");
        final var detail = visitQueryService.getVisit(fixture.memberId(), visit.visitId());
        assertThat(detail.status()).isEqualTo("COMPLETED");
        assertThat(detail.completedAt()).isNotNull();
        assertThat(detail.stages().getFirst().items().getFirst().status()).isEqualTo("CAUTION");
    }

    @DisplayName("메모 저장과 방문 완료 동시 요청은 최초 완료 시각과 메모를 모두 보존한다")
    @Test
    void saveMemoAndCompleteConcurrently() throws Exception {
        final Fixture fixture = fixture("visit-concurrency-memo-complete");
        final var visit = visitCommandService.startVisit(fixture.memberId(), fixture.propertyId());
        final long itemId = visit.stages().getFirst().items().getFirst().visitItemId();

        final List<String> results = runTogether(
                () -> memoSaveResult(fixture.memberId(), visit.visitId(), itemId, "완료 경합 메모", 0),
                () -> {
                    visitCommandService.completeVisit(
                            fixture.memberId(),
                            visit.visitId(),
                            new CompleteVisitCommand("COMPLETED")
                    );
                    return "SUCCESS";
                }
        );
        final var completed = visitQueryService.getVisit(fixture.memberId(), visit.visitId());
        final var repeated = visitCommandService.completeVisit(
                fixture.memberId(),
                visit.visitId(),
                new CompleteVisitCommand("COMPLETED")
        );

        assertThat(results).containsOnly("SUCCESS");
        assertThat(completed.status()).isEqualTo("COMPLETED");
        assertThat(completed.stages().getFirst().items().getFirst().inlineMemo()).isEqualTo("완료 경합 메모");
        assertThat(repeated.completedAt()).isEqualTo(completed.completedAt());
    }

    private List<String> runTogether(
            final Callable<String> first,
            final Callable<String> second
    ) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        try {
            final Future<String> firstFuture = executor.submit(awaitStart(ready, start, first));
            final Future<String> secondFuture = executor.submit(awaitStart(ready, start, second));
            ready.await();
            start.countDown();
            return List.of(firstFuture.get(), secondFuture.get());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<String> awaitStart(
            final CountDownLatch ready,
            final CountDownLatch start,
            final Callable<String> action
    ) {
        return () -> {
            ready.countDown();
            start.await();
            return action.call();
        };
    }

    private String saveResult(
            final long memberId,
            final long visitId,
            final long itemId,
            final String status,
            final long expectedVersion
    ) {
        try {
            visitCommandService.updateItemStatus(
                    memberId,
                    visitId,
                    itemId,
                    new UpdateVisitItemStatusCommand(status, expectedVersion)
            );
            return "SUCCESS";
        } catch (JachwiException exception) {
            return exception.getErrorCode().name();
        }
    }

    private String memoSaveResult(
            final long memberId,
            final long visitId,
            final long itemId,
            final String memo,
            final long expectedMemoVersion
    ) {
        try {
            visitCommandService.updateItemMemo(
                    memberId,
                    visitId,
                    itemId,
                    new UpdateVisitItemMemoCommand(new InlineMemo(memo), expectedMemoVersion)
            );
            return "SUCCESS";
        } catch (JachwiException exception) {
            return exception.getErrorCode().name();
        }
    }

    private Fixture fixture(final String subject) {
        jdbcTemplate.update(
                """
                INSERT INTO members (
                    oauth_provider, oauth_subject, email, display_name, last_login_at
                ) VALUES ('GOOGLE', ?, ?, '회원', CURRENT_TIMESTAMP(6))
                """,
                subject,
                subject + "@example.com"
        );
        final long memberId = jdbcTemplate.queryForObject(
                "SELECT id FROM members WHERE oauth_subject = ?",
                Long.class,
                subject
        );
        final long propertyId = propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand("동시성 매물", 0, 0, "직접 발견")
        ).propertyId();
        final long checklistId = checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand("현장", CheckStage.ON_SITE, List.of(101L, 102L))
        ).checklistId();
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, checklistId);
        return new Fixture(memberId, propertyId);
    }

    private record Fixture(long memberId, long propertyId) {
    }
}
