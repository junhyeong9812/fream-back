package com.fream.back.global.exception.file;
/**
 * 지원되지 않는 파일 형식 예외
 * 업로드한 파일의 형식이 지원되지 않는 경우 발생
 */
public class UnsupportedFileTypeException extends FileException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "지원되지 않는 파일 형식입니다."
     */
    public UnsupportedFileTypeException() {
        super(FileErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    /**
     * 사용자 정의 메시지로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public UnsupportedFileTypeException(String message) {
        super(FileErrorCode.UNSUPPORTED_FILE_TYPE, message);
    }
}
