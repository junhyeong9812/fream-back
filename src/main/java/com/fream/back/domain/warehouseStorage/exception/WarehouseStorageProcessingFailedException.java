package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 처리 중 오류가 발생했을 때 발생하는 예외
 */
public class WarehouseStorageProcessingFailedException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public WarehouseStorageProcessingFailedException() {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WarehouseStorageProcessingFailedException(String message) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WarehouseStorageProcessingFailedException(Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WarehouseStorageProcessingFailedException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_PROCESSING_FAILED, message, cause);
    }
}