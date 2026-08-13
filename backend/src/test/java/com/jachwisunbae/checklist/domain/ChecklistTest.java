package com.jachwisunbae.checklist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChecklistTest {

    private static final Instant NOW = Instant.parse("2026-08-10T00:00:00Z");

    @DisplayName("이름은 공백을 제거하고 유니코드 코드포인트 50자까지 허용한다")
    @Test
    void normalizeName() {
        final ChecklistName name = new ChecklistName("  " + "🏠".repeat(50) + "  ");

        assertThat(name.value()).isEqualTo("🏠".repeat(50));
        assertThatThrownBy(() -> new ChecklistName("🏠".repeat(51)))
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @DisplayName("체크리스트는 하나 이상의 같은 단계 항목을 연속 순서로 가진다")
    @Test
    void validateItems() {
        assertError(List.of(), ErrorCode.CHECKLIST_EMPTY);
        assertError(
                List.of(
                        new ChecklistItem(101, CheckStage.ON_SITE, 1),
                        new ChecklistItem(101, CheckStage.ON_SITE, 2)
                ),
                ErrorCode.CHECKLIST_ITEM_DUPLICATED
        );
        assertError(
                List.of(new ChecklistItem(201, CheckStage.ONLINE_PHONE, 1)),
                ErrorCode.CHECKLIST_ITEM_STAGE_MISMATCH
        );
        assertThatThrownBy(() -> checklist(List.of(
                new ChecklistItem(101, CheckStage.ON_SITE, 2)
        ))).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("유효한 항목은 전달 순서와 생성 후 불변 단계를 보존한다")
    @Test
    void preserveOrderAndStage() {
        final Checklist checklist = checklist(List.of(
                new ChecklistItem(103, CheckStage.ON_SITE, 1),
                new ChecklistItem(101, CheckStage.ON_SITE, 2)
        ));

        assertThat(checklist.stage()).isEqualTo(CheckStage.ON_SITE);
        assertThat(checklist.items()).extracting(ChecklistItem::sourceCheckItemId)
                .containsExactly(103L, 101L);
    }

    @DisplayName("PROVIDED와 CUSTOM은 출처와 질문을 배타적으로 가지며 CUSTOM은 200 코드포인트까지 허용한다")
    @Test
    void validateProvidedAndCustomItems() {
        final ChecklistItem provided = ChecklistItem.provided(10, 101, CheckStage.ON_SITE, 1);
        final ChecklistItem custom = ChecklistItem.custom(11, "  " + "🏠".repeat(200) + "  ", CheckStage.ON_SITE, 2);

        assertThat(provided.origin()).isEqualTo(ChecklistItemOrigin.PROVIDED);
        assertThat(provided.sourceCheckItemId()).isEqualTo(101L);
        assertThat(provided.customQuestion()).isNull();
        assertThat(custom.origin()).isEqualTo(ChecklistItemOrigin.CUSTOM);
        assertThat(custom.sourceCheckItemId()).isNull();
        assertThat(custom.customQuestion()).isEqualTo("🏠".repeat(200));

        assertThatThrownBy(() -> ChecklistItem.custom(0, null, CheckStage.ON_SITE, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChecklistItem.custom(0, "   ", CheckStage.ON_SITE, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChecklistItem.custom(0, "🏠".repeat(201), CheckStage.ON_SITE, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChecklistItem(
                0,
                ChecklistItemOrigin.PROVIDED,
                null,
                "출처가 뒤섞였는가?",
                CheckStage.ON_SITE,
                1
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("CUSTOM 문구 중복은 허용하지만 저장된 로컬 ID 중복은 거부한다")
    @Test
    void allowDuplicateCustomQuestionsAndRejectDuplicateLocalIds() {
        final Checklist checklist = checklist(List.of(
                ChecklistItem.custom(10, "창문은 잘 닫히는가?", CheckStage.ON_SITE, 1),
                ChecklistItem.custom(11, "창문은 잘 닫히는가?", CheckStage.ON_SITE, 2)
        ));

        assertThat(checklist.items()).extracting(ChecklistItem::customQuestion)
                .containsExactly("창문은 잘 닫히는가?", "창문은 잘 닫히는가?");
        assertError(List.of(
                ChecklistItem.custom(10, "첫 질문인가?", CheckStage.ON_SITE, 1),
                ChecklistItem.custom(10, "둘째 질문인가?", CheckStage.ON_SITE, 2)
        ), ErrorCode.CHECKLIST_ITEM_DUPLICATED);
    }

    private Checklist checklist(final List<ChecklistItem> items) {
        return new Checklist(
                0,
                1,
                new ChecklistName("체크리스트"),
                CheckStage.ON_SITE,
                items,
                NOW,
                NOW
        );
    }

    private void assertError(final List<ChecklistItem> items, final ErrorCode errorCode) {
        assertThatThrownBy(() -> checklist(items))
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
