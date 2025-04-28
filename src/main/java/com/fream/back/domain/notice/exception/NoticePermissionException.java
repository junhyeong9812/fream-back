package com.fream.back.domain.notice.exception;

/**
 * 공지사항 관리 권한 관련 예외
 */
public class NoticePermissionException extends NoticeException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public NoticePermissionException() {
        super(NoticeErrorCode.NOTICE_ADMIN_PERMISSION_REQUIRED);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public NoticePermissionException(String message) {
        super(NoticeErrorCode.NOTICE_ADMIN_PERMISSION_REQUIRED, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public NoticePermissionException(Throwable cause) {
        super(NoticeErrorCode.NOTICE_ADMIN_PERMISSION_REQUIRED, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public NoticePermissionException(String message, Throwable cause) {
        super(NoticeErrorCode.NOTICE_ADMIN_PERMISSION_REQUIRED, message, cause);
    }
}