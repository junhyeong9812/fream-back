package com.fream.back.domain.address.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주소 도메인에서 사용하는 에러 코드
 * 접두사 'ADR'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements ErrorCode {

    // 데이터 조회 관련 에러
    /**
     * 주소를 찾을 수 없음 (404)
     * 요청한 ID의 주소가 존재하지 않는 경우
     */
    ADDRESS_NOT_FOUND("ADR001", "주소를 찾을 수 없습니다.", 404),

    /**
     * 주소 목록 조회 오류 (500)
     * 주소 목록 조회 중 발생한 오류
     */
    ADDRESS_QUERY_ERROR("ADR002", "주소 목록 조회 중 오류가 발생했습니다.", 500),

    // 데이터 수정 관련 에러
    /**
     * 주소 생성 오류 (500)
     * 주소 생성 중 발생한 오류
     */
    ADDRESS_CREATE_ERROR("ADR003", "주소 생성 중 오류가 발생했습니다.", 500),

    /**
     * 주소 수정 오류 (500)
     * 주소 수정 중 발생한 오류
     */
    ADDRESS_UPDATE_ERROR("ADR004", "주소 수정 중 오류가 발생했습니다.", 500),

    /**
     * 주소 삭제 오류 (500)
     * 주소 삭제 중 발생한 오류
     */
    ADDRESS_DELETE_ERROR("ADR005", "주소 삭제 중 오류가 발생했습니다.", 500),

    // 사용자 관련 에러
    /**
     * 사용자를 찾을 수 없음 (404)
     * 주소와 연관된 사용자를 찾을 수 없는 경우
     */
    ADDRESS_USER_NOT_FOUND("ADR101", "주소와 연관된 사용자를 찾을 수 없습니다.", 404),

    // 권한 관련 에러
    /**
     * 주소 접근 권한 없음 (403)
     * 다른 사용자의 주소에 접근 시도한 경우
     */
    ADDRESS_ACCESS_DENIED("ADR201", "해당 주소에 대한 접근 권한이 없습니다.", 403),

    // 입력값 관련 에러
    /**
     * 잘못된 주소 정보 (400)
     * 주소 생성/수정 시 잘못된 정보가 제공된 경우
     */
    ADDRESS_INVALID_DATA("ADR301", "유효하지 않은 주소 정보입니다.", 400),

    /**
     * 우편번호 형식 오류 (400)
     * 유효하지 않은 우편번호 형식이 제공된 경우
     */
    ADDRESS_INVALID_ZIP_CODE("ADR302", "유효하지 않은 우편번호 형식입니다.", 400),

    /**
     * 전화번호 형식 오류 (400)
     * 유효하지 않은 전화번호 형식이 제공된 경우
     */
    ADDRESS_INVALID_PHONE_NUMBER("ADR303", "유효하지 않은 전화번호 형식입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}