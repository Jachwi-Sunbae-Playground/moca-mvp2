package com.jachwisunbae.property.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.checklist.domain.CheckStage;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActiveChecklistTest {

    @DisplayName("활성 체크리스트는 양수 식별자와 단계·시각을 요구한다")
    @Test
    void requireIdentityAndStage() {
        final Instant now = Instant.parse("2026-08-11T00:00:00Z");

        assertThatThrownBy(() -> ActiveChecklist.create(0, 1, CheckStage.ON_SITE, 1, now))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ActiveChecklist.create(1, 1, null, 1, now))
                .isInstanceOf(NullPointerException.class);
    }

    @DisplayName("활성 체크리스트는 같은 체크리스트 재지정을 판별한다")
    @Test
    void identifySameChecklist() {
        final ActiveChecklist activeChecklist = ActiveChecklist.create(
                1,
                2,
                CheckStage.ON_SITE,
                3,
                Instant.parse("2026-08-11T00:00:00Z")
        );

        assertThat(activeChecklist.uses(3)).isTrue();
        assertThat(activeChecklist.uses(4)).isFalse();
    }
}
