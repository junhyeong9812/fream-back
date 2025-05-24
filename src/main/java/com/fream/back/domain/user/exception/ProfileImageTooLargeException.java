package com.fream.back.domain.user.exception;

/**
 * 프로필 이미지 크기가 너무 클 때 발생하는 예외
 */
public class ProfileImageTooLargeException extends UserException {

    private final long fileSize;
    private final long maxSize;

    /**
     * 기본 생성자
     */
    public ProfileImageTooLargeException() {
        super(UserErrorCode.PROFILE_IMAGE_TOO_LARGE);
        this.fileSize = 0;
        this.maxSize = 0;
    }

    /**
     * 파일 크기와 최대 크기를 기반으로 한 생성자
     *
     * @param fileSize 현재 파일 크기 (bytes)
     * @param maxSize 최대 허용 크기 (bytes)
     */
    public ProfileImageTooLargeException(long fileSize, long maxSize) {
        super(UserErrorCode.PROFILE_IMAGE_TOO_LARGE,
                "프로필 이미지 크기가 너무 큽니다. 현재: " + formatFileSize(fileSize) + ", 최대: " + formatFileSize(maxSize));
        this.fileSize = fileSize;
        this.maxSize = maxSize;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public ProfileImageTooLargeException(String message) {
        super(UserErrorCode.PROFILE_IMAGE_TOO_LARGE, message);
        this.fileSize = 0;
        this.maxSize = 0;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public ProfileImageTooLargeException(String message, Throwable cause) {
        super(UserErrorCode.PROFILE_IMAGE_TOO_LARGE, message, cause);
        this.fileSize = 0;
        this.maxSize = 0;
    }

    /**
     * 현재 파일 크기 반환
     *
     * @return 파일 크기 (bytes)
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * 최대 허용 크기 반환
     *
     * @return 최대 크기 (bytes)
     */
    public long getMaxSize() {
        return maxSize;
    }

    /**
     * 파일 크기를 읽기 쉬운 형태로 포맷
     *
     * @param bytes 바이트 크기
     * @return 포맷된 크기 문자열
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " bytes";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}