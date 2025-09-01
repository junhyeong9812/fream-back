package com.fream.back.domain.chatQuestion.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ChatQuestion 도메인 감사 추적 어노테이션
 * GPT 사용량, 질문-답변 이력, 비용 등의 감사 로그를 남김
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ChatAudit {

    /**
     * 감사 이벤트 타입
     */
    AuditEvent event();

    /**
     * 감사 로그 설명
     */
    String description() default "";

    /**
     * 감사 로그 레벨
     */
    AuditLevel level() default AuditLevel.INFO;

    /**
     * 민감한 데이터 포함 여부 (질문 내용 등)
     */
    boolean containsSensitiveData() default true;

    /**
     * 사용자 IP 주소 기록 여부
     */
    boolean recordIpAddress() default true;

    /**
     * 사용자 에이전트 기록 여부
     */
    boolean recordUserAgent() default false;

    /**
     * 요청 헤더 기록 여부
     */
    boolean recordHeaders() default false;

    /**
     * GPT 토큰 사용량 기록 여부
     */
    boolean recordTokenUsage() default true;

    /**
     * GPT 모델 정보 기록 여부
     */
    boolean recordModelInfo() default true;

    /**
     * 예상 비용 계산 및 기록 여부
     */
    boolean calculateCost() default true;

    /**
     * 질문-답변 품질 평가 기록 여부
     */
    boolean recordQualityMetrics() default false;

    /**
     * 응답 시간 기록 여부
     */
    boolean recordResponseTime() default true;

    /**
     * 외부 시스템 연동 여부
     */
    boolean sendToExternalSystem() default false;

    /**
     * 실시간 알림 여부
     */
    boolean realTimeAlert() default false;

    /**
     * 감사 로그 보존 기간 (일 단위, -1이면 무제한)
     */
    int retentionDays() default 365;

    /**
     * 비용 임계값 (센트 단위) - 초과 시 알림
     */
    int costThreshold() default 100;

    /**
     * 감사 이벤트 타입 열거형
     */
    enum AuditEvent {
        CHAT_QUESTION_SUBMITTED("채팅 질문 제출"),
        CHAT_QUESTION_PROCESSED("채팅 질문 처리 완료"),
        GPT_API_CALLED("GPT API 호출"),
        GPT_API_RESPONSE_RECEIVED("GPT API 응답 수신"),
        TOKEN_USAGE_RECORDED("토큰 사용량 기록"),
        USAGE_STATS_ACCESSED("사용량 통계 조회"),
        CHAT_HISTORY_ACCESSED("채팅 기록 조회"),
        ADMIN_ACCESS("관리자 접근"),
        HIGH_TOKEN_USAGE("높은 토큰 사용량"),
        HIGH_COST_INCURRED("높은 비용 발생"),
        API_RATE_LIMIT_HIT("API 속도 제한 도달"),
        SUSPICIOUS_ACTIVITY("의심스러운 활동"),
        DATA_EXPORT("데이터 내보내기"),
        DATA_IMPORT("데이터 가져오기"),
        SYSTEM_MAINTENANCE("시스템 유지보수");

        private final String description;

        AuditEvent(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 감사 레벨 열거형
     */
    enum AuditLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR,
        SECURITY,
        COMPLIANCE,
        FINANCIAL    // 비용 관련 감사
    }
}