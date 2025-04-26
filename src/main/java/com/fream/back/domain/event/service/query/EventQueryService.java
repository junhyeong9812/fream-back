package com.fream.back.domain.event.service.query;

import com.fream.back.domain.event.dto.EventDetailDto;
import com.fream.back.domain.event.dto.EventListDto;
import com.fream.back.domain.event.dto.EventSearchCondition;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.domain.event.entity.SimpleImage;
import com.fream.back.domain.event.exception.EventNotFoundException;
import com.fream.back.domain.event.repository.EventRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventQueryService {

    private final EventRepository eventRepository;
    private final SimpleImageQueryService simpleImageQueryService;
    private final FileUtils fileUtils;

    /**
     * 이벤트 상세 조회 - 캐싱 적용
     */
    @Cacheable(value = "eventDetails", key = "#eventId")
    public EventDetailDto findEventById(Long eventId) {
        log.debug("이벤트 상세 조회: eventId={}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("이벤트를 찾을 수 없음: eventId={}", eventId);
                    return new EventNotFoundException("이벤트 ID: " + eventId);
                });

        // 심플이미지 URL 목록 조회
        List<SimpleImage> simpleImages = simpleImageQueryService.findByEventId(eventId);
        List<String> simpleImageUrls = simpleImages.stream()
                .map(img -> fileUtils.getFileUrl("event/" + eventId, img.getSavedFileName()))
                .collect(Collectors.toList());

        // 썸네일 URL 생성
        String thumbnailUrl = "";
        if (event.getThumbnailFileName() != null && !event.getThumbnailFileName().isEmpty()) {
            thumbnailUrl = fileUtils.getFileUrl("event/" + eventId, event.getThumbnailFileName());
        }

        // 이벤트 상태 정보 로깅
        EventStatus status = event.getStatus();
        log.debug("이벤트 상태: {}, 상태 표시명: {}", status, status.getDisplayName());

        return EventDetailDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .thumbnailUrl(thumbnailUrl)
                .simpleImageUrls(simpleImageUrls)
                .brandId(event.getBrand().getId())
                .brandName(event.getBrand().getName())
                .isActive(event.isActive())
                .status(event.getStatus())
                .statusDisplayName(event.getStatusDisplayName())
                .createdDate(event.getCreatedDate())
                .modifiedDate(event.getModifiedDate())
                .build();
    }

    /**
     * 활성 이벤트 목록 조회 (진행 중인 이벤트) - 캐싱 적용
     */
    @Cacheable(value = "activeEvents")
    public List<EventListDto> findActiveEvents() {
        log.debug("활성 이벤트 목록 조회");

        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findActiveEvents(now);

        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 브랜드의 이벤트 목록 조회 - 캐싱 적용
     */
    @Cacheable(value = "brandEvents", key = "#brandId")
    public List<EventListDto> findEventsByBrandId(Long brandId) {
        log.debug("브랜드별 이벤트 목록 조회: brandId={}", brandId);

        List<Event> events = eventRepository.findByBrandIdOrderByStartDateDesc(brandId);

        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * 검색 조건에 따른 이벤트 목록 페이징 조회
     */
    public Page<EventListDto> searchEvents(EventSearchCondition condition, Pageable pageable) {
        log.debug("이벤트 검색: condition={}, pageable={}", condition, pageable);

        return eventRepository.searchEvents(condition, pageable)
                .map(this::convertToEventListDto);
    }

    /**
     * 상태별 이벤트 목록 조회
     */
    public List<EventListDto> findEventsByStatus(EventStatus status) {
        log.debug("상태별 이벤트 목록 조회: status={}", status);

        LocalDateTime now = LocalDateTime.now();
        List<Event> events;

        switch (status) {
            case UPCOMING:
                // 시작 시간이 현재보다 미래인 이벤트
                events = eventRepository.findByStartDateAfterOrderByStartDateAsc(now);
                break;
            case ACTIVE:
                // 현재 진행 중인 이벤트
                events = eventRepository.findActiveEvents(now);
                break;
            case ENDED:
                // 종료된 이벤트
                events = eventRepository.findByEndDateBeforeOrderByEndDateDesc(now);
                break;
            default:
                events = List.of();
                break;
        }

        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * 모든 이벤트 목록 조회 (페이징)
     */
    public Page<EventListDto> findAllEventsWithPaging(Pageable pageable) {
        log.debug("모든 이벤트 목록 페이징 조회: {}", pageable);
        return eventRepository.findAllWithBrandPaging(pageable)
                .map(this::convertToEventListDto);
    }

    /**
     * 활성화된 이벤트 목록 조회 (페이징)
     */
    public Page<EventListDto> findActiveEventsWithPaging(Pageable pageable) {
        log.debug("활성 이벤트 목록 페이징 조회: {}", pageable);
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findActiveEventsWithBrandPaging(now, pageable)
                .map(this::convertToEventListDto);
    }

    /**
     * 모든 이벤트 목록 조회
     */
    public List<EventListDto> findAllEvents() {
        log.debug("모든 이벤트 목록 조회");
        List<Event> events = eventRepository.findAllWithBrand();
        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * Event 엔티티를 EventListDto로 변환
     */
    private EventListDto convertToEventListDto(Event event) {
        // 썸네일 URL 생성
        String thumbnailUrl = "";
        if (event.getThumbnailFileName() != null && !event.getThumbnailFileName().isEmpty()) {
            thumbnailUrl = fileUtils.getFileUrl("event/" + event.getId(), event.getThumbnailFileName());
        }

        // 이벤트 상태 계산
        EventStatus status = event.getStatus();

        return EventListDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .thumbnailUrl(thumbnailUrl)
                .brandId(event.getBrand().getId())
                .brandName(event.getBrand().getName())
                .isActive(event.isActive())
                .status(status)
                .statusDisplayName(status.getDisplayName())
                .build();
    }
}