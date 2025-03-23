package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 정보를 찾을 수 없을 때 발생하는 예외
 */
public class WarehouseStorageNotFoundException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public WarehouseStorageNotFoundException() {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WarehouseStorageNotFoundException(String message) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_NOT_FOUND, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WarehouseStorageNotFoundException(Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_NOT_FOUND, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WarehouseStorageNotFoundException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_NOT_FOUND, message, cause);
    }
}