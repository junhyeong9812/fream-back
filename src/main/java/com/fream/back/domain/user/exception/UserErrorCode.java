package com.fream.back.domain.user.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사용자 도메인 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 일반 사용자 관련 에러
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    DUPLICATE_EMAIL("U002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT.value()),
    DUPLICATE_PHONE_NUMBER("U003", "이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT.value()),
    INVALID_PASSWORD("U004", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_VERIFICATION_CODE("U005", "유효하지 않은 인증 코드입니다.", HttpStatus.BAD_REQUEST.value()),
    IDENTITY_VERIFICATION_FAILED("U006", "본인인증에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    INSUFFICIENT_PERMISSIONS("U007", "권한이 부족합니다.", HttpStatus.FORBIDDEN.value()),

    // 계정 관련 에러
    ACCOUNT_LOCKED("U101", "계정이 잠겼습니다. 관리자에게 문의하세요.", HttpStatus.FORBIDDEN.value()),
    ACCOUNT_INACTIVE("U102", "비활성화된 계정입니다.", HttpStatus.FORBIDDEN.value()),
    ACCOUNT_ALREADY_VERIFIED("U103", "이미 인증된 계정입니다.", HttpStatus.BAD_REQUEST.value()),

    // 프로필 관련 에러
    PROFILE_NOT_FOUND("U201", "프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    INVALID_PROFILE_IMAGE("U202", "잘못된 형식의 프로필 이미지입니다.", HttpStatus.BAD_REQUEST.value()),
    PROFILE_IMAGE_TOO_LARGE("U203", "프로필 이미지 크기가 너무 큽니다.", HttpStatus.BAD_REQUEST.value()),

    // 포인트 관련 에러
    INSUFFICIENT_POINTS("U301", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST.value()),
    POINT_HISTORY_NOT_FOUND("U302", "포인트 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    INVALID_POINT_AMOUNT("U303", "잘못된 포인트 금액입니다.", HttpStatus.BAD_REQUEST.value()),

    // 팔로우 관련 에러
    ALREADY_FOLLOWING("U401", "이미 팔로우 중인 사용자입니다.", HttpStatus.CONFLICT.value()),
    NOT_FOLLOWING("U402", "팔로우 중이 아닌 사용자입니다.", HttpStatus.BAD_REQUEST.value()),
    CANNOT_FOLLOW_SELF("U403", "자기 자신을 팔로우할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    // 차단 관련 에러
    ALREADY_BLOCKED("U501", "이미 차단된 사용자입니다.", HttpStatus.CONFLICT.value()),
    NOT_BLOCKED("U502", "차단되지 않은 사용자입니다.", HttpStatus.BAD_REQUEST.value()),
    CANNOT_BLOCK_SELF("U503", "자기 자신을 차단할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    // 은행 계좌 관련 에러
    BANK_ACCOUNT_NOT_FOUND("U601", "등록된 계좌 정보가 없습니다.", HttpStatus.NOT_FOUND.value()),
    INVALID_BANK_ACCOUNT("U602", "잘못된 계좌 정보입니다.", HttpStatus.BAD_REQUEST.value()),

    // 등급 관련 에러
    GRADE_NOT_FOUND("U701", "등급 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),

    // 제재 관련 에러
    SANCTION_NOT_FOUND("U801", "제재 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    USER_SANCTIONED("U802", "제재 중인 사용자입니다.", HttpStatus.FORBIDDEN.value()),

    // OAuth 관련 에러
    OAUTH_PROVIDER_NOT_SUPPORTED("U901", "지원하지 않는 OAuth 제공자입니다.", HttpStatus.BAD_REQUEST.value()),
    OAUTH_AUTHENTICATION_FAILED("U902", "OAuth 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED.value());

    private final String code;
    private final String message;
    private final int status;
}