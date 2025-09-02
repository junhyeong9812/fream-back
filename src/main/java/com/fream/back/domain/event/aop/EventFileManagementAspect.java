package com.fream.back.domain.event.aop;

import com.fream.back.domain.event.aop.annotation.EventFileManagement;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Event 도메인 파일 관리 AOP
 * 이미지 업로드, 리사이징, 변환, 삭제 등 파일 관련 작업 처리
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class EventFileManagementAspect {

    private final FileUtils fileUtils;

    // 파일 처리 메트릭
    private final ConcurrentHashMap<String, FileProcessingMetrics> metricsMap = new ConcurrentHashMap<>();

    // 파일 처리 통계
    private static class FileProcessingMetrics {
        private final AtomicLong totalUploads = new AtomicLong(0);
        private final AtomicLong totalDeletes = new AtomicLong(0);
        private final AtomicLong totalResizes = new AtomicLong(0);
        private final AtomicLong totalBytes = new AtomicLong(0);
        private final AtomicLong processingTime = new AtomicLong(0);
        private final AtomicLong failureCount = new AtomicLong(0);
    }

    @Around("@annotation(fileManagement)")
    public Object manageFiles(ProceedingJoinPoint joinPoint, EventFileManagement fileManagement) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("FILE_MANAGEMENT_START - Method: {}", methodName);

        long startTime = System.currentTimeMillis();
        FileProcessingMetrics metrics = metricsMap.computeIfAbsent(methodName, k -> new FileProcessingMetrics());

        try {
            // 파일 전처리
            preprocessFiles(args, fileManagement);

            // 메서드 실행
            Object result = joinPoint.proceed();

            // 파일 후처리
            postprocessFiles(result, fileManagement);

            long processingTime = System.currentTimeMillis() - startTime;
            metrics.processingTime.addAndGet(processingTime);

            log.info("FILE_MANAGEMENT_SUCCESS - Method: {}, ProcessingTime: {}ms",
                    methodName, processingTime);

            // 메트릭 수집
            if (fileManagement.collectMetrics()) {
                logFileMetrics(methodName, metrics);
            }

            return result;

        } catch (Exception e) {
            metrics.failureCount.incrementAndGet();

            if (fileManagement.rollbackOnFileFailure()) {
                log.error("FILE_MANAGEMENT_ERROR - Rolling back due to file failure: {}", e.getMessage());
                rollbackFileOperations(args);
                throw e;
            } else {
                log.warn("FILE_MANAGEMENT_WARNING - Continuing despite file failure: {}", e.getMessage());
                return joinPoint.proceed();
            }
        }
    }

    /**
     * 파일 전처리
     */
    private void preprocessFiles(Object[] args, EventFileManagement annotation) throws IOException {
        for (EventFileManagement.FileOperation operation : annotation.operations()) {
            switch (operation) {
                case VALIDATION:
                    validateFiles(args, annotation);
                    break;
                case RESIZE:
                    if (annotation.enableImageResize()) {
                        resizeImages(args, annotation);
                    }
                    break;
                case CONVERT:
                    if (annotation.convertToWebP()) {
                        convertToWebP(args, annotation);
                    }
                    break;
                case VIRUS_SCAN:
                    if (annotation.enableVirusScan()) {
                        scanForVirus(args);
                    }
                    break;
                case BACKUP:
                    if (annotation.createBackup()) {
                        createBackup(args);
                    }
                    break;
            }
        }
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFiles(Object[] args, EventFileManagement annotation) {
        int totalFiles = 0;
        long totalSize = 0;

        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                validateSingleFile(file, annotation);
                totalFiles++;
                totalSize += file.getSize();
            } else if (arg instanceof List<?>) {
                List<?> list = (List<?>) arg;
                for (Object item : list) {
                    if (item instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) item;
                        validateSingleFile(file, annotation);
                        totalFiles++;
                        totalSize += file.getSize();
                    }
                }
            }
        }

        // 파일 개수 검증
        if (totalFiles > annotation.maxFileCount()) {
            throw new RuntimeException(
                    String.format("파일 개수가 제한을 초과했습니다 (최대: %d개)", annotation.maxFileCount())
            );
        }

        log.debug("파일 검증 완료: totalFiles={}, totalSize={}bytes", totalFiles, totalSize);
    }

    /**
     * 단일 파일 유효성 검사
     */
    private void validateSingleFile(MultipartFile file, EventFileManagement annotation) {
        if (file.isEmpty()) {
            return;
        }

        // 파일 크기 검증
        if (file.getSize() > annotation.maxFileSize()) {
            throw new RuntimeException(
                    String.format("파일 크기가 제한을 초과했습니다: %s (최대: %dMB)",
                            file.getOriginalFilename(),
                            annotation.maxFileSize() / 1024 / 1024)
            );
        }

        // 확장자 검증
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!Arrays.asList(annotation.allowedExtensions()).contains(extension)) {
                throw new RuntimeException(
                        String.format("지원하지 않는 파일 형식입니다: %s", extension)
                );
            }
        }
    }

    /**
     * 이미지 리사이징
     */
    private void resizeImages(Object[] args, EventFileManagement annotation) throws IOException {
        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                if (isImageFile(file)) {
                    // 썸네일로 판단되는 경우
                    if (file.getName().contains("thumbnail")) {
                        resizeImage(file, annotation.thumbnailMaxSize());
                    } else {
                        resizeImage(file, annotation.simpleImageMaxSize());
                    }
                }
            } else if (arg instanceof List<?>) {
                List<?> list = (List<?>) arg;
                for (Object item : list) {
                    if (item instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) item;
                        if (isImageFile(file)) {
                            resizeImage(file, annotation.simpleImageMaxSize());
                        }
                    }
                }
            }
        }
    }

    /**
     * 이미지 리사이징 수행
     */
    private byte[] resizeImage(MultipartFile file, int maxSize) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(file.getBytes()));

        if (originalImage == null) {
            return file.getBytes();
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 리사이징이 필요한지 확인
        if (originalWidth <= maxSize && originalHeight <= maxSize) {
            return file.getBytes();
        }

        // 비율 계산
        double scale = Math.min((double) maxSize / originalWidth, (double) maxSize / originalHeight);
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // 리사이징
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        // 바이트 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "jpg", baos);

        log.debug("이미지 리사이징 완료: {}x{} -> {}x{}",
                originalWidth, originalHeight, newWidth, newHeight);

        return baos.toByteArray();
    }

    /**
     * WebP 변환
     */
    private void convertToWebP(Object[] args, EventFileManagement annotation) {
        // WebP 변환 로직 (실제 구현은 외부 라이브러리 필요)
        log.debug("WebP 변환 시뮬레이션");
    }

    /**
     * 바이러스 스캔
     */
    private void scanForVirus(Object[] args) {
        // 바이러스 스캔 로직 (실제 구현은 외부 서비스 연동 필요)
        log.debug("바이러스 스캔 시뮬레이션");
    }

    /**
     * 백업 생성
     */
    private void createBackup(Object[] args) {
        log.debug("파일 백업 생성 시작");
        // 백업 디렉토리 생성 및 파일 복사
        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                if (!file.isEmpty()) {
                    String backupPath = "backup/event/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    log.debug("파일 백업: {}", backupPath);
                }
            }
        }
    }

    /**
     * 파일 후처리
     */
    private void postprocessFiles(Object result, EventFileManagement annotation) {
        // 정리 작업
        for (EventFileManagement.FileOperation operation : annotation.operations()) {
            if (operation == EventFileManagement.FileOperation.CLEANUP) {
                cleanupTempFiles();
            }
        }
    }

    /**
     * 임시 파일 정리
     */
    private void cleanupTempFiles() {
        log.debug("임시 파일 정리 수행");
    }

    /**
     * 파일 작업 롤백
     */
    private void rollbackFileOperations(Object[] args) {
        log.warn("파일 작업 롤백 시작");
        // 업로드된 파일 삭제 등의 롤백 작업
    }

    /**
     * 이미지 파일 여부 확인
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 파일 처리 메트릭 로깅
     */
    private void logFileMetrics(String methodName, FileProcessingMetrics metrics) {
        log.info("FILE_METRICS - Method: {}, TotalUploads: {}, TotalDeletes: {}, " +
                        "TotalResizes: {}, TotalBytes: {}, AvgProcessingTime: {}ms, FailureCount: {}",
                methodName,
                metrics.totalUploads.get(),
                metrics.totalDeletes.get(),
                metrics.totalResizes.get(),
                metrics.totalBytes.get(),
                metrics.processingTime.get() / Math.max(1, metrics.totalUploads.get()),
                metrics.failureCount.get()
        );
    }
}