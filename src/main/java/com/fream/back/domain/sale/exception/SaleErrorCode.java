package com.fream.back.domain.sale.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Sale 도메인과 관련된 에러 코드 정의
 * 접두사 'S'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum SaleErrorCode implements ErrorCode {

    // 판매 관련 에러
    /**
     * 판매 정보를 찾을 수 없음 (404)
     */
    SALE_NOT_FOUND("S001", "판매 정보를 찾을 수 없습니다.", 404),

    /**
     * 판매 상태 전환 불가 (400)
     */
    SALE_STATUS_TRANSITION_NOT_ALLOWED("S002", "현재 상태에서 요청한 상태로 전환할 수 없습니다.", 400),

    /**
     * 판매 삭제 불가 (400)
     */
    SALE_DELETION_NOT_ALLOWED("S003", "현재 상태에서는 판매 정보를 삭제할 수 없습니다.", 400),

    // 판매 입찰 관련 에러
    /**
     * 판매 입찰 정보를 찾을 수 없음 (404)
     */
    SALE_BID_NOT_FOUND("S101", "판매 입찰 정보를 찾을 수 없습니다.", 404),

    /**
     * 판매 입찰 생성 실패 (400)
     */
    SALE_BID_CREATION_FAILED("S102", "판매 입찰 생성에 실패했습니다.", 400),

    /**
     * 판매 입찰 삭제 불가 (400)
     */
    SALE_BID_DELETION_NOT_ALLOWED("S103", "현재 상태에서는 판매 입찰을 삭제할 수 없습니다.", 400),

    /**
     * 판매 입찰 가격 유효하지 않음 (400)
     */
    INVALID_BID_PRICE("S104", "유효하지 않은 입찰 가격입니다.", 400),

    /**
     * 판매자 계좌 정보 없음 (400)
     */
    SELLER_BANK_ACCOUNT_NOT_FOUND("S201", "판매자의 계좌 정보를 찾을 수 없습니다.", 400),

    /**
     * 즉시 판매 생성 실패 (400)
     */
    INSTANT_SALE_CREATION_FAILED("S301", "즉시 판매 생성에 실패했습니다.", 400),

    /**
     * 판매 배송 정보 생성 실패 (400)
     */
    SALE_SHIPMENT_CREATION_FAILED("S401", "판매 배송 정보 생성에 실패했습니다.", 400),

    /**
     * 배송 정보 유효하지 않음 (400)
     */
    INVALID_SHIPMENT_INFO("S402", "유효하지 않은 배송 정보입니다.", 400),

    /**
     * 접근 권한 없음 (403)
     */
    ACCESS_DENIED("S501", "해당 판매 정보에 접근할 권한이 없습니다.", 403);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}