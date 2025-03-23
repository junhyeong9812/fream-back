package com.fream.back.domain.warehouseStorage.exception;

/**
 * 유효하지 않은 창고 상태일 때 발생하는 예외
 */
public class InvalidWarehouseStatusException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InvalidWarehouseStatusException() {
        super(WarehouseStorageErrorCode.INVALID_WAREHOUSE_STATUS);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InvalidWarehouseStatusException(String message) {
        super(WarehouseStorageErrorCode.INVALID_WAREHOUSE_STATUS, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InvalidWarehouseStatusException(Throwable cause) {
        super(WarehouseStorageErrorCode.INVALID_WAREHOUSE_STATUS, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InvalidWarehouseStatusException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.INVALID_WAREHOUSE_STATUS, message, cause);
    }
}