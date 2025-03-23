package com.fream.back.domain.payment.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 결제 도메인에서 사용하는 에러 코드
 * 접두사 'PAY'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentErrorCode implements ErrorCode {

    // 결제 프로세스 관련 에러
    /**
     * 결제 처리 실패 (500)
     * 결제 처리 중 오류가 발생한 경우
     */
    PAYMENT_PROCESSING_FAILED("PAY001", "결제 처리에 실패했습니다.", 500),

    /**
     * 결제 취소 실패 (500)
     * 결제 취소/환불 중 오류가 발생한 경우
     */
    PAYMENT_CANCELLATION_FAILED("PAY002", "결제 취소에 실패했습니다.", 500),

    /**
     * 결제 조회 실패 (500)
     * 결제 정보 조회 중 오류가 발생한 경우
     */
    PAYMENT_RETRIEVAL_FAILED("PAY003", "결제 정보 조회에 실패했습니다.", 500),

    /**
     * 결제 검증 실패 (400)
     * 결제 정보 검증에 실패한 경우
     */
    PAYMENT_VALIDATION_FAILED("PAY004", "결제 정보 검증에 실패했습니다.", 400),

    /**
     * 결제 정보를 찾을 수 없음 (404)
     * 요청한 ID의 결제 정보가 존재하지 않는 경우
     */
    PAYMENT_NOT_FOUND("PAY005", "결제 정보를 찾을 수 없습니다.", 404),

    /**
     * 잘못된 결제 상태 (400)
     * 현재 결제 상태에서 요청한 작업을 수행할 수 없는 경우
     */
    INVALID_PAYMENT_STATUS("PAY006", "유효하지 않은 결제 상태입니다.", 400),

    /**
     * 결제 금액 오류 (400)
     * 결제 금액이 유효하지 않은 경우
     */
    INVALID_PAYMENT_AMOUNT("PAY007", "유효하지 않은 결제 금액입니다.", 400),

    /**
     * 결제 정보 업데이트 실패 (500)
     * 결제 정보 업데이트 중 오류가 발생한 경우
     */
    PAYMENT_UPDATE_FAILED("PAY008", "결제 정보 업데이트에 실패했습니다.", 500),

    /**
     * 알 수 없는 결제 유형 (400)
     * 지원하지 않는 결제 유형이 제공된 경우
     */
    UNKNOWN_PAYMENT_TYPE("PAY009", "알 수 없는 결제 유형입니다.", 400),

    // 결제 정보(PaymentInfo) 관련 에러
    /**
     * 결제 정보 생성 실패 (500)
     * 결제 정보 생성 중 오류가 발생한 경우
     */
    PAYMENT_INFO_CREATION_FAILED("PAY101", "결제 정보 생성에 실패했습니다.", 500),

    /**
     * 결제 정보 삭제 실패 (500)
     * 결제 정보 삭제 중 오류가 발생한 경우
     */
    PAYMENT_INFO_DELETION_FAILED("PAY102", "결제 정보 삭제에 실패했습니다.", 500),

    /**
     * 결제 정보 업데이트 실패 (500)
     * 결제 정보 업데이트 중 오류가 발생한 경우
     */
    PAYMENT_INFO_UPDATE_FAILED("PAY103", "결제 정보 업데이트에 실패했습니다.", 500),

    /**
     * 결제 정보를 찾을 수 없음 (404)
     * 요청한 ID의 결제 정보가 존재하지 않는 경우
     */
    PAYMENT_INFO_NOT_FOUND("PAY104", "결제 정보를 찾을 수 없습니다.", 404),

    /**
     * 유효하지 않은 카드 정보 (400)
     * 카드 번호, 유효기간, 비밀번호 등이 유효하지 않은 경우
     */
    INVALID_CARD_INFO("PAY105", "유효하지 않은 카드 정보입니다.", 400),

    /**
     * 결제 정보 최대 개수 초과 (400)
     * 사용자가 저장할 수 있는 결제 정보 최대 개수를 초과한 경우
     */
    PAYMENT_INFO_LIMIT_EXCEEDED("PAY106", "결제 정보 최대 저장 개수를 초과했습니다.", 400),

    // 외부 API 관련 에러
    /**
     * 결제 API 연동 오류 (500)
     * 외부 결제 서비스(PortOne)와의 통신 중 오류가 발생한 경우
     */
    PAYMENT_API_ERROR("PAY201", "결제 서비스와 통신 중 오류가 발생했습니다.", 500),

    /**
     * 토큰 발급 실패 (500)
     * 결제 API 토큰 발급 중 오류가 발생한 경우
     */
    TOKEN_ISSUANCE_FAILED("PAY202", "결제 서비스 인증 토큰 발급에 실패했습니다.", 500),

    /**
     * API 요청 실패 (500)
     * 결제 API 요청이 실패한 경우
     */
    API_REQUEST_FAILED("PAY203", "결제 서비스 API 요청에 실패했습니다.", 500),

    /**
     * 결제 승인 실패 (400)
     * 결제 승인이 거절된 경우
     */
    PAYMENT_APPROVAL_DENIED("PAY204", "결제 승인이 거절되었습니다.", 400),

    // 권한 관련 에러
    /**
     * 결제 접근 권한 없음 (403)
     * 다른 사용자의 결제 정보에 접근 시도한 경우
     */
    PAYMENT_ACCESS_DENIED("PAY301", "해당 결제 정보에 대한 접근 권한이 없습니다.", 403),

    /**
     * 결제 정보 접근 권한 없음 (403)
     * 다른 사용자의 결제 수단 정보에 접근 시도한 경우
     */
    PAYMENT_INFO_ACCESS_DENIED("PAY302", "해당 결제 수단 정보에 대한 접근 권한이 없습니다.", 403),

    // 계좌이체 관련 에러
    /**
     * 유효하지 않은 은행 정보 (400)
     * 지원하지 않는 은행이나 유효하지 않은 은행 정보가 제공된 경우
     */
    INVALID_BANK_INFO("PAY401", "유효하지 않은 은행 정보입니다.", 400),

    /**
     * 유효하지 않은 계좌 정보 (400)
     * 계좌번호나 예금주 정보가 유효하지 않은 경우
     */
    INVALID_ACCOUNT_INFO("PAY402", "유효하지 않은 계좌 정보입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}