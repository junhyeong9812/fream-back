package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 기간 연장에 실패했을 때 발생하는 예외
 */
public class StorageExtensionFailedException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public StorageExtensionFailedException() {
        super(WarehouseStorageErrorCode.STORAGE_EXTENSION_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public StorageExtensionFailedException(String message) {
        super(WarehouseStorageErrorCode.STORAGE_EXTENSION_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public StorageExtensionFailedException(Throwable cause) {
        super(WarehouseStorageErrorCode.STORAGE_EXTENSION_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public StorageExtensionFailedException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.STORAGE_EXTENSION_FAILED, message, cause);
    }
}