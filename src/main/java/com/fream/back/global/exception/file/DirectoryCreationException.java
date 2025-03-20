package com.fream.back.global.exception.file;
/**
 * 디렉토리 생성 예외
 * 파일 저장을 위한 디렉토리를 생성하지 못한 경우 발생
 */
public class DirectoryCreationException extends FileException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "디렉토리 생성 중 오류가 발생했습니다."
     */
    public DirectoryCreationException() {
        super(FileErrorCode.DIRECTORY_CREATION_ERROR);
    }

    /**
     * 원인 예외로 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public DirectoryCreationException(Throwable cause) {
        super(FileErrorCode.DIRECTORY_CREATION_ERROR, cause);
    }
}
