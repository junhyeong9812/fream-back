package com.fream.back.global.exception.file;

import com.fream.back.global.exception.GlobalException;

/**
 * 파일 처리 관련 예외의 기본 클래스
 * 모든 파일 관련 예외는 이 클래스를 상속받음
 */
public abstract class FileException extends GlobalException {
    /**
     * FileErrorCode로 예외 생성
     *
     * @param errorCode 파일 관련 에러 코드
     */
    public FileException(FileErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * FileErrorCode와 사용자 정의 메시지로 예외 생성
     *
     * @param errorCode 파일 관련 에러 코드
     * @param message 사용자 정의 에러 메시지
     */
    public FileException(FileErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * FileErrorCode와 원인 예외로 예외 생성
     *
     * @param errorCode 파일 관련 에러 코드
     * @param cause 원인이 되는 예외
     */
    public FileException(FileErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}