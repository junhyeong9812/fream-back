package com.fream.back.domain.order.exception;

/**
 * 주문 관련 창고 보관 처리 시 발생하는 예외
 * 주문 도메인에서 창고 보관 기능을 사용할 때 발생하는 예외를 처리
 */
public class OrderWarehouseStorageProcessingFailedException extends OrderException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public OrderWarehouseStorageProcessingFailedException() {
        super(OrderErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public OrderWarehouseStorageProcessingFailedException(String message) {
        super(OrderErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public OrderWarehouseStorageProcessingFailedException(Throwable cause) {
        super(OrderErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public OrderWarehouseStorageProcessingFailedException(String message, Throwable cause) {
        super(OrderErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED, message, cause);
    }
}