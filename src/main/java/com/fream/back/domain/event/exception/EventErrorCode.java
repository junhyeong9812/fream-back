package com.fream.back.domain.event.exception;

import com.fream.back.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum EventErrorCode implements ErrorCode {
    // 이벤트 조회 관련 오류
    EVENT_NOT_FOUND("EVENT-001", "이벤트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),

    // 이벤트 생성 관련 오류
    INVALID_EVENT_DATE("EVENT-002", "이벤트 날짜가 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_EVENT_DATA("EVENT-003", "이벤트 데이터가 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    EVENT_TITLE_DUPLICATE("EVENT-004", "이미 존재하는 이벤트 제목입니다.", HttpStatus.CONFLICT.value()),

    // 이벤트 수정/삭제 관련 오류
    EVENT_MODIFICATION_DENIED("EVENT-005", "이벤트 수정 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    EVENT_DELETION_DENIED("EVENT-006", "이벤트 삭제 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),

    // 이미지 관련 오류
    IMAGE_NOT_FOUND("EVENT-007", "이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    IMAGE_UPLOAD_FAILED("EVENT-008", "이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INVALID_IMAGE_FORMAT("EVENT-009", "이미지 형식이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value()),

    // 브랜드 관련 오류
    BRAND_NOT_FOUND("EVENT-010", "브랜드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value());

    private final String code;
    private final String message;
    private final int status;

    EventErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}