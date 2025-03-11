package com.fream.back.domain.event.service.query;

import com.fream.back.domain.event.dto.EventDetailDto;
import com.fream.back.domain.event.dto.EventListDto;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.SimpleImage;
import com.fream.back.domain.event.repository.EventRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 쿼리 서비스는 읽기 전용으로 설정
public class EventQueryService {

    private final EventRepository eventRepository;
    private final SimpleImageQueryService simpleImageQueryService;
    private final FileUtils fileUtils;

    /**
     * 모든 이벤트 목록 조회 (브랜드 정보 포함)
     */
    public List<EventListDto> findAllEvents() {
        List<Event> events = eventRepository.findAllWithBrand();
        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 이벤트 목록 조회 (현재 진행 중인 이벤트)
     */
    public List<EventListDto> findActiveEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findActiveEventsWithBrand(now);
        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 브랜드의 이벤트 목록 조회
     */
    public List<EventListDto> findEventsByBrandId(Long brandId) {
        List<Event> events = eventRepository.findByBrandIdWithBrand(brandId);
        return events.stream()
                .map(this::convertToEventListDto)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 상세 정보 조회 (브랜드 정보 + 심플이미지 목록 포함)
     */
    public EventDetailDto findEventDetail(Long eventId) {
        Event event = eventRepository.findByIdWithBrandAndSimpleImages(eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이벤트가 존재하지 않습니다: " + eventId));

        return convertToEventDetailDto(event);
    }

    /**
     * ID로 이벤트 엔티티 조회 (서비스 내부 사용)
     */
    public Event findById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이벤트가 존재하지 않습니다: " + eventId));
    }

    /**
     * ID로 이벤트 엔티티 조회 (브랜드 정보 포함)
     */
    public Event findByIdWithBrand(Long eventId) {
        return eventRepository.findByIdWithBrand(eventId)
                .orElseThrow(() -> new IllegalArgumentException("해당 이벤트가 존재하지 않습니다: " + eventId));
    }

    /**
     * 이벤트 엔티티를 목록 DTO로 변환
     */
    private EventListDto convertToEventListDto(Event event) {
        // 썸네일 파일 URL 생성
        String thumbnailUrl = "";
        if (event.getThumbnailFileName() != null && !event.getThumbnailFileName().isEmpty()) {
            thumbnailUrl = fileUtils.getFileUrl("event/" + event.getId(), event.getThumbnailFileName());
        }

        return EventListDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .thumbnailUrl(thumbnailUrl)
                .brandId(event.getBrand().getId())
                .brandName(event.getBrand().getName())
                .isActive(isEventActive(event))
                .build();
    }

    /**
     * 이벤트 엔티티를 상세 DTO로 변환
     */
    private EventDetailDto convertToEventDetailDto(Event event) {
        // 썸네일 파일 URL 생성
        String thumbnailUrl = "";
        if (event.getThumbnailFileName() != null && !event.getThumbnailFileName().isEmpty()) {
            thumbnailUrl = fileUtils.getFileUrl("event/" + event.getId(), event.getThumbnailFileName());
        }

        // 심플이미지 URL 목록 생성
        List<String> simpleImageUrls = event.getSimpleImages().stream()
                .map(SimpleImage::getSavedFileName)
                .map(fileName -> fileUtils.getFileUrl("event/" + event.getId(), fileName))
                .collect(Collectors.toList());

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
                .isActive(isEventActive(event))
                .createdDate(event.getCreatedDate())
                .modifiedDate(event.getModifiedDate())
                .build();
    }

    /**
     * 이벤트가 현재 활성 상태인지 확인
     */
    private boolean isEventActive(Event event) {
        LocalDateTime now = LocalDateTime.now();
        return event.getStartDate().isBefore(now) && event.getEndDate().isAfter(now);
    }
}