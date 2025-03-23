package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 정보에 접근 권한이 없을 때 발생하는 예외
 */
public class WarehouseStorageAccessDeniedException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public WarehouseStorageAccessDeniedException() {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_ACCESS_DENIED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public WarehouseStorageAccessDeniedException(String message) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_ACCESS_DENIED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public WarehouseStorageAccessDeniedException(Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_ACCESS_DENIED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public WarehouseStorageAccessDeniedException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.WAREHOUSE_STORAGE_ACCESS_DENIED, message, cause);
    }
}