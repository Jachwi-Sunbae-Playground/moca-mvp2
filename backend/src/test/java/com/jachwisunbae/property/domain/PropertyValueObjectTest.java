package com.jachwisunbae.property.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PropertyValueObjectTest {

    @DisplayName("매물 이름은 앞뒤 공백을 제거해 보존한다")
    @Test
    void trimPropertyName() {
        final PropertyName name = new PropertyName("  신림역 원룸  ");

        assertThat(name.value()).isEqualTo("신림역 원룸");
    }

    @DisplayName("공백뿐인 매물 이름은 거부한다")
    @Test
    void rejectBlankPropertyName() {
        assertThatThrownBy(() -> new PropertyName("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("금액은 0과 JavaScript 안전 정수 최댓값을 허용한다")
    @Test
    void allowMoneyBoundaries() {
        assertThat(new Money(0).amount()).isZero();
        assertThat(new Money(Money.MAX_AMOUNT).amount()).isEqualTo(Money.MAX_AMOUNT);
    }

    @DisplayName("음수와 JavaScript 안전 정수 범위를 넘은 금액은 거부한다")
    @Test
    void rejectOutOfRangeMoney() {
        assertThatThrownBy(() -> new Money(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(Money.MAX_AMOUNT + 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("host가 있는 HTTP와 HTTPS 발견 경로를 URL로 분류한다")
    @Test
    void classifyUrlDiscoverySource() {
        assertThat(DiscoverySource.from(" https://example.com/listings/10 "))
                .isEqualTo(new DiscoverySource(DiscoverySourceType.URL, "https://example.com/listings/10"));
        assertThat(DiscoverySource.from("HTTP://example.com/home").type()).isEqualTo(DiscoverySourceType.URL);
    }

    @DisplayName("URL이 아닌 유효 문자열은 변경하지 않고 일반 텍스트로 보존한다")
    @Test
    void classifyTextDiscoverySource() {
        final DiscoverySource source = DiscoverySource.from("  직방 앱에서 발견 #10  ");

        assertThat(source.type()).isEqualTo(DiscoverySourceType.TEXT);
        assertThat(source.value()).isEqualTo("직방 앱에서 발견 #10");
    }

    @DisplayName("빈 문자열은 메모 지우기 값으로 허용한다")
    @Test
    void allowEmptyMemo() {
        assertThat(PropertyMemo.empty().content()).isEmpty();
    }

    @DisplayName("5,000자를 넘는 메모는 거부한다")
    @Test
    void rejectTooLongMemo() {
        assertThatThrownBy(() -> new PropertyMemo("메".repeat(5_001)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("방문 전 사전 메모 필드는 빈 값과 200 유니코드 코드포인트를 허용한다")
    @Test
    void allowPreVisitMemoFieldBoundaries() {
        assertThat(PreVisitMemoField.empty().value()).isEmpty();
        assertThat(new PreVisitMemoField("📝".repeat(200)).value()).hasSize(400);
    }

    @DisplayName("방문 전 사전 메모 필드는 null과 200 코드포인트 초과를 거부한다")
    @Test
    void rejectInvalidPreVisitMemoField() {
        assertThatThrownBy(() -> new PreVisitMemoField(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreVisitMemoField("📝".repeat(201)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
