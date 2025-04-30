package com.fream.back.domain.shipment.exception;

import com.fream.back.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * 배송 관련 에러 코드 정의
 */
public enum ShipmentErrorCode implements ErrorCode {

    // 공통 에러 코드 (S000 ~ S099)
    SHIPMENT_NOT_FOUND("S001", "배송 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    INVALID_SHIPMENT_STATUS_TRANSITION("S002", "유효하지 않은 배송 상태 전환입니다.", HttpStatus.BAD_REQUEST.value()),
    TRACKING_NUMBER_INVALID("S003", "유효하지 않은 운송장 번호입니다.", HttpStatus.BAD_REQUEST.value()),
    TRACKING_INFO_REQUIRED("S004", "배송사와 운송장 번호가 필요합니다.", HttpStatus.BAD_REQUEST.value()),
    TRACKING_SERVICE_UNAVAILABLE("S005", "배송 조회 서비스를 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE.value()),

    // 주문 배송 관련 에러 코드 (S100 ~ S199)
    ORDER_SHIPMENT_NOT_FOUND("S101", "주문 배송 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    ORDER_NOT_FOUND_FOR_SHIPMENT("S102", "배송과 연결된 주문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    ORDER_ALREADY_COMPLETED("S103", "이미 완료된 주문의 배송 상태는 변경할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    // 판매자 배송 관련 에러 코드 (S200 ~ S299)
    SELLER_SHIPMENT_NOT_FOUND("S201", "판매자 배송 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    SALE_NOT_FOUND_FOR_SHIPMENT("S202", "배송과 연결된 판매를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    SALE_ALREADY_COMPLETED("S203", "이미 완료된 판매의 배송 상태는 변경할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    // 배송 외부 서비스 관련 에러 코드 (S300 ~ S399)
    TRACKING_HTML_PARSE_ERROR("S301", "운송장 조회 결과를 파싱할 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    EXTERNAL_TRACKING_SERVICE_ERROR("S302", "외부 배송 조회 서비스에 오류가 발생했습니다.", HttpStatus.SERVICE_UNAVAILABLE.value()),
    BROWSER_INITIALIZATION_ERROR("S303", "배송 조회를 위한 브라우저 초기화에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int status;

    ShipmentErrorCode(String code, String message, int status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}