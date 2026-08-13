package com.jachwisunbae.property.controller.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jachwisunbae.common.exception.client.InvalidCommandException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import com.jachwisunbae.property.service.dto.command.SavePropertyMemoCommand;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SavePropertyMemoRequestTest {

    @DisplayName("여덟 구조화 필드가 모두 있으면 전체 교체 명령으로 변환한다")
    @Test
    void convertStructuredRequest() {
        final SavePropertyMemoRequest request = structuredRequest();

        final SavePropertyMemoCommand command = request.toCommand();

        assertThat(command.isLegacy()).isFalse();
        assertThat(command.viewingSchedule()).isEqualTo("방문 일정");
        assertThat(command.additionalMemo()).isEqualTo("추가 메모");
    }

    @DisplayName("content 단독 요청은 legacy 추가 메모 명령으로 변환한다")
    @Test
    void convertLegacyRequest() {
        final SavePropertyMemoRequest request = new SavePropertyMemoRequest();
        request.setContent("기존 메모");

        final SavePropertyMemoCommand command = request.toCommand();

        assertThat(command.isLegacy()).isTrue();
        assertThat(command.additionalMemo()).isEqualTo("기존 메모");
    }

    @DisplayName("content와 구조화 필드가 함께 존재하면 값이 null이어도 모호한 표현으로 거부한다")
    @Test
    void rejectAmbiguousRepresentation() {
        final SavePropertyMemoRequest request = new SavePropertyMemoRequest();
        request.setContent("기존 메모");
        request.setViewingSchedule(null);

        assertErrorCode(request, ErrorCode.AMBIGUOUS_MEMO_CONTENT);
    }

    @DisplayName("구조화 필드가 일부 누락되거나 null이면 메모 요청 오류로 거부한다")
    @Test
    void rejectPartialOrNullStructuredRequest() {
        final SavePropertyMemoRequest partialRequest = new SavePropertyMemoRequest();
        partialRequest.setViewingSchedule("방문 일정");
        assertErrorCode(partialRequest, ErrorCode.PROPERTY_MEMO_INVALID);

        final SavePropertyMemoRequest nullRequest = structuredRequest();
        nullRequest.setGovernmentSupport(null);
        assertErrorCode(nullRequest, ErrorCode.PROPERTY_MEMO_INVALID);
    }

    @DisplayName("일곱 구조화 필드 각각과 추가 메모는 유니코드 코드포인트 상한을 적용한다")
    @Test
    void validateCodePointLength() {
        final String longField = "📝".repeat(201);
        final List<Consumer<SavePropertyMemoRequest>> invalidFieldSetters = List.of(
                request -> request.setViewingSchedule(longField),
                request -> request.setMoveInAvailability(longField),
                request -> request.setProvisionalDeposit(longField),
                request -> request.setRoomOptions(longField),
                request -> request.setMaintenanceAndUtilities(longField),
                request -> request.setCommuteTime(longField),
                request -> request.setGovernmentSupport(longField)
        );
        invalidFieldSetters.forEach(setter -> {
            final SavePropertyMemoRequest fieldRequest = structuredRequest();
            setter.accept(fieldRequest);
            assertErrorCode(fieldRequest, ErrorCode.PROPERTY_MEMO_INVALID);
        });

        final SavePropertyMemoRequest memoRequest = structuredRequest();
        memoRequest.setAdditionalMemo("📝".repeat(5_001));
        assertErrorCode(memoRequest, ErrorCode.PROPERTY_MEMO_INVALID);
    }

    private SavePropertyMemoRequest structuredRequest() {
        final SavePropertyMemoRequest request = new SavePropertyMemoRequest();
        request.setViewingSchedule("방문 일정");
        request.setMoveInAvailability("입주 가능일");
        request.setProvisionalDeposit("가계약금");
        request.setRoomOptions("방 옵션");
        request.setMaintenanceAndUtilities("관리비와 공과금");
        request.setCommuteTime("통학 시간");
        request.setGovernmentSupport("정부 지원");
        request.setAdditionalMemo("추가 메모");
        return request;
    }

    private void assertErrorCode(final SavePropertyMemoRequest request, final ErrorCode errorCode) {
        assertThatThrownBy(request::toCommand)
                .isInstanceOf(InvalidCommandException.class)
                .extracting(exception -> ((InvalidCommandException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
