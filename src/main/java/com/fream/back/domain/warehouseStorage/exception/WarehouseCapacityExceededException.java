package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 용량을 초과했을 때 발생하는 예외
 */
public class WarehouseCapacityExceededException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public WarehouseCapacityExceededException() {
        super(WarehouseStorageErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WarehouseCapacityExceededException(String message) {
        super(WarehouseStorageErrorCode.WAREHOUSE_CAPACITY_EXCEEDED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WarehouseCapacityExceededException(Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_CAPACITY_EXCEEDED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WarehouseCapacityExceededException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_CAPACITY_EXCEEDED, message, cause);
    }
}