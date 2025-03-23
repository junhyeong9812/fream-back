package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 기간이 만료되었을 때 발생하는 예외
 */
public class StoragePeriodExpiredException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public StoragePeriodExpiredException() {
        super(WarehouseStorageErrorCode.STORAGE_PERIOD_EXPIRED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public StoragePeriodExpiredException(String message) {
        super(WarehouseStorageErrorCode.STORAGE_PERIOD_EXPIRED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public StoragePeriodExpiredException(Throwable cause) {
        super(WarehouseStorageErrorCode.STORAGE_PERIOD_EXPIRED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public StoragePeriodExpiredException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.STORAGE_PERIOD_EXPIRED, message, cause);
    }
}