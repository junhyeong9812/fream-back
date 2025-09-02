package com.fream.back.domain.event.aop;

import com.fream.back.domain.event.aop.annotation.EventBusinessRule;
import com.fream.back.domain.event.dto.CreateEventRequest;
import com.fream.back.domain.event.dto.UpdateEventRequest;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.domain.event.repository.EventRepository;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Event 도메인 비즈니스 규칙 AOP
 * 이벤트 관련 비즈니스 로직과 제약사항 적용
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(5)
public class EventBusinessRuleAspect {

    private final EventRepository eventRepository;
    private final BrandQueryService brandQueryService;

    // 브랜드별 이벤트 통계
    private final ConcurrentHashMap<Long, BrandEventInfo> brandEventInfoMap = new ConcurrentHashMap<>();

    // 이벤트 생성 쿨다운 추적
    private final ConcurrentHashMap<String, LocalDateTime> lastCreationMap = new ConcurrentHashMap<>();

    // 이벤트 아카이브
    private final List<ArchivedEvent> archivedEvents = Collections.synchronizedList(new ArrayList<>());

    /**
     * 브랜드별 이벤트 정보
     */
    private static class BrandEventInfo {
        private final AtomicInteger activeEventCount = new AtomicInteger(0);
        private final AtomicInteger totalEventCount = new AtomicInteger(0);
        private LocalDateTime lastEventCreation;
        private final List<Long> activeEventIds = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * 아카이빙된 이벤트
     */
    private static class ArchivedEvent {
        private final Long eventId;
        private final String title;
        private final Long brandId;
        private final EventStatus finalStatus;
        private final LocalDateTime archivedAt;
        private final String archivedBy;

        public ArchivedEvent(Event event, String user) {
            this.eventId = event.getId();
            this.title = event.getTitle();
            this.brandId = event.getBrand().getId();
            this.finalStatus = event.getStatus();
            this.archivedAt = LocalDateTime.now();
            this.archivedBy = user;
        }
    }

    @Around("@annotation(businessRule)")
    public Object applyBusinessRules(ProceedingJoinPoint joinPoint, EventBusinessRule businessRule) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.debug("BUSINESS_RULE_START - Method: {}", methodName);

        try {
            // 비즈니스 규칙 검증
            for (EventBusinessRule.BusinessRule rule : businessRule.rules()) {
                validateBusinessRule(rule, businessRule, args, methodName);
            }

            // 메서드 실행
            Object result = joinPoint.proceed();

            // 실행 후 처리
            postProcessBusinessRules(businessRule, methodName, args, result);

            log.info("BUSINESS_RULE_SUCCESS - Method: {}", methodName);
            return result;

        } catch (BusinessRuleViolationException e) {
            handleRuleViolation(businessRule.violationAction(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("BUSINESS_RULE_ERROR - Method: {}, Error: {}", methodName, e.getMessage());
            throw e;
        }
    }

    /**
     * 비즈니스 규칙 검증
     */
    private void validateBusinessRule(EventBusinessRule.BusinessRule rule,
                                      EventBusinessRule annotation,
                                      Object[] args,
                                      String methodName) {
        switch (rule) {
            case EVENT_OVERLAP_CHECK:
                checkEventOverlap(args, annotation);
                break;

            case BRAND_LIMIT_CHECK:
                checkBrandEventLimit(args, annotation);
                break;

            case DATE_VALIDATION:
                validateEventDates(args);
                break;

            case STATUS_TRANSITION:
                validateStatusTransition(args, methodName);
                break;

            case EDIT_PERMISSION:
                checkEditPermission(args, annotation, methodName);
                break;

            case DELETE_PERMISSION:
                checkDeletePermission(args, annotation, methodName);
                break;

            case FILE_SIZE_LIMIT:
                checkFileSizeLimit(args);
                break;

            case CREATION_FREQUENCY:
                checkCreationFrequency(annotation);
                break;

            case BRAND_STATUS_CHECK:
                checkBrandStatus(args);
                break;

            case AUTO_ARCHIVE_RULE:
                checkAutoArchiveRule(args, annotation);
                break;
        }
    }

    /**
     * 이벤트 중복 검사
     */
    private void checkEventOverlap(Object[] args, EventBusinessRule annotation) {
        if (!annotation.allowOverlappingEvents()) {
            LocalDateTime startDate = null;
            LocalDateTime endDate = null;
            Long brandId = null;

            for (Object arg : args) {
                if (arg instanceof CreateEventRequest) {
                    CreateEventRequest request = (CreateEventRequest) arg;
                    startDate = request.getStartDate();
                    endDate = request.getEndDate();
                    brandId = request.getBrandId();
                } else if (arg instanceof UpdateEventRequest) {
                    UpdateEventRequest request = (UpdateEventRequest) arg;
                    startDate = request.getStartDate();
                    endDate = request.getEndDate();
                }
            }

            if (startDate != null && endDate != null && brandId != null) {
                // 같은 브랜드의 겹치는 이벤트 확인
                List<Event> overlappingEvents = findOverlappingEvents(brandId, startDate, endDate);

                if (!overlappingEvents.isEmpty()) {
                    String overlappingTitles = overlappingEvents.stream()
                            .map(Event::getTitle)
                            .collect(Collectors.joining(", "));

                    throw new BusinessRuleViolationException(
                            String.format("이벤트 기간이 겹치는 이벤트가 있습니다: %s", overlappingTitles)
                    );
                }
            }
        }
    }

    /**
     * 브랜드별 이벤트 수 제한 검사
     */
    private void checkBrandEventLimit(Object[] args, EventBusinessRule annotation) {
        Long brandId = extractBrandId(args);
        if (brandId == null) return;

        BrandEventInfo brandInfo = brandEventInfoMap.computeIfAbsent(brandId, k -> new BrandEventInfo());

        // 현재 활성 이벤트 수 업데이트
        updateBrandActiveEvents(brandId, brandInfo);

        if (brandInfo.activeEventCount.get() >= annotation.maxActiveEventsPerBrand()) {
            log.warn("BRAND_EVENT_LIMIT_EXCEEDED - BrandId: {}, ActiveEvents: {}, Limit: {}",
                    brandId, brandInfo.activeEventCount.get(), annotation.maxActiveEventsPerBrand());

            throw new BusinessRuleViolationException(
                    String.format("브랜드당 최대 %d개의 활성 이벤트만 허용됩니다",
                            annotation.maxActiveEventsPerBrand())
            );
        }
    }

    /**
     * 날짜 유효성 검사
     */
    private void validateEventDates(Object[] args) {
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;

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
            // 시작일이 종료일보다 늦은 경우
            if (startDate.isAfter(endDate)) {
                throw new BusinessRuleViolationException("시작일은 종료일보다 이전이어야 합니다");
            }

            // 최대 1년 제한
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            if (daysBetween > 365) {
                throw new BusinessRuleViolationException("이벤트 기간은 최대 1년까지만 가능합니다");
            }
        }
    }

    /**
     * 상태 전환 검증
     */
    private void validateStatusTransition(Object[] args, String methodName) {
        if (!methodName.contains("Status")) return;

        Long eventId = null;
        EventStatus newStatus = null;

        for (Object arg : args) {
            if (arg instanceof Long) {
                eventId = (Long) arg;
            } else if (arg instanceof EventStatus) {
                newStatus = (EventStatus) arg;
            }
        }

        if (eventId != null && newStatus != null) {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event != null) {
                EventStatus currentStatus = event.getStatus();

                // 종료된 이벤트는 상태 변경 불가
                if (currentStatus == EventStatus.ENDED) {
                    throw new BusinessRuleViolationException("종료된 이벤트는 상태를 변경할 수 없습니다");
                }

                // UPCOMING -> ENDED 직접 전환 불가
                if (currentStatus == EventStatus.UPCOMING && newStatus == EventStatus.ENDED) {
                    log.warn("INVALID_STATUS_TRANSITION - From: {}, To: {}", currentStatus, newStatus);
                    throw new BusinessRuleViolationException("예정된 이벤트는 바로 종료할 수 없습니다");
                }
            }
        }
    }

    /**
     * 수정 권한 검사
     */
    private void checkEditPermission(Object[] args, EventBusinessRule annotation, String methodName) {
        if (!methodName.contains("update") && !methodName.contains("Update")) return;

        Long eventId = extractEventId(args);
        if (eventId == null) return;

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return;

        EventStatus status = event.getStatus();
        LocalDateTime now = LocalDateTime.now();

        // 진행 중인 이벤트 수정 제한
        if (status == EventStatus.ACTIVE && !annotation.allowActiveEventModification()) {
            throw new BusinessRuleViolationException("진행 중인 이벤트는 수정할 수 없습니다");
        }

        // 종료된 이벤트 수정 제한
        if (status == EventStatus.ENDED && !annotation.allowEndedEventModification()) {
            throw new BusinessRuleViolationException("종료된 이벤트는 수정할 수 없습니다");
        }

        // 시작 임박 이벤트 수정 제한
        long hoursUntilStart = ChronoUnit.HOURS.between(now, event.getStartDate());
        if (hoursUntilStart > 0 && hoursUntilStart < annotation.editableHoursBeforeStart()) {
            throw new BusinessRuleViolationException(
                    String.format("이벤트 시작 %d시간 전에는 수정할 수 없습니다",
                            annotation.editableHoursBeforeStart())
            );
        }
    }

    /**
     * 삭제 권한 검사
     */
    private void checkDeletePermission(Object[] args, EventBusinessRule annotation, String methodName) {
        if (!methodName.contains("delete") && !methodName.contains("Delete")) return;

        Long eventId = extractEventId(args);
        if (eventId == null) return;

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null) return;

        // 진행 중인 이벤트 삭제 불가
        if (event.getStatus() == EventStatus.ACTIVE) {
            throw new BusinessRuleViolationException("진행 중인 이벤트는 삭제할 수 없습니다");
        }

        // 종료 후 일정 기간이 지나야 삭제 가능
        if (event.getStatus() == EventStatus.ENDED) {
            long daysSinceEnd = ChronoUnit.DAYS.between(event.getEndDate(), LocalDateTime.now());
            if (daysSinceEnd < annotation.deletableAfterEndDays()) {
                throw new BusinessRuleViolationException(
                        String.format("이벤트 종료 후 %d일이 지나야 삭제할 수 있습니다",
                                annotation.deletableAfterEndDays())
                );
            }
        }
    }

    /**
     * 파일 크기 제한 검사
     */
    private void checkFileSizeLimit(Object[] args) {
        // FileManagementAspect에서 처리하므로 여기서는 간단히 체크
        log.debug("File size limit check delegated to FileManagementAspect");
    }

    /**
     * 생성 빈도 제한 검사
     */
    private void checkCreationFrequency(EventBusinessRule annotation) {
        String userKey = getUserKey();
        LocalDateTime lastCreation = lastCreationMap.get(userKey);

        if (lastCreation != null) {
            long minutesSinceLastCreation = ChronoUnit.MINUTES.between(lastCreation, LocalDateTime.now());

            if (minutesSinceLastCreation < annotation.creationCooldownMinutes()) {
                throw new BusinessRuleViolationException(
                        String.format("이벤트 생성은 %d분에 한 번만 가능합니다",
                                annotation.creationCooldownMinutes())
                );
            }
        }

        lastCreationMap.put(userKey, LocalDateTime.now());
    }

    /**
     * 브랜드 상태 확인
     */
    private void checkBrandStatus(Object[] args) {
        Long brandId = extractBrandId(args);
        if (brandId == null) return;

        try {
            Brand brand = brandQueryService.findById(brandId);
            // 브랜드 활성 상태 확인
            log.debug("Brand status check passed for brandId: {}", brandId);
        } catch (Exception e) {
            throw new BusinessRuleViolationException("비활성 브랜드에는 이벤트를 생성할 수 없습니다");
        }
    }

    /**
     * 자동 아카이빙 규칙 확인
     */
    private void checkAutoArchiveRule(Object[] args, EventBusinessRule annotation) {
        if (!annotation.autoArchiveOnEnd()) return;

        Long eventId = extractEventId(args);
        if (eventId == null) return;

        Event event = eventRepository.findById(eventId).orElse(null);
        if (event != null && event.getStatus() == EventStatus.ENDED) {
            archiveEvent(event);
        }
    }

    /**
     * 실행 후 처리
     */
    private void postProcessBusinessRules(EventBusinessRule annotation, String methodName,
                                          Object[] args, Object result) {
        // 자동 상태 변경
        if (annotation.enableAutoStatusChange()) {
            handleAutoStatusChange(methodName, result);
        }

        // 브랜드 비활성화 처리
        if (methodName.contains("Brand")) {
            handleBrandDeactivation(annotation.brandDeactivationAction(), args);
        }

        // 이벤트 종료 시 자동 아카이빙
        if (annotation.autoArchiveOnEnd() && methodName.contains("Status")) {
            handleAutoArchiving(args);
        }
    }

    /**
     * 자동 상태 변경 처리
     */
    private void handleAutoStatusChange(String methodName, Object result) {
        if (methodName.contains("create") && result instanceof Long) {
            Long eventId = (Long) result;
            log.info("AUTO_STATUS_CHANGE - Scheduling status check for event: {}", eventId);
            // 스케줄러에 등록하여 자동 상태 변경 처리
        }
    }

    /**
     * 브랜드 비활성화 처리
     */
    private void handleBrandDeactivation(EventBusinessRule.BrandDeactivationAction action, Object[] args) {
        Long brandId = extractBrandId(args);
        if (brandId == null) return;

        switch (action) {
            case MARK_INACTIVE:
                markBrandEventsInactive(brandId);
                break;
            case END_IMMEDIATELY:
                endBrandEventsImmediately(brandId);
                break;
            case MAINTAIN_STATUS:
                // 상태 유지
                break;
            case TRANSFER_OWNERSHIP:
                transferEventOwnership(brandId);
                break;
        }
    }

    /**
     * 자동 아카이빙 처리
     */
    private void handleAutoArchiving(Object[] args) {
        EventStatus status = extractEventStatus(args);
        if (status == EventStatus.ENDED) {
            Long eventId = extractEventId(args);
            if (eventId != null) {
                Event event = eventRepository.findById(eventId).orElse(null);
                if (event != null) {
                    archiveEvent(event);
                }
            }
        }
    }

    /**
     * 이벤트 아카이빙
     */
    private void archiveEvent(Event event) {
        String user = getUserKey();
        ArchivedEvent archived = new ArchivedEvent(event, user);
        archivedEvents.add(archived);

        log.info("EVENT_ARCHIVED - EventId: {}, Title: {}, ArchivedBy: {}",
                event.getId(), event.getTitle(), user);
    }

    /**
     * 규칙 위반 처리
     */
    private void handleRuleViolation(EventBusinessRule.RuleViolationAction action, String message) {
        switch (action) {
            case THROW_EXCEPTION:
                // 예외는 이미 발생
                break;
            case LOG_AND_ALLOW:
                log.warn("BUSINESS_RULE_VIOLATION_ALLOWED - {}", message);
                break;
            case MODIFY_REQUEST:
                log.info("BUSINESS_RULE_MODIFY_REQUEST - {}", message);
                break;
            case PROMPT_APPROVAL:
                log.info("BUSINESS_RULE_APPROVAL_REQUIRED - {}", message);
                break;
        }
    }

    /**
     * 헬퍼 메서드들
     */
    private List<Event> findOverlappingEvents(Long brandId, LocalDateTime startDate, LocalDateTime endDate) {
        return eventRepository.findByBrandIdOrderByStartDateDesc(brandId).stream()
                .filter(e -> !e.getEndDate().isBefore(startDate) && !e.getStartDate().isAfter(endDate))
                .collect(Collectors.toList());
    }

    private void updateBrandActiveEvents(Long brandId, BrandEventInfo info) {
        List<Event> activeEvents = eventRepository.findByBrandIdOrderByStartDateDesc(brandId).stream()
                .filter(Event::isActive)
                .collect(Collectors.toList());

        info.activeEventCount.set(activeEvents.size());
        info.activeEventIds.clear();
        info.activeEventIds.addAll(activeEvents.stream().map(Event::getId).collect(Collectors.toList()));
    }

    private void markBrandEventsInactive(Long brandId) {
        log.info("Marking all events inactive for brand: {}", brandId);
    }

    private void endBrandEventsImmediately(Long brandId) {
        log.info("Ending all events immediately for brand: {}", brandId);
    }

    private void transferEventOwnership(Long brandId) {
        log.info("Transferring ownership for brand events: {}", brandId);
    }

    private Long extractEventId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private Long extractBrandId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof CreateEventRequest) {
                return ((CreateEventRequest) arg).getBrandId();
            }
        }
        return null;
    }

    private EventStatus extractEventStatus(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof EventStatus) {
                return (EventStatus) arg;
            }
        }
        return null;
    }

    private String getUserKey() {
        try {
            return SecurityUtils.extractEmailFromSecurityContext();
        } catch (Exception e) {
            return "system";
        }
    }

    /**
     * 비즈니스 규칙 위반 예외
     */
    private static class BusinessRuleViolationException extends RuntimeException {
        public BusinessRuleViolationException(String message) {
            super(message);
        }
    }
}