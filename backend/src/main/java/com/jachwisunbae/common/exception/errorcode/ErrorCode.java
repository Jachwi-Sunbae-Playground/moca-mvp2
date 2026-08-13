package com.jachwisunbae.common.exception.errorcode;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지 요청값이 올바르지 않습니다."),
    GOOGLE_AUTHORIZATION_CODE_INVALID(HttpStatus.BAD_REQUEST, "Google 인증 요청이 올바르지 않습니다."),
    GOOGLE_IDENTITY_INVALID(HttpStatus.BAD_REQUEST, "Google 사용자 정보를 확인할 수 없습니다."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
    ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Access Token이 올바르지 않습니다."),
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 매물을 찾을 수 없습니다."),
    PROPERTY_MEMO_INVALID(HttpStatus.BAD_REQUEST, "매물 메모 요청이 올바르지 않습니다."),
    AMBIGUOUS_MEMO_CONTENT(HttpStatus.BAD_REQUEST, "메모 표현을 하나만 사용해야 합니다."),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사진을 찾을 수 없습니다."),
    PHOTO_FORMAT_UNSUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 사진 형식입니다."),
    PHOTO_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "사진 파일 크기 제한을 초과했습니다."),
    PHOTO_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "매물의 사진 개수 제한을 초과했습니다."),
    PHOTO_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사진 업로드에 실패했습니다."),
    PHOTO_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사진을 불러오지 못했습니다."),
    PHOTO_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "사진 삭제에 실패했습니다."),
    INVALID_STAGE(HttpStatus.BAD_REQUEST, "확인 단계 값이 올바르지 않습니다."),
    CHECK_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 체크 항목을 찾을 수 없습니다."),
    CHECK_ITEM_INACTIVE(HttpStatus.BAD_REQUEST, "비활성 체크 항목은 새로 선택할 수 없습니다."),
    CHECKLIST_PRESET_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 체크리스트 프리셋을 찾을 수 없습니다."),
    CHECKLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 체크리스트를 찾을 수 없습니다."),
    CHECKLIST_EMPTY(HttpStatus.BAD_REQUEST, "체크리스트에는 하나 이상의 항목이 필요합니다."),
    CHECKLIST_ITEM_DUPLICATED(HttpStatus.BAD_REQUEST, "체크리스트에 같은 항목을 중복할 수 없습니다."),
    CHECKLIST_ITEM_STAGE_MISMATCH(HttpStatus.BAD_REQUEST, "체크리스트와 체크 항목의 단계가 다릅니다."),
    CUSTOM_CHECKLIST_ITEM_INVALID(HttpStatus.BAD_REQUEST, "사용자 체크리스트 항목이 올바르지 않습니다."),
    CHECKLIST_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 체크리스트 항목을 찾을 수 없습니다."),
    CHECKLIST_ITEMS_REPRESENTATION_CONFLICT(HttpStatus.BAD_REQUEST, "체크리스트 항목 표현을 하나만 사용해야 합니다."),
    CHECKLIST_REQUIRES_V11_CLIENT(HttpStatus.CONFLICT, "사용자 항목을 변경하려면 최신 클라이언트가 필요합니다."),
    CHECKLIST_STAGE_MISMATCH(HttpStatus.BAD_REQUEST, "체크리스트의 확인 단계가 다릅니다."),
    VISIT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방문 기록을 찾을 수 없습니다."),
    VISIT_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방문 체크 항목을 찾을 수 없습니다."),
    ACTIVE_CHECKLIST_REQUIRED(HttpStatus.BAD_REQUEST, "방문을 시작하려면 활성 체크리스트가 필요합니다."),
    INVALID_CHECK_STATUS(HttpStatus.BAD_REQUEST, "확인 상태가 올바르지 않습니다."),
    INVALID_VISIT_STATUS(HttpStatus.BAD_REQUEST, "방문 상태가 올바르지 않습니다."),
    VISIT_ITEM_STATUS_VERSION_CONFLICT(HttpStatus.CONFLICT, "방문 체크 항목 상태가 이미 변경되었습니다."),
    VISIT_ITEM_MEMO_VERSION_CONFLICT(HttpStatus.CONFLICT, "방문 체크 항목 메모가 이미 변경되었습니다."),
    VISIT_ITEM_MEMO_INVALID(HttpStatus.BAD_REQUEST, "방문 체크 항목 메모 요청이 올바르지 않습니다."),
    AMBIGUOUS_STATUS_VERSION(HttpStatus.BAD_REQUEST, "상태 버전 표현의 값이 서로 다릅니다."),
    CHECKLIST_SNAPSHOT_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "체크리스트 스냅샷 생성에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    GOOGLE_AUTHENTICATION_FAILED(HttpStatus.BAD_GATEWAY, "Google 인증 서비스 요청에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(final HttpStatus status, final String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
