package com.fream.back.domain.warehouseStorage.exception;

/**
 * 창고 보관 상품의 상태 확인에 실패했을 때 발생하는 예외
 */
public class ItemStatusCheckFailedException extends WarehouseStorageException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public ItemStatusCheckFailedException() {
        super(WarehouseStorageErrorCode.ITEM_STATUS_CHECK_FAILED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public ItemStatusCheckFailedException(String message) {
        super(WarehouseStorageErrorCode.ITEM_STATUS_CHECK_FAILED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public ItemStatusCheckFailedException(Throwable cause) {
        super(WarehouseStorageErrorCode.ITEM_STATUS_CHECK_FAILED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public ItemStatusCheckFailedException(String message, Throwable cause) {
        super(WarehouseStorageErrorCode.ITEM_STATUS_CHECK_FAILED, message, cause);
    }
}