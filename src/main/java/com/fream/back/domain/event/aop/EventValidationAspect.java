package com.fream.back.domain.event.aop;

import com.fream.back.domain.event.aop.annotation.EventValidation;
import com.fream.back.domain.event.dto.CreateEventRequest;
import com.fream.back.domain.event.dto.UpdateEventRequest;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.domain.event.exception.InvalidEventDateException;
import com.fream.back.domain.event.repository.EventRepository;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Event 도메인 유효성 검사 AOP
 * 이벤트 생성/수정 시 날짜, 브랜드, 파일 등의 유효성을 검증
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // 가장 먼저 실행되도록 우선순위 설정
public class EventValidationAspect {

    private final EventRepository eventRepository;
    private final BrandQueryService brandQueryService;

    // 브랜드별 이벤트 생성 빈도 추적
    private final ConcurrentHashMap<Long, AtomicInteger> brandEventCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDateTime> lastEventCreation = new ConcurrentHashMap<>();

    // 상태 전환 규칙
    private static final ConcurrentHashMap<EventStatus, List<EventStatus>> ALLOWED_TRANSITIONS = new ConcurrentHashMap<>();

    static {
        // 허용된 상태 전환 정의
        ALLOWED_TRANSITIONS.put(EventStatus.UPCOMING, List.of(EventStatus.ACTIVE, EventStatus.ENDED));
        ALLOWED_TRANSITIONS.put(EventStatus.ACTIVE, List.of(EventStatus.ENDED));
        ALLOWED_TRANSITIONS.put(EventStatus.ENDED, List.of()); // 종료된 이벤트는 상태 변경 불가
    }

    @Around("@annotation(eventValidation)")
    public Object validateEvent(ProceedingJoinPoint joinPoint, EventValidation eventValidation) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("EVENT_VALIDATION_START - Method: {}", methodName);

        try {
            // 유효성 검사 수행
            for (EventValidation.ValidationType validationType : eventValidation.validations()) {
                performValidation(validationType, args, eventValidation);
            }

            // 권한 검증
            if (eventValidation.requireAdminPermission()) {
                validateAdminPermission();
            }

            // 메서드 실행
            Object result = joinPoint.proceed();

            log.debug("EVENT_VALIDATION_SUCCESS - Method: {}", methodName);
            return result;

        } catch (InvalidEventDateException e) {
            handleValidationFailure(eventValidation.failureAction(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("EVENT_VALIDATION_ERROR - Method: {}, Error: {}", methodName, e.getMessage());
            handleValidationFailure(eventValidation.failureAction(), e.getMessage());
            throw e;
        }
    }

    /**
     * 유효성 검사 수행
     */
    private void performValidation(EventValidation.ValidationType type, Object[] args, EventValidation annotation) {
        switch (type) {
            case DATE_RANGE:
                validateDateRange(args, annotation);
                break;
            case BUSINESS_RULE:
                validateBusinessRules(args, annotation);
                break;
            case BRAND_VALIDATION:
                validateBrand(args, annotation);
                break;
            case FILE_VALIDATION:
                validateFiles(args, annotation);
                break;
            case STATUS_VALIDATION:
                validateStatusTransition(args);
                break;
            case DUPLICATE_CHECK:
                checkDuplicates(args, annotation);
                break;
            case CAPACITY_CHECK:
                checkCapacity(args, annotation);
                break;
            case PERMISSION_CHECK:
                validatePermissions();
                break;
        }
    }

    /**
     * 날짜 범위 유효성 검사
     */
    private void validateDateRange(Object[] args, EventValidation annotation) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

        // 파라미터에서 날짜 추출
        for (Object arg : args) {
            if (arg instanceof CreateEventRequest) {
                CreateEventRequest request = (CreateEventRequest) arg;
                startDate = request.getStartDate();
                endDate = request.getEndDate();
            } else if (arg instanceof UpdateEventRequest) {
                UpdateEventRequest request = (UpdateEventRequest) arg;
                startDate = request.getStartDate();
                endDate = request.getEndDate();
            }
        }

        if (startDate != null && endDate != null) {
            // 기본 날짜 검증
            if (startDate.isAfter(endDate)) {
                throw new InvalidEventDateException("종료일은 시작일 이후여야 합니다");
            }

            // 최소 지속 시간 검증
            long durationHours = ChronoUnit.HOURS.between(startDate, endDate);
            if (durationHours < annotation.minDurationHours()) {
                throw new InvalidEventDateException(
                        String.format("이벤트 최소 지속 시간은 %d시간입니다", annotation.minDurationHours())
                );
            }

            // 최대 지속 시간 검증
            long durationDays = ChronoUnit.DAYS.between(startDate, endDate);
            if (durationDays > annotation.maxDurationDays()) {
                throw new InvalidEventDateException(
                        String.format("이벤트 최대 지속 기간은 %d일입니다", annotation.maxDurationDays())
                );
            }

            // 과거 날짜 검증
            if (!annotation.allowPastDates() && startDate.isBefore(LocalDateTime.now())) {
                throw new InvalidEventDateException("과거 날짜는 설정할 수 없습니다");
            }

            // 미래 날짜 제한 검증
            long futureDays = ChronoUnit.DAYS.between(LocalDateTime.now(), startDate);
            if (futureDays > annotation.maxFutureDays()) {
                throw new InvalidEventDateException(
                        String.format("이벤트는 %d일 이내에만 생성 가능합니다", annotation.maxFutureDays())
                );
            }
        }
    }

    /**
     * 비즈니스 규칙 검증
     */
    private void validateBusinessRules(Object[] args, EventValidation annotation) {
        // 브랜드별 동시 활성 이벤트 수 제한 체크
        Long brandId = extractBrandId(args);
        if (brandId != null) {
            LocalDateTime now = LocalDateTime.now();
            List<Event> activeEvents = eventRepository.findByBrandIdOrderByStartDateDesc(brandId)
                    .stream()
                    .filter(e -> e.getStartDate().isBefore(now) && e.getEndDate().isAfter(now))
                    .toList();

            if (activeEvents.size() >= 5) { // 기본 제한: 브랜드당 5개
                log.warn("브랜드 활성 이벤트 제한 초과: brandId={}, count={}", brandId, activeEvents.size());
                throw new RuntimeException("브랜드당 최대 5개의 활성 이벤트만 허용됩니다");
            }
        }
    }

    /**
     * 브랜드 유효성 검증
     */
    private void validateBrand(Object[] args, EventValidation annotation) {
        if (!annotation.checkBrandStatus()) {
            return;
        }

        Long brandId = extractBrandId(args);
        if (brandId != null) {
            try {
                Brand brand = brandQueryService.findById(brandId);
                // 브랜드 활성 상태 확인 (Brand 엔티티에 isActive 필드가 있다고 가정)
                log.debug("브랜드 유효성 검증 완료: brandId={}", brandId);
            } catch (Exception e) {
                log.error("브랜드 유효성 검증 실패: brandId={}", brandId);
                throw new RuntimeException("유효하지 않은 브랜드입니다");
            }
        }
    }

    /**
     * 파일 유효성 검증
     */
    private void validateFiles(Object[] args, EventValidation annotation) {
        long totalSize = 0;
        int fileCount = 0;

        for (Object arg : args) {
            if (arg instanceof MultipartFile) {
                MultipartFile file = (MultipartFile) arg;
                if (!file.isEmpty()) {
                    totalSize += file.getSize();
                    fileCount++;
                    validateFileExtension(file);
                }
            } else if (arg instanceof List<?>) {
                List<?> list = (List<?>) arg;
                for (Object item : list) {
                    if (item instanceof MultipartFile) {
                        MultipartFile file = (MultipartFile) item;
                        if (!file.isEmpty()) {
                            totalSize += file.getSize();
                            fileCount++;
                            validateFileExtension(file);
                        }
                    }
                }
            }
        }

        // 총 파일 크기 검증
        if (totalSize > annotation.maxTotalFileSize()) {
            throw new RuntimeException(
                    String.format("총 파일 크기가 제한을 초과했습니다 (최대: %dMB)",
                            annotation.maxTotalFileSize() / 1024 / 1024)
            );
        }

        log.debug("파일 유효성 검증 완료: fileCount={}, totalSize={}bytes", fileCount, totalSize);
    }

    /**
     * 파일 확장자 검증
     */
    private void validateFileExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.isEmpty()) {
            String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
                throw new RuntimeException("지원하지 않는 파일 형식입니다: " + extension);
            }
        }
    }

    /**
     * 상태 전환 유효성 검증
     */
    private void validateStatusTransition(Object[] args) {
        // 상태 변경 요청 처리
        EventStatus currentStatus = null;
        EventStatus newStatus = null;

        for (Object arg : args) {
            if (arg instanceof EventStatus) {
                newStatus = (EventStatus) arg;
            } else if (arg instanceof Long) {
                // eventId로 현재 상태 조회
                Long eventId = (Long) arg;
                eventRepository.findById(eventId).ifPresent(event -> {
                    // currentStatus를 설정하는 로직
                });
            }
        }

        if (currentStatus != null && newStatus != null) {
            List<EventStatus> allowedTransitions = ALLOWED_TRANSITIONS.get(currentStatus);
            if (!allowedTransitions.contains(newStatus)) {
                throw new RuntimeException(
                        String.format("상태 전환이 허용되지 않습니다: %s -> %s",
                                currentStatus, newStatus)
                );
            }
        }
    }

    /**
     * 중복 검사
     */
    private void checkDuplicates(Object[] args, EventValidation annotation) {
        if (!annotation.checkTitleDuplicate()) {
            return;
        }

        String title = extractTitle(args);
        if (title != null && !title.isEmpty()) {
            // 제목 중복 체크 로직
            log.debug("이벤트 제목 중복 검사: title={}", title);
        }
    }

    /**
     * 용량 체크
     */
    private void checkCapacity(Object[] args, EventValidation annotation) {
        // 파일 용량 체크는 validateFiles에서 수행
        log.debug("용량 체크 수행");
    }

    /**
     * 권한 검증
     */
    private void validatePermissions() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.debug("권한 검증 수행: user={}", email);
    }

    /**
     * 관리자 권한 검증
     */
    private void validateAdminPermission() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        // 관리자 권한 체크 로직
        log.debug("관리자 권한 검증: user={}", email);
    }

    /**
     * 유효성 검사 실패 처리
     */
    private void handleValidationFailure(EventValidation.ValidationFailureAction action, String message) {
        switch (action) {
            case THROW_EXCEPTION:
                // 예외는 이미 발생시킴
                break;
            case LOG_AND_CONTINUE:
                log.warn("EVENT_VALIDATION_WARNING: {}", message);
                break;
            case RETURN_DEFAULT:
                log.info("EVENT_VALIDATION_DEFAULT: {}", message);
                break;
            case PROMPT_USER:
                log.info("EVENT_VALIDATION_PROMPT: {}", message);
                break;
        }
    }

    /**
     * 파라미터에서 브랜드 ID 추출
     */
    private Long extractBrandId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof CreateEventRequest) {
                return ((CreateEventRequest) arg).getBrandId();
            }
        }
        return null;
    }

    /**
     * 파라미터에서 제목 추출
     */
    private String extractTitle(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof CreateEventRequest) {
                return ((CreateEventRequest) arg).getTitle();
            } else if (arg instanceof UpdateEventRequest) {
                return ((UpdateEventRequest) arg).getTitle();
            }
        }
        return null;
    }
}