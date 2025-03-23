package com.fream.back.domain.order.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 도메인에서 사용하는 에러 코드
 * 접두사 'ORD'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

    // 주문(Order) 관련 에러
    /**
     * 주문을 찾을 수 없음 (404)
     * 요청한 ID의 주문이 존재하지 않는 경우
     */
    ORDER_NOT_FOUND("ORD001", "주문을 찾을 수 없습니다.", 404),

    /**
     * 주문 생성 실패 (500)
     * 주문 생성 중 오류가 발생한 경우
     */
    ORDER_CREATION_FAILED("ORD002", "주문 생성에 실패했습니다.", 500),

    /**
     * 주문 수정 실패 (500)
     * 주문 수정 중 오류가 발생한 경우
     */
    ORDER_UPDATE_FAILED("ORD003", "주문 수정에 실패했습니다.", 500),

    /**
     * 주문 삭제 실패 (500)
     * 주문 삭제 중 오류가 발생한 경우
     */
    ORDER_DELETION_FAILED("ORD004", "주문 삭제에 실패했습니다.", 500),

    /**
     * 주문 결제 처리 실패 (500)
     * 주문 결제 처리 중 오류가 발생한 경우
     */
    ORDER_PAYMENT_PROCESSING_FAILED("ORD005", "주문 결제 처리에 실패했습니다.", 500),

    /**
     * 주문 배송 처리 실패 (500)
     * 주문 배송 처리 중 오류가 발생한 경우
     */
    ORDER_SHIPMENT_PROCESSING_FAILED("ORD006", "주문 배송 처리에 실패했습니다.", 500),

    /**
     * 주문 상태 변경 실패 (500)
     * 주문 상태 변경 중 오류가 발생한 경우
     */
    ORDER_STATUS_UPDATE_FAILED("ORD007", "주문 상태 변경에 실패했습니다.", 500),

    /**
     * 유효하지 않은 주문 상태 (400)
     * 현재 주문 상태에서 요청한 작업을 수행할 수 없는 경우
     */
    INVALID_ORDER_STATUS("ORD008", "유효하지 않은 주문 상태입니다.", 400),

    // 주문 입찰(OrderBid) 관련 에러
    /**
     * 주문 입찰을 찾을 수 없음 (404)
     * 요청한 ID의 주문 입찰이 존재하지 않는 경우
     */
    ORDER_BID_NOT_FOUND("ORD101", "주문 입찰을 찾을 수 없습니다.", 404),

    /**
     * 주문 입찰 생성 실패 (500)
     * 주문 입찰 생성 중 오류가 발생한 경우
     */
    ORDER_BID_CREATION_FAILED("ORD102", "주문 입찰 생성에 실패했습니다.", 500),

    /**
     * 주문 입찰 수정 실패 (500)
     * 주문 입찰 수정 중 오류가 발생한 경우
     */
    ORDER_BID_UPDATE_FAILED("ORD103", "주문 입찰 수정에 실패했습니다.", 500),

    /**
     * 주문 입찰 삭제 실패 (500)
     * 주문 입찰 삭제 중 오류가 발생한 경우
     */
    ORDER_BID_DELETION_FAILED("ORD104", "주문 입찰 삭제에 실패했습니다.", 500),

    /**
     * 주문 입찰 매칭 실패 (500)
     * 주문 입찰 매칭 중 오류가 발생한 경우
     */
    ORDER_BID_MATCHING_FAILED("ORD105", "주문 입찰 매칭에 실패했습니다.", 500),

    /**
     * 유효하지 않은 주문 입찰 상태 (400)
     * 현재 주문 입찰 상태에서 요청한 작업을 수행할 수 없는 경우
     */
    INVALID_ORDER_BID_STATUS("ORD106", "유효하지 않은 주문 입찰 상태입니다.", 400),

    /**
     * 주문 입찰 매칭 불가 (400)
     * 주문 입찰이 이미 매칭되었거나 매칭할 수 없는 상태인 경우
     */
    ORDER_BID_ALREADY_MATCHED("ORD107", "이미 매칭된 주문 입찰입니다.", 400),

    /**
     * 주문 입찰 삭제 불가 (400)
     * 주문 입찰이 이미 매칭되어 삭제할 수 없는 경우
     */
    ORDER_BID_CANNOT_BE_DELETED("ORD108", "매칭된 주문 입찰은 삭제할 수 없습니다.", 400),

    // 주문 항목(OrderItem) 관련 에러
    /**
     * 주문 항목을 찾을 수 없음 (404)
     * 요청한 ID의 주문 항목이 존재하지 않는 경우
     */
    ORDER_ITEM_NOT_FOUND("ORD201", "주문 항목을 찾을 수 없습니다.", 404),

    /**
     * 주문 항목 생성 실패 (500)
     * 주문 항목 생성 중 오류가 발생한 경우
     */
    ORDER_ITEM_CREATION_FAILED("ORD202", "주문 항목 생성에 실패했습니다.", 500),

    // 권한 관련 에러
    /**
     * 주문 접근 권한 없음 (403)
     * 다른 사용자의 주문에 접근 시도한 경우
     */
    ORDER_ACCESS_DENIED("ORD301", "해당 주문에 대한 접근 권한이 없습니다.", 403),

    /**
     * 주문 입찰 접근 권한 없음 (403)
     * 다른 사용자의 주문 입찰에 접근 시도한 경우
     */
    ORDER_BID_ACCESS_DENIED("ORD302", "해당 주문 입찰에 대한 접근 권한이 없습니다.", 403),

    // 입력값 관련 에러
    /**
     * 유효하지 않은 주문 정보 (400)
     * 주문 정보가 유효하지 않은 경우
     */
    INVALID_ORDER_DATA("ORD401", "유효하지 않은 주문 정보입니다.", 400),

    /**
     * 유효하지 않은 주문 입찰 정보 (400)
     * 주문 입찰 정보가 유효하지 않은 경우
     */
    INVALID_ORDER_BID_DATA("ORD402", "유효하지 않은 주문 입찰 정보입니다.", 400),

    /**
     * 유효하지 않은 결제 및 배송 정보 (400)
     * 결제 및 배송 정보가 유효하지 않은 경우
     */
    INVALID_PAYMENT_SHIPMENT_DATA("ORD403", "유효하지 않은 결제 및 배송 정보입니다.", 400),

    /**
     * 유효하지 않은 입찰 가격 (400)
     * 입찰 가격이 유효하지 않은 경우
     */
    INVALID_BID_PRICE("ORD404", "유효하지 않은 입찰 가격입니다.", 400),

    // 외부 관련 에러
    /**
     * 상품 사이즈를 찾을 수 없음 (404)
     * 주문에 필요한 상품 사이즈를 찾을 수 없는 경우
     */
    PRODUCT_SIZE_NOT_FOUND("ORD501", "상품 사이즈를 찾을 수 없습니다.", 404),

    /**
     * 판매 입찰을 찾을 수 없음 (404)
     * 즉시 구매에 필요한 판매 입찰을 찾을 수 없는 경우
     */
    SALE_BID_NOT_FOUND("ORD502", "판매 입찰을 찾을 수 없습니다.", 404),

    /**
     * 창고 보관 처리 실패 (500)
     * 창고 보관 처리 중 오류가 발생한 경우
     */
    WAREHOUSE_STORAGE_PROCESSING_FAILED("ORD503", "창고 보관 처리에 실패했습니다.", 500);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}