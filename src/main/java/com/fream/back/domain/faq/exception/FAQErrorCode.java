package com.fream.back.domain.faq.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * FAQ 도메인에서 사용하는 에러 코드
 * 접두사 'FAQ'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum FAQErrorCode implements ErrorCode {

    // 데이터 조회 관련 에러
    /**
     * FAQ를 찾을 수 없음 (404)
     * 요청한 ID의 FAQ가 존재하지 않는 경우
     */
    FAQ_NOT_FOUND("FAQ001", "FAQ를 찾을 수 없습니다.", 404),

    /**
     * FAQ 조회 오류 (500)
     * FAQ 데이터 조회 중 발생한 오류
     */
    FAQ_QUERY_ERROR("FAQ002", "FAQ 조회 중 오류가 발생했습니다.", 500),

    // 데이터 수정 관련 에러
    /**
     * FAQ 저장 오류 (500)
     * FAQ 저장 중 발생한 오류
     */
    FAQ_SAVE_ERROR("FAQ003", "FAQ 저장 중 오류가 발생했습니다.", 500),

    /**
     * FAQ 삭제 오류 (500)
     * FAQ 삭제 중 발생한 오류
     */
    FAQ_DELETE_ERROR("FAQ004", "FAQ 삭제 중 오류가 발생했습니다.", 500),

    /**
     * FAQ 수정 오류 (500)
     * FAQ 수정 중 발생한 오류
     */
    FAQ_UPDATE_ERROR("FAQ005", "FAQ 수정 중 오류가 발생했습니다.", 500),

    // 파일 관련 에러
    /**
     * FAQ 파일 저장 오류 (500)
     * FAQ 관련 파일 저장 중 발생한 오류
     */
    FAQ_FILE_SAVE_ERROR("FAQ101", "FAQ 파일 저장 중 오류가 발생했습니다.", 500),

    /**
     * FAQ 파일 삭제 오류 (500)
     * FAQ 관련 파일 삭제 중 발생한 오류
     */
    FAQ_FILE_DELETE_ERROR("FAQ102", "FAQ 파일 삭제 중 오류가 발생했습니다.", 500),

    /**
     * FAQ 파일 조회 오류 (404)
     * FAQ 관련 파일을 찾을 수 없는 경우
     */
    FAQ_FILE_NOT_FOUND("FAQ103", "FAQ 파일을 찾을 수 없습니다.", 404),

    /**
     * 파일 형식 오류 (400)
     * 지원하지 않는 파일 형식을 업로드한 경우
     */
    FAQ_UNSUPPORTED_FILE_TYPE("FAQ104", "지원하지 않는 파일 형식입니다.", 400),

    // 권한 관련 에러
    /**
     * 관리자 권한 필요 (403)
     * FAQ 관리 작업에 관리자 권한이 필요한 경우
     */
    FAQ_ADMIN_PERMISSION_REQUIRED("FAQ201", "FAQ 관리는 관리자만 가능합니다.", 403),

    /**
     * 잘못된 카테고리 (400)
     * 요청한 FAQ 카테고리가 유효하지 않은 경우
     */
    FAQ_INVALID_CATEGORY("FAQ301", "유효하지 않은 FAQ 카테고리입니다.", 400),

    /**
     * 잘못된 요청 데이터 (400)
     * FAQ 저장/수정 시 잘못된 데이터가 전달된 경우
     */
    FAQ_INVALID_REQUEST_DATA("FAQ302", "유효하지 않은 FAQ 데이터입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}