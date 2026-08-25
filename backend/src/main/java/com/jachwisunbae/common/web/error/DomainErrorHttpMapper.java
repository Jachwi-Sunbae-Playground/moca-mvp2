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
                    MEMBER_EMAIL_INVALID,
                    MEMBER_NAME_INVALID,
                    NICKNAME_INVALID,
                    NICKNAME_PASSWORD_INVALID,
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
                    PROPERTY_LOCATION_INVALID,
                    PROPERTY_MEMO_INVALID,
                    PROPERTY_CHECKLIST_STAGE_MISMATCH,
                    PROPERTY_CHECK_RESULT_INVALID,
                    PHOTO_LIMIT_EXCEEDED,
                    PHOTO_CONTENT_TYPE_UNSUPPORTED,
                    PHOTO_SIZE_EXCEEDED,
                    MAP_QUERY_INVALID -> HttpStatus.BAD_REQUEST;
            case MEMBER_NOT_FOUND,
                    CHECKLIST_NOT_FOUND,
                    CHECKLIST_ITEM_NOT_FOUND,
                    PROPERTY_NOT_FOUND,
                    PHOTO_NOT_FOUND,
                    PROPERTY_CHECKLIST_NOT_FOUND,
                    PROPERTY_CHECKLIST_ITEM_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ACCESS_TOKEN_INVALID,
                    NICKNAME_AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            case PROPERTY_LIMIT_EXCEEDED,
                    NICKNAME_PASSWORD_UNEXPECTED -> HttpStatus.CONFLICT;
            case NICKNAME_AUTH_RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case PHOTO_STORAGE_FAILURE,
                    PROPERTY_COMPARISON_EXPORT_FAILED,
                    MAP_PROVIDER_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
    }
}
