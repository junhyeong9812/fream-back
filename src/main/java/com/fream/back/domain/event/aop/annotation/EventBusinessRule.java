package com.fream.back.domain.event.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event 도메인 비즈니스 규칙 어노테이션
 * 이벤트 관련 비즈니스 로직과 제약사항 적용
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventBusinessRule {

    /**
     * 적용할 비즈니스 규칙들
     */
    BusinessRule[] rules() default {BusinessRule.EVENT_OVERLAP_CHECK, BusinessRule.BRAND_LIMIT_CHECK};

    /**
     * 브랜드당 동시 활성 이벤트 최대 개수
     */
    int maxActiveEventsPerBrand() default 5;

    /**
     * 동일 기간 이벤트 중복 허용 여부
     */
    boolean allowOverlappingEvents() default true;

    /**
     * 이벤트 생성 쿨다운 시간 (분)
     */
    int creationCooldownMinutes() default 5;

    /**
     * 이벤트 수정 가능 기간 (시작일 기준, 시간 단위)
     */
    int editableHoursBeforeStart() default 24;

    /**
     * 진행 중인 이벤트 수정 허용 여부
     */
    boolean allowActiveEventModification() default false;

    /**
     * 종료된 이벤트 수정 허용 여부
     */
    boolean allowEndedEventModification() default false;

    /**
     * 이벤트 삭제 가능 기간 (종료일 기준, 일 단위)
     */
    int deletableAfterEndDays() default 30;

    /**
     * 자동 상태 변경 활성화 여부
     */
    boolean enableAutoStatusChange() default true;

    /**
     * 이벤트 종료 시 자동 아카이빙 여부
     */
    boolean autoArchiveOnEnd() default false;

    /**
     * 브랜드 비활성화 시 이벤트 처리 방식
     */
    BrandDeactivationAction brandDeactivationAction() default BrandDeactivationAction.MARK_INACTIVE;

    /**
     * 규칙 위반 시 처리 방식
     */
    RuleViolationAction violationAction() default RuleViolationAction.THROW_EXCEPTION;

    /**
     * 비즈니스 규칙 타입
     */
    enum BusinessRule {
        EVENT_OVERLAP_CHECK,        // 이벤트 중복 검사
        BRAND_LIMIT_CHECK,          // 브랜드당 이벤트 수 제한
        DATE_VALIDATION,            // 날짜 유효성 검사
        STATUS_TRANSITION,          // 상태 전환 규칙
        EDIT_PERMISSION,            // 수정 권한 검사
        DELETE_PERMISSION,          // 삭제 권한 검사
        FILE_SIZE_LIMIT,            // 파일 크기 제한
        CREATION_FREQUENCY,         // 생성 빈도 제한
        BRAND_STATUS_CHECK,         // 브랜드 상태 확인
        AUTO_ARCHIVE_RULE           // 자동 아카이빙 규칙
    }

    /**
     * 브랜드 비활성화 시 처리 방식
     */
    enum BrandDeactivationAction {
        MARK_INACTIVE,      // 이벤트를 비활성으로 표시
        END_IMMEDIATELY,    // 즉시 종료
        MAINTAIN_STATUS,    // 상태 유지
        TRANSFER_OWNERSHIP  // 소유권 이전
    }

    /**
     * 규칙 위반 처리 방식
     */
    enum RuleViolationAction {
        THROW_EXCEPTION,    // 예외 발생
        LOG_AND_ALLOW,      // 로그 남기고 허용
        MODIFY_REQUEST,     // 요청 수정
        PROMPT_APPROVAL     // 승인 요청
    }
}