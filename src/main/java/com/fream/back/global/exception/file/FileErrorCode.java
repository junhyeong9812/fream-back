package com.fream.back.global.exception.file;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 처리 관련 에러 코드
 * 접두사 'F'로 시작하는 코드를 사용
 */
@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {
    /**
     * 파일 업로드 오류 (500)
     * 파일 업로드 처리 중 오류가 발생한 경우
     */
    FILE_UPLOAD_ERROR("F001", "파일 업로드 중 오류가 발생했습니다.", 500),

    /**
     * 파일 다운로드 오류 (500)
     * 파일 다운로드 처리 중 오류가 발생한 경우
     */
    FILE_DOWNLOAD_ERROR("F002", "파일 다운로드 중 오류가 발생했습니다.", 500),

    /**
     * 지원되지 않는 파일 형식 (400)
     * 업로드한 파일의 형식이 지원되지 않는 경우 (ex: jpg, png가 아닌 파일)
     */
    UNSUPPORTED_FILE_TYPE("F003", "지원되지 않는 파일 형식입니다.", 400),

    /**
     * 파일 찾을 수 없음 (404)
     * 요청한 파일이 존재하지 않는 경우
     */
    FILE_NOT_FOUND("F004", "파일을 찾을 수 없습니다.", 404),

    /**
     * 디렉토리 생성 오류 (500)
     * 파일 저장을 위한 디렉토리를 생성하지 못한 경우
     */
    DIRECTORY_CREATION_ERROR("F005", "디렉토리 생성 중 오류가 발생했습니다.", 500);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}