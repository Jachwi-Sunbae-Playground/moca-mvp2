package com.jachwisunbae.visit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.checklist.domain.ChecklistItemOrigin;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitDomainTest {

    @DisplayName("방문 완료는 최초 완료 시각을 기록하고 재완료에서도 그대로 유지한다")
    @Test
    void preserveFirstCompletionTime() {
        final Instant startedAt = Instant.parse("2026-08-10T01:00:00Z");
        final Instant completedAt = Instant.parse("2026-08-10T02:00:00Z");
        final Visit visit = Visit.start(1, 2, startedAt).withId(3);

        final Visit completed = visit.complete(completedAt);
        final Visit repeated = completed.complete(Instant.parse("2026-08-10T03:00:00Z"));

        assertThat(completed.status()).isEqualTo(VisitStatus.COMPLETED);
        assertThat(repeated.completedAt()).isEqualTo(completedAt);
        assertThat(repeated.updatedAt()).isEqualTo(completedAt);
    }

    @DisplayName("방문 집계는 확인 상태 합계와 checked 계산이 일치해야 한다")
    @Test
    void validateSummaryInvariant() {
        assertThat(VisitSummary.from(5, 2, 1, 2))
                .isEqualTo(new VisitSummary(5, 3, 2, 1, 2));
        assertThatThrownBy(() -> new VisitSummary(5, 4, 2, 1, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("확인 상태와 완료 명령은 계약된 값 외에는 전용 오류로 거부한다")
    @Test
    void validateRequestedStatuses() {
        assertThat(CheckStatus.from("GOOD")).isEqualTo(CheckStatus.GOOD);
        assertThat(VisitStatus.completionFrom("COMPLETED")).isEqualTo(VisitStatus.COMPLETED);

        assertError(() -> CheckStatus.from("BAD"), ErrorCode.INVALID_CHECK_STATUS);
        assertError(() -> VisitStatus.completionFrom("IN_PROGRESS"), ErrorCode.INVALID_VISIT_STATUS);
    }

    @DisplayName("인라인 메모는 빈 값과 200 코드포인트와 공백을 보존하고 null·초과·개행은 거부한다")
    @Test
    void validateInlineMemo() {
        final String boundary = "🏠".repeat(200);

        assertThat(new InlineMemo("").value()).isEmpty();
        assertThat(new InlineMemo("  메모  ").value()).isEqualTo("  메모  ");
        assertThat(new InlineMemo(boundary).value()).isEqualTo(boundary);
        assertError(() -> new InlineMemo(null), ErrorCode.VISIT_ITEM_MEMO_INVALID);
        assertError(() -> new InlineMemo("🏠".repeat(201)), ErrorCode.VISIT_ITEM_MEMO_INVALID);
        assertError(() -> new InlineMemo("첫 줄\n둘째 줄"), ErrorCode.VISIT_ITEM_MEMO_INVALID);
        assertError(() -> new InlineMemo("첫 줄\r둘째 줄"), ErrorCode.VISIT_ITEM_MEMO_INVALID);
    }

    @DisplayName("상태와 메모 변경은 각 값·버전·저장 시각만 변경한다")
    @Test
    void changeStatusAndMemoIndependently() {
        final Instant initial = Instant.parse("2026-08-12T01:00:00Z");
        final VisitCheckItem item = new VisitCheckItem(
                1,
                ChecklistItemOrigin.PROVIDED,
                2L,
                3L,
                "질문",
                "안내",
                1,
                CheckStatus.UNCONFIRMED,
                0,
                initial,
                new InlineMemo("기존"),
                0,
                null
        );
        final Instant statusSavedAt = initial.plusSeconds(10);
        final VisitCheckItem statusChanged = item.changeStatus(CheckStatus.GOOD, 0, statusSavedAt);

        assertThat(statusChanged.status()).isEqualTo(CheckStatus.GOOD);
        assertThat(statusChanged.statusVersion()).isOne();
        assertThat(statusChanged.statusSavedAt()).isEqualTo(statusSavedAt);
        assertThat(statusChanged.inlineMemo()).isEqualTo(item.inlineMemo());
        assertThat(statusChanged.memoVersion()).isZero();
        assertThat(statusChanged.memoSavedAt()).isNull();

        final Instant memoSavedAt = initial.plusSeconds(20);
        final VisitCheckItem memoChanged = statusChanged.changeMemo(new InlineMemo("변경"), 0, memoSavedAt);

        assertThat(memoChanged.inlineMemo().value()).isEqualTo("변경");
        assertThat(memoChanged.memoVersion()).isOne();
        assertThat(memoChanged.memoSavedAt()).isEqualTo(memoSavedAt);
        assertThat(memoChanged.status()).isEqualTo(statusChanged.status());
        assertThat(memoChanged.statusVersion()).isEqualTo(statusChanged.statusVersion());
        assertThat(memoChanged.statusSavedAt()).isEqualTo(statusChanged.statusSavedAt());
        assertError(
                () -> memoChanged.changeStatus(CheckStatus.CAUTION, 0, memoSavedAt),
                ErrorCode.VISIT_ITEM_STATUS_VERSION_CONFLICT
        );
        assertError(
                () -> memoChanged.changeMemo(new InlineMemo("충돌"), 0, memoSavedAt),
                ErrorCode.VISIT_ITEM_MEMO_VERSION_CONFLICT
        );
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
