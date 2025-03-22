package com.fream.back.domain.inspection.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 검수 기준을 찾을 수 없을 때 발생하는 예외
 */
public class InspectionNotFoundException extends InspectionException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InspectionNotFoundException() {
        super(InspectionErrorCode.INSPECTION_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InspectionNotFoundException(String message) {
        super(InspectionErrorCode.INSPECTION_NOT_FOUND, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InspectionNotFoundException(Throwable cause) {
        super(InspectionErrorCode.INSPECTION_NOT_FOUND, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InspectionNotFoundException(String message, Throwable cause) {
        super(InspectionErrorCode.INSPECTION_NOT_FOUND, message, cause);
    }
}