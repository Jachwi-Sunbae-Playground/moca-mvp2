package com.jachwisunbae.common.web.error;

import com.jachwisunbae.common.exception.DomainErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DomainErrorHttpMapper {

    public HttpStatus statusOf(DomainErrorCode code) {
        return switch (code) {
            case SYSTEM_CHECK_ITEM_STAGE_REQUIRED,
                    SYSTEM_CHECK_ITEM_TYPE_REQUIRED,
                    SYSTEM_CHECK_ITEM_QUESTION_INVALID,
                    MEMBER_SUBJECT_INVALID,
                    MEMBER_EMAIL_INVALID,
                    MEMBER_NAME_INVALID,
                    USER_CHECKLIST_MEMBER_REQUIRED,
                    USER_CHECKLIST_NAME_INVALID,
                    USER_CHECKLIST_STAGE_REQUIRED,
                    USER_CHECKLIST_DELETED_AT_REQUIRED,
                    CHECKLIST_ITEMS_INVALID,
                    CHECKLIST_ITEM_STAGE_MISMATCH,
                    CHECKLIST_CORE_ITEM_REQUIRED,
                    CHECKLIST_INACTIVE_ITEM_NOT_ALLOWED,
                    DUPLICATE_CHECK_ITEM,
                    CHECKLIST_ITEM_COUNT_OUT_OF_RANGE,
                    INVALID_SYSTEM_CHECK_ITEM,
                    PROPERTY_INPUT_INVALID,
                    PROPERTY_MEMO_INVALID,
                    PROPERTY_CHECKLIST_STAGE_MISMATCH,
                    PROPERTY_CHECK_RESULT_INVALID -> HttpStatus.BAD_REQUEST;
            case MEMBER_NOT_FOUND,
                    CHECKLIST_NOT_FOUND,
                    CHECKLIST_ITEM_NOT_FOUND,
                    PROPERTY_NOT_FOUND,
                    PHOTO_NOT_FOUND,
                    PROPERTY_CHECKLIST_NOT_FOUND,
                    PROPERTY_CHECKLIST_ITEM_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACCESS_TOKEN_INVALID,
                    REFRESH_TOKEN_INVALID,
                    REFRESH_TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case REFRESH_TOKEN_REUSED,
                    PROPERTY_LIMIT_EXCEEDED -> HttpStatus.CONFLICT;
            case GOOGLE_AUTHENTICATION_FAILED,
                    GOOGLE_IDENTITY_INVALID,
                    OAUTH_PROVIDER_UNSUPPORTED -> HttpStatus.BAD_REQUEST;
        };
    }
}
