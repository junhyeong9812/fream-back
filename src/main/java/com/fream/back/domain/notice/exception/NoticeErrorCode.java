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
    /**
     * 공지사항을 찾을 수 없음 (404)
     * 요청한 ID의 공지사항이 존재하지 않는 경우
     */
    NOTICE_NOT_FOUND("NOT001", "공지사항을 찾을 수 없습니다.", 404),

    /**
     * 공지사항 조회 오류 (500)
     * 공지사항 데이터 조회 중 발생한 오류
     */
    NOTICE_QUERY_ERROR("NOT002", "공지사항 조회 중 오류가 발생했습니다.", 500),

    // 데이터 수정 관련 에러
    /**
     * 공지사항 저장 오류 (500)
     * 공지사항 저장 중 발생한 오류
     */
    NOTICE_SAVE_ERROR("NOT003", "공지사항 저장 중 오류가 발생했습니다.", 500),

    /**
     * 공지사항 삭제 오류 (500)
     * 공지사항 삭제 중 발생한 오류
     */
    NOTICE_DELETE_ERROR("NOT004", "공지사항 삭제 중 오류가 발생했습니다.", 500),

    /**
     * 공지사항 수정 오류 (500)
     * 공지사항 수정 중 발생한 오류
     */
    NOTICE_UPDATE_ERROR("NOT005", "공지사항 수정 중 오류가 발생했습니다.", 500),

    // 파일 관련 에러
    /**
     * 공지사항 파일 저장 오류 (500)
     * 공지사항 관련 파일 저장 중 발생한 오류
     */
    NOTICE_FILE_SAVE_ERROR("NOT101", "공지사항 파일 저장 중 오류가 발생했습니다.", 500),

    /**
     * 공지사항 파일 삭제 오류 (500)
     * 공지사항 관련 파일 삭제 중 발생한 오류
     */
    NOTICE_FILE_DELETE_ERROR("NOT102", "공지사항 파일 삭제 중 오류가 발생했습니다.", 500),

    /**
     * 공지사항 파일 조회 오류 (404)
     * 공지사항 관련 파일을 찾을 수 없는 경우
     */
    NOTICE_FILE_NOT_FOUND("NOT103", "공지사항 파일을 찾을 수 없습니다.", 404),

    /**
     * 파일 형식 오류 (400)
     * 지원하지 않는 파일 형식을 업로드한 경우
     */
    NOTICE_UNSUPPORTED_FILE_TYPE("NOT104", "지원하지 않는 파일 형식입니다.", 400),

    // 권한 관련 에러
    /**
     * 관리자 권한 필요 (403)
     * 공지사항 관리 작업에 관리자 권한이 필요한 경우
     */
    NOTICE_ADMIN_PERMISSION_REQUIRED("NOT201", "공지사항 관리는 관리자만 가능합니다.", 403),

    /**
     * 잘못된 카테고리 (400)
     * 요청한 공지사항 카테고리가 유효하지 않은 경우
     */
    NOTICE_INVALID_CATEGORY("NOT301", "유효하지 않은 공지사항 카테고리입니다.", 400),

    /**
     * 잘못된 요청 데이터 (400)
     * 공지사항 저장/수정 시 잘못된 데이터가 전달된 경우
     */
    NOTICE_INVALID_REQUEST_DATA("NOT302", "유효하지 않은 공지사항 데이터입니다.", 400),

    /**
     * 알림 발송 오류 (500)
     * 공지사항 알림 발송 중 오류 발생
     */
    NOTICE_NOTIFICATION_ERROR("NOT401", "공지사항 알림 발송 중 오류가 발생했습니다.", 500);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}