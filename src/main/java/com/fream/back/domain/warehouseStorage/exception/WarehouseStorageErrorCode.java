package com.fream.back.domain.warehouseStorage.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 창고 보관 도메인에서 사용하는 에러 코드
 * 접두사 'WH'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum WarehouseStorageErrorCode implements ErrorCode {

    // 기본 에러
    /**
     * 창고 보관 정보를 찾을 수 없음 (404)
     * 요청한 ID의 창고 보관 정보가 존재하지 않는 경우
     */
    WAREHOUSE_STORAGE_NOT_FOUND("WH001", "창고 보관 정보를 찾을 수 없습니다.", 404),

    /**
     * 유효하지 않은 창고 상태 (400)
     * 요청한 창고 상태가 유효하지 않은 경우
     */
    INVALID_WAREHOUSE_STATUS("WH002", "유효하지 않은 창고 상태입니다.", 400),

    /**
     * 유효하지 않은 상태 전환 (400)
     * 현재 상태에서 요청한 상태로 전환할 수 없는 경우
     */
    INVALID_STATUS_TRANSITION("WH003", "유효하지 않은 상태 전환입니다.", 400),

    /**
     * 창고 보관 기간 만료 (400)
     * 창고 보관 기간이 만료된 경우
     */
    STORAGE_PERIOD_EXPIRED("WH004", "창고 보관 기간이 만료되었습니다.", 400),

    /**
     * 창고 보관 처리 실패 (500)
     * 창고 보관 처리 중 오류가 발생한 경우
     */
    WAREHOUSE_STORAGE_PROCESSING_FAILED("WH005", "창고 보관 처리에 실패했습니다.", 500),

    /**
     * 창고 보관 정보 접근 권한 없음 (403)
     * 다른 사용자의 창고 보관 정보에 접근 시도한 경우
     */
    WAREHOUSE_STORAGE_ACCESS_DENIED("WH006", "해당 창고 보관 정보에 대한 접근 권한이 없습니다.", 403),

    /**
     * 창고 용량 초과 (400)
     * 창고 보관 용량을 초과한 경우
     */
    WAREHOUSE_CAPACITY_EXCEEDED("WH007", "창고 보관 용량을 초과했습니다.", 400),

    /**
     * 창고 보관 기간 연장 실패 (400)
     * 창고 보관 기간 연장에 실패한 경우
     */
    STORAGE_EXTENSION_FAILED("WH008", "창고 보관 기간 연장에 실패했습니다.", 400),

    /**
     * 창고 보관 지역 할당 실패 (500)
     * 창고 내 보관 위치 할당에 실패한 경우
     */
    STORAGE_LOCATION_ALLOCATION_FAILED("WH009", "창고 보관 위치 할당에 실패했습니다.", 500),

    /**
     * 창고 보관 상품 상태 확인 실패 (500)
     * 창고 보관 상품의 상태 확인에 실패한 경우
     */
    ITEM_STATUS_CHECK_FAILED("WH010", "창고 보관 상품 상태 확인에 실패했습니다.", 500);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}