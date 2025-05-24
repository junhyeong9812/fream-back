package com.fream.back.domain.user.exception;

/**
 * 포인트 내역을 찾을 수 없을 때 발생하는 예외
 */
public class PointHistoryNotFoundException extends UserException {

    private final Long pointId;

    /**
     * 기본 생성자
     */
    public PointHistoryNotFoundException() {
        super(UserErrorCode.POINT_HISTORY_NOT_FOUND);
        this.pointId = null;
    }

    /**
     * 포인트 ID를 기반으로 한 생성자
     *
     * @param pointId 찾을 수 없는 포인트 ID
     */
    public PointHistoryNotFoundException(Long pointId) {
        super(UserErrorCode.POINT_HISTORY_NOT_FOUND, "포인트 내역을 찾을 수 없습니다. ID: " + pointId);
        this.pointId = pointId;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public PointHistoryNotFoundException(String message) {
        super(UserErrorCode.POINT_HISTORY_NOT_FOUND, message);
        this.pointId = null;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public PointHistoryNotFoundException(String message, Throwable cause) {
        super(UserErrorCode.POINT_HISTORY_NOT_FOUND, message, cause);
        this.pointId = null;
    }

    /**
     * 포인트 ID 반환
     *
     * @return 포인트 ID
     */
    public Long getPointId() {
        return pointId;
    }
}