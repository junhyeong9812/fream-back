package com.fream.back.domain.inspection.exception;

import com.fream.back.global.exception.ErrorCode;

/**
 * 검수 기준 관리 권한 관련 예외
 */
public class InspectionPermissionException extends InspectionException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public InspectionPermissionException() {
        super(InspectionErrorCode.INSPECTION_ADMIN_PERMISSION_REQUIRED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public InspectionPermissionException(String message) {
        super(InspectionErrorCode.INSPECTION_ADMIN_PERMISSION_REQUIRED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public InspectionPermissionException(Throwable cause) {
        super(InspectionErrorCode.INSPECTION_ADMIN_PERMISSION_REQUIRED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public InspectionPermissionException(String message, Throwable cause) {
        super(InspectionErrorCode.INSPECTION_ADMIN_PERMISSION_REQUIRED, message, cause);
    }
}