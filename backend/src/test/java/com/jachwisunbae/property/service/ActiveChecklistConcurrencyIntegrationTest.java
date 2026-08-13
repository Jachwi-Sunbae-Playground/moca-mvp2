package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.checklist.domain.CheckStage;
import com.jachwisunbae.checklist.service.ChecklistCommandService;
import com.jachwisunbae.checklist.service.dto.command.CreateChecklistCommand;
import com.jachwisunbae.checklist.service.dto.command.ReplaceChecklistCommand;
import com.jachwisunbae.common.IntegrationTest;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.property.service.dto.command.CreatePropertyCommand;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ActiveChecklistConcurrencyIntegrationTest extends IntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    private long memberId;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM checklists");
        jdbcTemplate.update("DELETE FROM properties");
        jdbcTemplate.update("DELETE FROM members");
        memberId = saveMember("active-concurrency-owner");
    }

    @DisplayName("같은 매물·단계의 동시 지정은 모두 성공하고 마지막 직렬화 요청 하나만 남는다")
    @Test
    void serializeConcurrentAssignments() throws Exception {
        final long propertyId = createProperty("동시 지정 매물");
        final long firstChecklistId = createChecklist("첫 현장", 101L);
        final long secondChecklistId = createChecklist("둘째 현장", 102L);

        final List<Throwable> failures = runConcurrently(
                () -> activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.ON_SITE,
                        firstChecklistId
                ),
                () -> activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.ON_SITE,
                        secondChecklistId
                )
        );

        assertThat(failures).containsOnlyNulls();
        assertThat(activeCount(propertyId)).isOne();
        assertThat(activeChecklistId(propertyId)).isIn(firstChecklistId, secondChecklistId);
    }

    @DisplayName("동시 지정과 연결 해제는 매물 잠금으로 직렬화되어 연결이 없거나 지정 값 하나만 남는다")
    @Test
    void serializeAssignmentAndUnassignment() throws Exception {
        final long propertyId = createProperty("지정 해제 매물");
        final long firstChecklistId = createChecklist("기존 현장", 101L);
        final long secondChecklistId = createChecklist("새 현장", 102L);
        activeChecklistService.assign(memberId, propertyId, CheckStage.ON_SITE, firstChecklistId);

        final List<Throwable> failures = runConcurrently(
                () -> activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.ON_SITE,
                        secondChecklistId
                ),
                () -> activeChecklistService.unassign(memberId, propertyId, CheckStage.ON_SITE)
        );

        assertThat(failures).containsOnlyNulls();
        assertThat(activeCount(propertyId)).isBetween(0L, 1L);
        if (activeCount(propertyId) == 1L) {
            assertThat(activeChecklistId(propertyId)).isEqualTo(secondChecklistId);
        }
    }

    @DisplayName("동시 지정과 매물 삭제는 고아 연결이나 내부 DB 오류 없이 매물 삭제로 수렴한다")
    @Test
    void raceAssignmentAndPropertyDeletion() throws Exception {
        final long propertyId = createProperty("삭제 경합 매물");
        final long checklistId = createChecklist("삭제 경합 현장", 101L);

        final List<Throwable> failures = runConcurrently(
                () -> activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.ON_SITE,
                        checklistId
                ),
                () -> propertyDeletionService.deleteProperty(memberId, propertyId)
        );

        assertOnlyAllowed(failures, ErrorCode.PROPERTY_NOT_FOUND);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM properties WHERE id = ?",
                Long.class,
                propertyId
        )).isZero();
        assertThat(activeCount(propertyId)).isZero();
    }

    @DisplayName("동시 지정과 체크리스트 삭제는 삭제된 체크리스트와 활성 연결을 남기지 않는다")
    @Test
    void raceAssignmentAndChecklistDeletion() throws Exception {
        final long propertyId = createProperty("체크리스트 삭제 경합 매물");
        final long checklistId = createChecklist("삭제 경합 현장", 101L);

        final List<Throwable> failures = runConcurrently(
                () -> activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.ON_SITE,
                        checklistId
                ),
                () -> checklistCommandService.deleteChecklist(memberId, checklistId)
        );

        assertOnlyAllowed(failures, ErrorCode.CHECKLIST_NOT_FOUND);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM checklists WHERE id = ?",
                Long.class,
                checklistId
        )).isZero();
        assertThat(activeCount(propertyId)).isZero();
    }

    @DisplayName("동시 지정과 체크리스트 수정은 루트 잠금으로 직렬화되고 매물 상세가 최종 원본을 참조한다")
    @Test
    void raceAssignmentAndChecklistReplacement() throws Exception {
        final long propertyId = createProperty("체크리스트 수정 경합 매물");
        final long checklistId = createChecklist("변경 전", 101L);

        final List<Throwable> failures = runConcurrently(
                () -> activeChecklistService.assign(
                        memberId,
                        propertyId,
                        CheckStage.ON_SITE,
                        checklistId
                ),
                () -> checklistCommandService.replaceChecklist(
                        memberId,
                        checklistId,
                        new ReplaceChecklistCommand("변경 후", List.of(102L, 103L))
                )
        );

        assertThat(failures).containsOnlyNulls();
        assertThat(propertyQueryService.getProperty(memberId, propertyId).activeChecklists())
                .singleElement()
                .satisfies(result -> {
                    assertThat(result.name()).isEqualTo("변경 후");
                    assertThat(result.itemCount()).isEqualTo(2);
                });
    }

    private List<Throwable> runConcurrently(final Runnable first, final Runnable second) throws Exception {
        final CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            final Future<Throwable> firstFuture = executor.submit(() -> runAfter(start, first));
            final Future<Throwable> secondFuture = executor.submit(() -> runAfter(start, second));
            start.countDown();
            return Arrays.asList(firstFuture.get(), secondFuture.get());
        }
    }

    private Throwable runAfter(final CountDownLatch start, final Runnable action) {
        try {
            start.await();
            action.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void assertOnlyAllowed(final List<Throwable> failures, final ErrorCode allowedErrorCode) {
        assertThat(failures).allSatisfy(failure -> {
            if (failure != null) {
                assertThat(failure).isInstanceOf(JachwiException.class);
                assertThat(((JachwiException) failure).getErrorCode()).isEqualTo(allowedErrorCode);
            }
        });
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

    private long createProperty(final String name) {
        return propertyCommandService.createProperty(
                memberId,
                new CreatePropertyCommand(name, 0, 0, "직접 발견")
        ).propertyId();
    }

    private long createChecklist(final String name, final long checkItemId) {
        return checklistCommandService.createChecklist(
                memberId,
                new CreateChecklistCommand(name, CheckStage.ON_SITE, List.of(checkItemId))
        ).checklistId();
    }

    private long activeCount(final long propertyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM property_active_checklists WHERE property_id = ?",
                Long.class,
                propertyId
        );
    }

    private long activeChecklistId(final long propertyId) {
        return jdbcTemplate.queryForObject(
                "SELECT checklist_id FROM property_active_checklists WHERE property_id = ? AND stage = 'ON_SITE'",
                Long.class,
                propertyId
        );
    }
}
