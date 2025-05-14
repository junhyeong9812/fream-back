package com.fream.back.domain.inquiry.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 1대1 문의 관련 에러 코드 Enum
 */
@Getter
@RequiredArgsConstructor
public enum InquiryErrorCode implements ErrorCode {

    // 400 BAD_REQUEST
    INQUIRY_INVALID_INPUT("INQUIRY-400-1", "1대1 문의 입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    INQUIRY_FILE_EMPTY("INQUIRY-400-2", "업로드된 파일이 비어있습니다.", HttpStatus.BAD_REQUEST.value()),
    INQUIRY_FILE_TOO_LARGE("INQUIRY-400-3", "파일 크기가 제한을 초과했습니다.", HttpStatus.BAD_REQUEST.value()),
    INQUIRY_FILE_UNSUPPORTED_TYPE("INQUIRY-400-4", "지원되지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST.value()),
    INQUIRY_STATUS_INVALID("INQUIRY-400-5", "유효하지 않은 문의 상태입니다.", HttpStatus.BAD_REQUEST.value()),
    INQUIRY_ALREADY_ANSWERED("INQUIRY-400-6", "이미 답변이 완료된 문의입니다.", HttpStatus.BAD_REQUEST.value()),

    // 401 UNAUTHORIZED
    INQUIRY_UNAUTHORIZED("INQUIRY-401-1", "1대1 문의에 접근할 권한이 없습니다.", HttpStatus.UNAUTHORIZED.value()),

    // 403 FORBIDDEN
    INQUIRY_ACCESS_DENIED("INQUIRY-403-1", "이 문의에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    INQUIRY_PRIVATE("INQUIRY-403-2", "비공개 문의는 작성자와 관리자만 조회할 수 있습니다.", HttpStatus.FORBIDDEN.value()),

    // 404 NOT_FOUND
    INQUIRY_NOT_FOUND("INQUIRY-404-1", "해당 1대1 문의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    INQUIRY_IMAGE_NOT_FOUND("INQUIRY-404-2", "해당 이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),

    // 500 INTERNAL_SERVER_ERROR
    INQUIRY_SAVE_ERROR("INQUIRY-500-1", "1대1 문의 저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INQUIRY_UPDATE_ERROR("INQUIRY-500-2", "1대1 문의 수정 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INQUIRY_DELETE_ERROR("INQUIRY-500-3", "1대1 문의 삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INQUIRY_FILE_SAVE_ERROR("INQUIRY-500-4", "파일 저장 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INQUIRY_FILE_DELETE_ERROR("INQUIRY-500-5", "파일 삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int status;
}