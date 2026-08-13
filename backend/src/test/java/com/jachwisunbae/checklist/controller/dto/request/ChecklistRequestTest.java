package com.jachwisunbae.checklist.controller.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jachwisunbae.checklist.service.dto.command.ChecklistRequestMode;
import com.jachwisunbae.common.exception.JachwiException;
import com.jachwisunbae.common.exception.errorcode.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChecklistRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("명시적 null도 표현 사용으로 추적해 items와 checkItemIds 혼합 요청을 거부한다")
    @Test
    void rejectMixedRepresentationsEvenWhenNull() throws Exception {
        final CreateChecklistRequest request = objectMapper.readValue(
                """
                {
                  "name": "혼합",
                  "stage": "ON_SITE",
                  "items": null,
                  "checkItemIds": [101]
                }
                """,
                CreateChecklistRequest.class
        );

        assertError(request::toCommand, ErrorCode.CHECKLIST_ITEMS_REPRESENTATION_CONFLICT);
    }

    @DisplayName("v1.1 CUSTOM은 trim과 내부 개행을 적용하고 200 코드포인트 경계를 검증한다")
    @Test
    void normalizeCustomQuestionByUnicodeCodePoints() throws Exception {
        final String boundary = "🏠".repeat(200);
        final String overLimit = "🏠".repeat(201);
        final CreateChecklistRequest request = objectMapper.readValue(
                """
                {
                  "name": "CUSTOM",
                  "stage": "ON_SITE",
                  "items": [{"origin": "CUSTOM", "question": "  질문  \\n추가  "}]
                }
                """,
                CreateChecklistRequest.class
        );

        final var command = request.toCommand();

        assertThat(command.mode()).isEqualTo(ChecklistRequestMode.V11);
        assertThat(command.items().getFirst().question()).isEqualTo("질문  \n추가");
        assertThat(new ChecklistItemRequest("CUSTOM", null, null, boundary).toCommand(false).question())
                .isEqualTo(boundary);
        assertThatThrownBy(() -> new ChecklistItemRequest("CUSTOM", null, null, overLimit).toCommand(false))
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CUSTOM_CHECKLIST_ITEM_INVALID);
    }

    private void assertError(final Runnable action, final ErrorCode errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(JachwiException.class)
                .extracting(exception -> ((JachwiException) exception).getErrorCode())
                .isEqualTo(errorCode);
    }
}
