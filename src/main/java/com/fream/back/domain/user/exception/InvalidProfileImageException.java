package com.fream.back.domain.user.exception;

/**
 * 잘못된 프로필 이미지 형식일 때 발생하는 예외
 */
public class InvalidProfileImageException extends UserException {

    private final String fileName;
    private final String contentType;

    /**
     * 기본 생성자
     */
    public InvalidProfileImageException() {
        super(UserErrorCode.INVALID_PROFILE_IMAGE);
        this.fileName = null;
        this.contentType = null;
    }

    /**
     * 파일명과 컨텐츠 타입을 기반으로 한 생성자
     *
     * @param fileName 파일명
     * @param contentType 컨텐츠 타입
     */
    public InvalidProfileImageException(String fileName, String contentType) {
        super(UserErrorCode.INVALID_PROFILE_IMAGE,
                "잘못된 프로필 이미지 형식입니다. 파일명: " + fileName + ", 형식: " + contentType);
        this.fileName = fileName;
        this.contentType = contentType;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public InvalidProfileImageException(String message) {
        super(UserErrorCode.INVALID_PROFILE_IMAGE, message);
        this.fileName = null;
        this.contentType = null;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidProfileImageException(String message, Throwable cause) {
        super(UserErrorCode.INVALID_PROFILE_IMAGE, message, cause);
        this.fileName = null;
        this.contentType = null;
    }

    /**
     * 파일명 반환
     *
     * @return 파일명
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 컨텐츠 타입 반환
     *
     * @return 컨텐츠 타입
     */
    public String getContentType() {
        return contentType;
    }
}