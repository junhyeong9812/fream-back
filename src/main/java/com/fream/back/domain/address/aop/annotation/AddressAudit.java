package com.fream.back.domain.address.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Address 도메인 감사 추적 어노테이션
 * 중요한 비즈니스 로직에 적용하여 감사 로그를 남김
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AddressAudit {

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
     * 민감한 데이터 포함 여부
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
     * 비즈니스 컨텍스트 정보 포함 여부
     */
    boolean includeBusinessContext() default true;

    /**
     * 변경 전후 데이터 비교 여부
     */
    boolean trackChanges() default false;

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
     * 감사 이벤트 타입 열거형
     */
    enum AuditEvent {
        ADDRESS_CREATE("주소 생성"),
        ADDRESS_UPDATE("주소 수정"),
        ADDRESS_DELETE("주소 삭제"),
        ADDRESS_VIEW("주소 조회"),
        ADDRESS_LIST_VIEW("주소 목록 조회"),
        ADDRESS_SEARCH("주소 검색"),
        DEFAULT_ADDRESS_CHANGE("기본 주소 변경"),
        BULK_ADDRESS_OPERATION("주소 일괄 처리"),
        ADDRESS_VALIDATION("주소 유효성 검증"),
        ADDRESS_ENCRYPTION("주소 암호화"),
        ADDRESS_DECRYPTION("주소 복호화"),
        SECURITY_VIOLATION("보안 위반"),
        UNAUTHORIZED_ACCESS("무권한 접근"),
        DATA_EXPORT("데이터 내보내기"),
        DATA_IMPORT("데이터 가져오기");

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
        COMPLIANCE
    }
}