package com.fream.back.global.exception.file;
/**
 * 파일 찾을 수 없음 예외
 * 요청한 파일이 존재하지 않는 경우 발생
 */
public class FileNotFoundException extends FileException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "파일을 찾을 수 없습니다."
     */
    public FileNotFoundException() {
        super(FileErrorCode.FILE_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지 (예: 파일 경로)
     */
    public FileNotFoundException(String message) {
        super(FileErrorCode.FILE_NOT_FOUND, message);
    }
}
