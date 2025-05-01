package com.fream.back.domain.style.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 스타일 도메인에서 사용되는 에러 코드 정의
 * ST: Style 도메인 에러 코드 접두사
 */
@Getter
@RequiredArgsConstructor
public enum StyleErrorCode implements ErrorCode {

    // 공통 에러
    STYLE_NOT_FOUND("ST001", "스타일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    STYLE_ACCESS_DENIED("ST002", "스타일에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    STYLE_INVALID_REQUEST("ST003", "잘못된 스타일 요청입니다.", HttpStatus.BAD_REQUEST.value()),

    // 미디어 관련 에러
    MEDIA_FILE_UPLOAD_FAILED("ST101", "미디어 파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    MEDIA_FILE_NOT_FOUND("ST102", "미디어 파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    MEDIA_FILE_INVALID("ST103", "유효하지 않은 미디어 파일입니다.", HttpStatus.BAD_REQUEST.value()),

    // 해시태그 관련 에러
    HASHTAG_NOT_FOUND("ST201", "해시태그를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    HASHTAG_ALREADY_EXISTS("ST202", "이미 존재하는 해시태그입니다.", HttpStatus.CONFLICT.value()),
    HASHTAG_IN_USE("ST203", "사용 중인 해시태그는 삭제할 수 없습니다.", HttpStatus.CONFLICT.value()),

    // 댓글 관련 에러
    COMMENT_NOT_FOUND("ST301", "댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    COMMENT_ACCESS_DENIED("ST302", "댓글에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN.value()),
    COMMENT_CONTENT_INVALID("ST303", "댓글 내용이 유효하지 않습니다.", HttpStatus.BAD_REQUEST.value()),

    // 좋아요/관심 관련 에러
    LIKE_OPERATION_FAILED("ST401", "좋아요 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    INTEREST_OPERATION_FAILED("ST402", "관심 등록 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),

    // 스타일 조회 관련 에러
    STYLE_VIEW_EVENT_FAILED("ST501", "스타일 조회 이벤트 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int status;
}