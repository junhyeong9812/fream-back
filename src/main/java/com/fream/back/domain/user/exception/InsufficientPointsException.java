package com.fream.back.domain.user.exception;

/**
 * 포인트가 부족할 때 발생하는 예외
 */
public class InsufficientPointsException extends UserException {

    private final Integer requiredPoints;
    private final Integer availablePoints;

    /**
     * 기본 생성자
     */
    public InsufficientPointsException() {
        super(UserErrorCode.INSUFFICIENT_POINTS);
        this.requiredPoints = null;
        this.availablePoints = null;
    }

    /**
     * 필요 포인트와 보유 포인트를 기반으로 한 생성자
     *
     * @param requiredPoints 필요한 포인트
     * @param availablePoints 보유 포인트
     */
    public InsufficientPointsException(Integer requiredPoints, Integer availablePoints) {
        super(UserErrorCode.INSUFFICIENT_POINTS,
                String.format("포인트가 부족합니다. 필요: %d점, 보유: %d점", requiredPoints, availablePoints));
        this.requiredPoints = requiredPoints;
        this.availablePoints = availablePoints;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public InsufficientPointsException(String message) {
        super(UserErrorCode.INSUFFICIENT_POINTS, message);
        this.requiredPoints = null;
        this.availablePoints = null;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InsufficientPointsException(String message, Throwable cause) {
        super(UserErrorCode.INSUFFICIENT_POINTS, message, cause);
        this.requiredPoints = null;
        this.availablePoints = null;
    }

    /**
     * 필요한 포인트 반환
     *
     * @return 필요한 포인트
     */
    public Integer getRequiredPoints() {
        return requiredPoints;
    }

    /**
     * 보유 포인트 반환
     *
     * @return 보유 포인트
     */
    public Integer getAvailablePoints() {
        return availablePoints;
    }

    /**
     * 부족한 포인트 계산
     *
     * @return 부족한 포인트 (필요 포인트 - 보유 포인트)
     */
    public Integer getShortfallPoints() {
        if (requiredPoints != null && availablePoints != null) {
            return requiredPoints - availablePoints;
        }
        return null;
    }
}