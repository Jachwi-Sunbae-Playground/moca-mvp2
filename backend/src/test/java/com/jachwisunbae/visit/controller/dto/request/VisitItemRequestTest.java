package com.jachwisunbae.visit.controller.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VisitItemRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("v1.1 상태 버전과 legacy 상태 버전은 각각 정규화하고 같은 값이면 함께 허용한다")
    @Test
    void normalizeStatusVersions() throws Exception {
        final UpdateVisitItemRequest v11 = request("{\"status\":\"GOOD\",\"expectedStatusVersion\":1}");
        final UpdateVisitItemRequest legacy = request("{\"status\":\"GOOD\",\"expectedVersion\":2}");
        final UpdateVisitItemRequest both = request(
                "{\"status\":\"GOOD\",\"expectedStatusVersion\":3,\"expectedVersion\":3}"
        );

        assertThat(v11.toCommand().expectedStatusVersion()).isOne();
        assertThat(legacy.toCommand().expectedStatusVersion()).isEqualTo(2);
        assertThat(both.toCommand().expectedStatusVersion()).isEqualTo(3);
    }

    @DisplayName("상태 버전 표현 충돌은 null을 포함해 필드 존재 여부로 거부한다")
    @Test
    void rejectAmbiguousStatusVersions() throws Exception {
        assertError(
                request("{\"status\":\"GOOD\",\"expectedStatusVersion\":1,\"expectedVersion\":2}")::toCommand,
                ErrorCode.AMBIGUOUS_STATUS_VERSION
        );
        assertError(
                request("{\"status\":\"GOOD\",\"expectedStatusVersion\":null,\"expectedVersion\":0}")::toCommand,
                ErrorCode.AMBIGUOUS_STATUS_VERSION
        );
    }

    @DisplayName("상태 버전 누락·null·음수와 메모 버전 null·음수는 일반 요청 오류로 거부한다")
    @Test
    void rejectInvalidExpectedVersions() throws Exception {
        assertError(request("{\"status\":\"GOOD\"}")::toCommand, ErrorCode.INVALID_REQUEST);
        assertError(
                request("{\"status\":\"GOOD\",\"expectedStatusVersion\":null}")::toCommand,
                ErrorCode.INVALID_REQUEST
        );
        assertError(
                request("{\"status\":\"GOOD\",\"expectedVersion\":-1}")::toCommand,
                ErrorCode.INVALID_REQUEST
        );
        assertError(() -> new UpdateVisitItemMemoRequest("메모", null).toCommand(), ErrorCode.INVALID_REQUEST);
        assertError(() -> new UpdateVisitItemMemoRequest("메모", -1L).toCommand(), ErrorCode.INVALID_REQUEST);
    }

    private UpdateVisitItemRequest request(final String json) throws Exception {
        return objectMapper.readValue(json, UpdateVisitItemRequest.class);
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
