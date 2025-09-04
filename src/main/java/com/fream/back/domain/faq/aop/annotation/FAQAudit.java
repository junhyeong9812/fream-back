package com.fream.back.domain.faq.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * FAQ 감사 및 모니터링 어노테이션
 * 생성/수정/삭제 이력, 조회 통계, 관리자 활동 추적
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FAQAudit {

    /**
     * 감사 활성화
     */
    boolean enabled() default true;

    /**
     * 감사 이벤트
     */
    AuditEvent event() default AuditEvent.UNKNOWN;

    /**
     * 감사 레벨
     */
    AuditLevel level() default AuditLevel.INFO;

    /**
     * 상세 로깅
     */
    boolean detailed() default false;

    /**
     * 사용자 정보 기록
     */
    boolean recordUser() default true;

    /**
     * IP 주소 기록
     */
    boolean recordIpAddress() default false;

    /**
     * 요청 파라미터 기록
     */
    boolean recordParameters() default true;

    /**
     * 응답 결과 기록
     */
    boolean recordResult() default false;

    /**
     * 실행 시간 기록
     */
    boolean recordExecutionTime() default true;

    /**
     * 조회수 추적
     */
    boolean trackViewCount() default false;

    /**
     * 관리자 활동 추적
     */
    boolean trackAdminActivity() default true;

    /**
     * 카테고리별 통계 수집
     */
    boolean collectCategoryStats() default true;

    /**
     * 중요 이벤트 알림
     */
    boolean alertOnCriticalEvents() default false;

    /**
     * 데이터베이스 저장
     */
    boolean persistToDatabase() default true;

    /**
     * 외부 시스템 전송
     */
    boolean sendToExternalSystem() default false;

    /**
     * 외부 시스템 URL
     */
    String externalSystemUrl() default "";

    /**
     * 감사 이벤트 타입
     */
    enum AuditEvent {
        UNKNOWN,            // 알 수 없음
        FAQ_CREATED,        // FAQ 생성
        FAQ_UPDATED,        // FAQ 수정
        FAQ_DELETED,        // FAQ 삭제
        FAQ_VIEWED,         // FAQ 조회
        FAQ_SEARCHED,       // FAQ 검색
        FILE_UPLOADED,      // 파일 업로드
        FILE_DELETED,       // 파일 삭제
        CATEGORY_CHANGED,   // 카테고리 변경
        BULK_OPERATION,     // 대량 작업
        EXPORT,            // 내보내기
        IMPORT,            // 가져오기
        ADMIN_ACCESS       // 관리자 접근
    }

    /**
     * 감사 레벨
     */
    enum AuditLevel {
        DEBUG,      // 디버그
        INFO,       // 정보
        WARN,       // 경고
        ERROR,      // 오류
        CRITICAL    // 치명적
    }

    /**
     * 보존 기간 (일)
     */
    int retentionDays() default 90;

    /**
     * 민감한 데이터 마스킹
     */
    boolean maskSensitiveData() default true;

    /**
     * 실시간 모니터링
     */
    boolean realTimeMonitoring() default false;
}