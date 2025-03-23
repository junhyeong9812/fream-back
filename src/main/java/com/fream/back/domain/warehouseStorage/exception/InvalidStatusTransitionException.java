package com.fream.back.domain.warehouseStorage.exception;

/**
 * 유효하지 않은 상태 전환을 시도할 때 발생하는 예외
 */
public class InvalidStatusTransitionException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InvalidStatusTransitionException() {
        super(WarehouseStorageErrorCode.INVALID_STATUS_TRANSITION);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidStatusTransitionException(String message) {
        super(WarehouseStorageErrorCode.INVALID_STATUS_TRANSITION, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InvalidStatusTransitionException(Throwable cause) {
        super(WarehouseStorageErrorCode.INVALID_STATUS_TRANSITION, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InvalidStatusTransitionException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.INVALID_STATUS_TRANSITION, message, cause);
    }
}