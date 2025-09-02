package com.fream.back.domain.event.aop.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Event 도메인 파일 관리 어노테이션
 * 이벤트 이미지 업로드, 삭제, 최적화 등의 파일 관리 제어
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface EventFileManagement {

    /**
     * 파일 관리 작업 타입
     */
    FileOperation[] operations() default {FileOperation.UPLOAD, FileOperation.VALIDATION};

    /**
     * 허용된 파일 확장자들
     */
    String[] allowedExtensions() default {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 최대 파일 크기 (바이트)
     */
    long maxFileSize() default 10485760; // 10MB

    /**
     * 최대 파일 개수
     */
    int maxFileCount() default 10;

    /**
     * 이미지 리사이징 여부
     */
    boolean enableImageResize() default true;

    /**
     * 썸네일 최대 크기 (픽셀)
     */
    int thumbnailMaxSize() default 800;

    /**
     * 심플 이미지 최대 크기 (픽셀)
     */
    int simpleImageMaxSize() default 1200;

    /**
     * WebP 변환 여부
     */
    boolean convertToWebP() default false;

    /**
     * 이미지 품질 (1-100)
     */
    int imageQuality() default 85;

    /**
     * 파일 업로드 실패 시 전체 작업 롤백 여부
     */
    boolean rollbackOnFileFailure() default true;

    /**
     * 중복 파일 처리 방식
     */
    DuplicateHandling duplicateHandling() default DuplicateHandling.RENAME;

    /**
     * 파일 삭제 시 확인 여부
     */
    boolean confirmBeforeDelete() default false;

    /**
     * 백업 생성 여부
     */
    boolean createBackup() default false;

    /**
     * 비동기 처리 여부
     */
    boolean asyncProcessing() default false;

    /**
     * 파일 처리 메트릭 수집 여부
     */
    boolean collectMetrics() default true;

    /**
     * 파일 바이러스 스캔 여부
     */
    boolean enableVirusScan() default false;

    /**
     * 파일 관리 작업 타입
     */
    enum FileOperation {
        UPLOAD,         // 파일 업로드
        DELETE,         // 파일 삭제
        VALIDATION,     // 파일 유효성 검사
        RESIZE,         // 이미지 리사이징
        CONVERT,        // 포맷 변환
        BACKUP,         // 백업 생성
        CLEANUP,        // 정리 작업
        VIRUS_SCAN      // 바이러스 스캔
    }

    /**
     * 중복 파일 처리 방식
     */
    enum DuplicateHandling {
        RENAME,         // 파일명 변경
        OVERWRITE,      // 덮어쓰기
        ERROR,          // 에러 발생
        SKIP            // 건너뛰기
    }
}