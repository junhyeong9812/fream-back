package com.fream.back.domain.inspection.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 검수 도메인에서 사용하는 에러 코드
 * 접두사 'INS'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum InspectionErrorCode implements ErrorCode {

    // 데이터 조회 관련 에러
    /**
     * 검수 기준을 찾을 수 없음 (404)
     * 요청한 ID의 검수 기준이 존재하지 않는 경우
     */
    INSPECTION_NOT_FOUND("INS001", "검수 기준을 찾을 수 없습니다.", 404),

    /**
     * 검수 기준 조회 오류 (500)
     * 검수 기준 데이터 조회 중 발생한 오류
     */
    INSPECTION_QUERY_ERROR("INS002", "검수 기준 조회 중 오류가 발생했습니다.", 500),

    // 데이터 수정 관련 에러
    /**
     * 검수 기준 저장 오류 (500)
     * 검수 기준 저장 중 발생한 오류
     */
    INSPECTION_SAVE_ERROR("INS003", "검수 기준 저장 중 오류가 발생했습니다.", 500),

    /**
     * 검수 기준 삭제 오류 (500)
     * 검수 기준 삭제 중 발생한 오류
     */
    INSPECTION_DELETE_ERROR("INS004", "검수 기준 삭제 중 오류가 발생했습니다.", 500),

    /**
     * 검수 기준 수정 오류 (500)
     * 검수 기준 수정 중 발생한 오류
     */
    INSPECTION_UPDATE_ERROR("INS005", "검수 기준 수정 중 오류가 발생했습니다.", 500),

    // 파일 관련 에러
    /**
     * 검수 기준 파일 저장 오류 (500)
     * 검수 기준 관련 파일 저장 중 발생한 오류
     */
    INSPECTION_FILE_SAVE_ERROR("INS101", "검수 기준 파일 저장 중 오류가 발생했습니다.", 500),

    /**
     * 검수 기준 파일 삭제 오류 (500)
     * 검수 기준 관련 파일 삭제 중 발생한 오류
     */
    INSPECTION_FILE_DELETE_ERROR("INS102", "검수 기준 파일 삭제 중 오류가 발생했습니다.", 500),

    /**
     * 검수 기준 파일 조회 오류 (404)
     * 검수 기준 관련 파일을 찾을 수 없는 경우
     */
    INSPECTION_FILE_NOT_FOUND("INS103", "검수 기준 파일을 찾을 수 없습니다.", 404),

    /**
     * 파일 형식 오류 (400)
     * 지원하지 않는 파일 형식을 업로드한 경우
     */
    INSPECTION_UNSUPPORTED_FILE_TYPE("INS104", "지원하지 않는 파일 형식입니다.", 400),

    // 권한 관련 에러
    /**
     * 관리자 권한 필요 (403)
     * 검수 기준 관리 작업에 관리자 권한이 필요한 경우
     */
    INSPECTION_ADMIN_PERMISSION_REQUIRED("INS201", "검수 기준 관리는 관리자만 가능합니다.", 403),

    /**
     * 잘못된 카테고리 (400)
     * 요청한 검수 기준 카테고리가 유효하지 않은 경우
     */
    INSPECTION_INVALID_CATEGORY("INS301", "유효하지 않은 검수 기준 카테고리입니다.", 400),

    /**
     * 잘못된 요청 데이터 (400)
     * 검수 기준 저장/수정 시 잘못된 데이터가 전달된 경우
     */
    INSPECTION_INVALID_REQUEST_DATA("INS302", "유효하지 않은 검수 기준 데이터입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}