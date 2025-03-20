package com.fream.back.global.exception.file;
/**
 * 파일 다운로드 예외
 * 파일 다운로드 처리 중 오류가 발생한 경우 발생
 */
public class FileDownloadException extends FileException {
    /**
     * 기본 생성자
     * 기본 에러 메시지: "파일 다운로드 중 오류가 발생했습니다."
     */
    public FileDownloadException() {
        super(FileErrorCode.FILE_DOWNLOAD_ERROR);
    }

    /**
     * 원인 예외로 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public FileDownloadException(Throwable cause) {
        super(FileErrorCode.FILE_DOWNLOAD_ERROR, cause);
    }
}
