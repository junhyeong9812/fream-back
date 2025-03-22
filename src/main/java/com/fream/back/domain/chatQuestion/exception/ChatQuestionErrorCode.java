package com.fream.back.domain.chatQuestion.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 채팅 질문 도메인에서 사용하는 에러 코드
 * 접두사 'CQ'로 시작하는 코드를 사용합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ChatQuestionErrorCode implements ErrorCode {

    // API 관련 에러
    /**
     * GPT API 호출 실패 (500)
     * 외부 GPT API 서버 연결 또는 응답 처리 중 오류 발생
     */
    GPT_API_ERROR("CQ001", "GPT API 호출 중 오류가 발생했습니다.", 500),

    /**
     * GPT API 응답 처리 오류 (500)
     * GPT API 응답을 처리하는 과정에서 발생하는 오류
     */
    GPT_RESPONSE_PROCESSING_ERROR("CQ002", "GPT API 응답 처리 중 오류가 발생했습니다.", 500),

    /**
     * GPT 사용량 기록 실패 (500)
     * GPT 사용량을 기록하는 과정에서 발생하는 오류
     */
    GPT_USAGE_LOG_ERROR("CQ003", "GPT 사용량 기록 중 오류가 발생했습니다.", 500),

    // 데이터 관련 에러
    /**
     * 채팅 질문 저장 실패 (500)
     * 채팅 질문을 데이터베이스에 저장하는 과정에서 발생하는 오류
     */
    CHAT_QUESTION_SAVE_ERROR("CQ101", "채팅 질문 저장 중 오류가 발생했습니다.", 500),

    /**
     * 채팅 기록 조회 실패 (500)
     * 채팅 기록을 조회하는 과정에서 발생하는 오류
     */
    CHAT_HISTORY_QUERY_ERROR("CQ102", "채팅 기록 조회 중 오류가 발생했습니다.", 500),

    /**
     * 사용량 통계 조회 실패 (500)
     * GPT 사용량 통계를 조회하는 과정에서 발생하는 오류
     */
    USAGE_STATS_QUERY_ERROR("CQ103", "사용량 통계 조회 중 오류가 발생했습니다.", 500),

    // 권한 관련 에러
    /**
     * 관리자 권한 필요 (403)
     * 관리자 권한이 필요한 작업을 수행할 때 권한이 없는 경우
     */
    ADMIN_PERMISSION_REQUIRED("CQ201", "관리자 권한이 필요합니다.", 403),

    /**
     * 질문 권한 없음 (403)
     * 채팅 질문을 할 수 있는 권한이 없는 경우
     */
    QUESTION_PERMISSION_DENIED("CQ202", "질문 권한이 없습니다.", 403),

    // 파라미터 관련 에러
    /**
     * 유효하지 않은 질문 데이터 (400)
     * 질문 데이터가 유효하지 않은 경우
     */
    INVALID_QUESTION_DATA("CQ301", "유효하지 않은 질문 데이터입니다.", 400),

    /**
     * 유효하지 않은 날짜 범위 (400)
     * 통계 조회 시 유효하지 않은 날짜 범위가 지정된 경우
     */
    INVALID_DATE_RANGE("CQ302", "유효하지 않은 날짜 범위입니다.", 400),

    /**
     * 질문 길이 초과 (400)
     * 질문 길이가 제한을 초과한 경우
     */
    QUESTION_LENGTH_EXCEEDED("CQ303", "질문 길이가 제한을 초과했습니다.", 400),

    /**
     * GPT 사용량 한도 초과 (429)
     * 사용자 또는 시스템의 GPT 사용량이 한도를 초과한 경우
     */
    GPT_USAGE_LIMIT_EXCEEDED("CQ304", "GPT 사용량 한도를 초과했습니다.", 429);

    private final String code;      // 에러 코드
    private final String message;   // 에러 메시지
    private final int status;       // HTTP 상태 코드
}