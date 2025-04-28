package com.fream.back.domain.notice.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공지사항 도메인에서 사용하는 에러 코드
 * 접두사 'NOT'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements ErrorCode {

    // 데이터 조회 관련 에러
    NOTICE_NOT_FOUND("NOT001", "공지사항을 찾을 수 없습니다.", 404),
    NOTICE_QUERY_ERROR("NOT002", "공지사항 조회 중 오류가 발생했습니다.", 500),

    // 데이터 수정 관련 에러
    NOTICE_SAVE_ERROR("NOT003", "공지사항 저장 중 오류가 발생했습니다.", 500),
    NOTICE_DELETE_ERROR("NOT004", "공지사항 삭제 중 오류가 발생했습니다.", 500),
    NOTICE_UPDATE_ERROR("NOT005", "공지사항 수정 중 오류가 발생했습니다.", 500),

    // 파일 관련 에러
    NOTICE_FILE_SAVE_ERROR("NOT101", "공지사항 파일 저장 중 오류가 발생했습니다.", 500),
    NOTICE_FILE_DELETE_ERROR("NOT102", "공지사항 파일 삭제 중 오류가 발생했습니다.", 500),
    NOTICE_FILE_NOT_FOUND("NOT103", "공지사항 파일을 찾을 수 없습니다.", 404),
    NOTICE_UNSUPPORTED_FILE_TYPE("NOT104", "지원하지 않는 파일 형식입니다.", 400),

    // 권한 관련 에러
    NOTICE_ADMIN_PERMISSION_REQUIRED("NOT201", "공지사항 관리는 관리자만 가능합니다.", 403),

    // 입력 데이터 관련 에러
    NOTICE_INVALID_CATEGORY("NOT301", "유효하지 않은 공지사항 카테고리입니다.", 400),
    NOTICE_INVALID_REQUEST_DATA("NOT302", "유효하지 않은 공지사항 데이터입니다.", 400),

    // 기타 에러
    NOTICE_NOTIFICATION_ERROR("NOT401", "공지사항 알림 발송 중 오류가 발생했습니다.", 500),
    NOTICE_HTML_PARSING_ERROR("NOT402", "HTML 콘텐츠 처리 중 오류가 발생했습니다.", 500);

    private final String code;  // 에러 코드
    private final String message;  // 에러 메시지
    private final int status;  // HTTP 상태 코드

}