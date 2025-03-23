package com.fream.back.domain.notification.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 도메인에서 사용되는 에러 코드
 * 접두사 'N'으로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    // 알림 생성 관련 에러
    /**
     * 알림 생성 실패 (500)
     * 알림 생성 중 오류가 발생한 경우
     */
    NOTIFICATION_CREATION_FAILED("N001", "알림 생성에 실패했습니다.", 500),

    // 알림 조회 관련 에러
    /**
     * 알림을 찾을 수 없음 (404)
     * 요청한 알림이 존재하지 않는 경우
     */
    NOTIFICATION_NOT_FOUND("N002", "알림을 찾을 수 없습니다.", 404),

    /**
     * 알림 조회 실패 (500)
     * 알림 조회 중 오류가 발생한 경우
     */
    NOTIFICATION_RETRIEVAL_FAILED("N003", "알림 조회에 실패했습니다.", 500),

    // 알림 수정 관련 에러
    /**
     * 알림 수정 권한 없음 (403)
     * 알림 수정 권한이 없는 사용자가 수정을 시도한 경우
     */
    NOTIFICATION_UPDATE_UNAUTHORIZED("N004", "알림 수정 권한이 없습니다.", 403),

    /**
     * 알림 수정 실패 (500)
     * 알림 수정 중 오류가 발생한 경우
     */
    NOTIFICATION_UPDATE_FAILED("N005", "알림 수정에 실패했습니다.", 500),

    // 알림 삭제 관련 에러
    /**
     * 알림 삭제 권한 없음 (403)
     * 알림 삭제 권한이 없는 사용자가 삭제를 시도한 경우
     */
    NOTIFICATION_DELETE_UNAUTHORIZED("N006", "알림 삭제 권한이 없습니다.", 403),

    /**
     * 알림 삭제 실패 (500)
     * 알림 삭제 중 오류가 발생한 경우
     */
    NOTIFICATION_DELETE_FAILED("N007", "알림 삭제에 실패했습니다.", 500),

    // 알림 읽음 처리 관련 에러
    /**
     * 알림 읽음 처리 실패 (500)
     * 알림 읽음 처리 중 오류가 발생한 경우
     */
    NOTIFICATION_READ_FAILED("N008", "알림 읽음 처리에 실패했습니다.", 500),

    // 웹소켓 알림 관련 에러
    /**
     * 알림 전송 실패 (500)
     * 웹소켓을 통한 알림 전송 중 오류가 발생한 경우
     */
    NOTIFICATION_DELIVERY_FAILED("N009", "알림 전송에 실패했습니다.", 500),

    /**
     * 웹소켓 연결 유지 실패 (500)
     * 웹소켓 연결 상태 유지 중 오류가 발생한 경우
     */
    WEBSOCKET_CONNECTION_MAINTENANCE_FAILED("N010", "웹소켓 연결 유지에 실패했습니다.", 500),

    // 사용자 관련 알림 에러
    /**
     * 알림 대상 사용자를 찾을 수 없음 (404)
     * 알림을 전송하려는 사용자가 존재하지 않는 경우
     */
    NOTIFICATION_USER_NOT_FOUND("N011", "알림 대상 사용자를 찾을 수 없습니다.", 404),

    /**
     * 본인의 알림만 확인 가능 (403)
     * 다른 사용자의 알림을 조회하거나 수정하려는 경우
     */
    NOTIFICATION_ACCESS_DENIED("N012", "본인의 알림만 확인 가능합니다.", 403),

    // 기타 알림 관련 에러
    /**
     * 유효하지 않은 알림 유형 (400)
     * 지원하지 않는 알림 유형이 지정된 경우
     */
    INVALID_NOTIFICATION_TYPE("N020", "유효하지 않은 알림 유형입니다.", 400),

    /**
     * 유효하지 않은 알림 카테고리 (400)
     * 지원하지 않는 알림 카테고리가 지정된 경우
     */
    INVALID_NOTIFICATION_CATEGORY("N021", "유효하지 않은 알림 카테고리입니다.", 400);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}