package com.fream.back.domain.event.service.command;

import com.fream.back.domain.event.dto.UpdateEventRequest;
import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.domain.event.entity.SimpleImage;
import com.fream.back.domain.event.exception.EventNotFoundException;
import com.fream.back.domain.event.exception.InvalidEventDateException;
import com.fream.back.domain.event.repository.SimpleImageRepository;
import com.fream.back.domain.event.service.query.SimpleImageQueryService;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.exception.BrandNotFoundException;
import com.fream.back.domain.product.service.brand.BrandQueryService;
import com.fream.back.domain.event.dto.CreateEventRequest;
import com.fream.back.domain.event.repository.EventRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EventCommandService {
    private final EventRepository eventRepository;
    private final SimpleImageCommandService simpleImageCommandService;
    private final SimpleImageQueryService simpleImageQueryService;
    private final SimpleImageRepository simpleImageRepository;
    private final BrandQueryService brandQueryService;
    private final FileUtils fileUtils;

    /**
     * 이벤트 생성
     */
    public Long createEvent(CreateEventRequest request,
                            MultipartFile thumbnailFile,
                            List<MultipartFile> simpleImageFiles) {
        log.info("이벤트 생성 시작: title={}, brandId={}", request.getTitle(), request.getBrandId());

        // 요청 데이터 검증
        validateEventDates(request.getStartDate(), request.getEndDate());

        try {
            // 브랜드 조회
            Brand brand;
            try {
                brand = brandQueryService.findById(request.getBrandId());
            } catch (RuntimeException e) {
                log.error("이벤트 생성 실패 - 브랜드를 찾을 수 없음: brandId={}", request.getBrandId(), e);
                throw new BrandNotFoundException("브랜드 ID: " + request.getBrandId());
            }

            // Event 엔티티 생성
            Event event = Event.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .thumbnailFileName("")
                    .brand(brand)
                    .build();

            // 이벤트 상태 로깅 (상태는 자동 계산됨)
            EventStatus initialStatus = event.getStatus();
            log.debug("이벤트 초기 상태: {}", initialStatus.getDisplayName());

            eventRepository.save(event);
            log.debug("이벤트 엔티티 저장 완료: eventId={}", event.getId());

            // 썸네일 파일 저장 (thumbnailFile이 있을 경우에만)
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                String directory = "event/" + event.getId();
                String savedThumbnailName = fileUtils.saveFile(directory, "thumbnail_" + event.getId() + "_", thumbnailFile);

                // DB에는 파일명만 저장
                event.updateThumbnailFileName(savedThumbnailName);
                log.debug("썸네일 저장 완료: eventId={}, fileName={}", event.getId(), savedThumbnailName);
            }

            // 심플이미지 저장
            if (simpleImageFiles != null && !simpleImageFiles.isEmpty()) {
                simpleImageCommandService.createSimpleImages(event, simpleImageFiles);
                log.debug("심플이미지 저장 완료: eventId={}, 개수={}", event.getId(), simpleImageFiles.size());
            }

            log.info("이벤트 생성 완료: eventId={}, status={}", event.getId(), event.getStatus());
            return event.getId();
        } catch (BrandNotFoundException e) {
            log.error("이벤트 생성 실패 - 브랜드를 찾을 수 없음: brandId={}", request.getBrandId(), e);
            throw e; // 브랜드 예외는 그대로 전달
        } catch (Exception e) {
            log.error("이벤트 생성 중 예외 발생", e);
            throw new RuntimeException("이벤트 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 이벤트 수정
     */
    public Long updateEvent(Long eventId,
                            UpdateEventRequest request,
                            MultipartFile thumbnailFile,
                            List<MultipartFile> simpleImageFiles,
                            List<String> keepImageFileNames) {
        log.info("이벤트 수정 시작: eventId={}", eventId);

        // 요청 데이터 검증
        validateEventDates(request.getStartDate(), request.getEndDate());

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("이벤트 수정 실패 - 이벤트를 찾을 수 없음: eventId={}", eventId);
                    return new EventNotFoundException("이벤트 ID: " + eventId);
                });

        // 수정 전 상태 기록
        EventStatus beforeStatus = event.getStatus();
        log.debug("이벤트 수정 전 상태: {}", beforeStatus.getDisplayName());

        // 1) 이벤트 기본 필드 업데이트
        event.updateEvent(request.getTitle(),
                request.getDescription(),
                request.getStartDate(),
                request.getEndDate());

        // 수정 후 상태 기록
        EventStatus afterStatus = event.getStatus();
        log.debug("이벤트 수정 후 상태: {}", afterStatus.getDisplayName());

        // 상태가 변경된 경우 로그 남김
        if (beforeStatus != afterStatus) {
            log.info("이벤트 상태 변경됨: {} -> {}", beforeStatus.getDisplayName(), afterStatus.getDisplayName());
        }

        log.debug("이벤트 기본 정보 수정 완료: eventId={}", eventId);

        // 2) 심플이미지 처리 - 유지할 이미지와 삭제할 이미지 분리
        handleSimpleImagesUpdate(event, keepImageFileNames, simpleImageFiles);

        // 3) 썸네일 교체
        handleThumbnailUpdate(event, thumbnailFile);

        // 이벤트 저장
        eventRepository.save(event);
        log.info("이벤트 수정 완료: eventId={}, status={}", eventId, event.getStatus());

        return event.getId();
    }

    /**
     * 이벤트 상태 직접 변경 (관리자용)
     * 실제로는 상태가 자동 계산되므로, 시작일/종료일을 조정하여 원하는 상태로 변경
     */
    public EventStatus updateEventStatus(Long eventId, EventStatus newStatus) {
        log.info("이벤트 상태 변경 시작: eventId={}, newStatus={}", eventId, newStatus);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("이벤트 상태 변경 실패 - 이벤트를 찾을 수 없음: eventId={}", eventId);
                    return new EventNotFoundException("이벤트 ID: " + eventId);
                });

        // 기존 상태 확인
        EventStatus currentStatus = event.getStatus();
        log.debug("이벤트 현재 상태: {}", currentStatus.getDisplayName());

        // 이미 원하는 상태이면 변경하지 않음
        if (currentStatus == newStatus) {
            log.debug("이벤트가 이미 원하는 상태임: {}", newStatus);
            return currentStatus;
        }

        // 현재 시간 기준
        LocalDateTime now = LocalDateTime.now();

        // 원하는 상태에 따라 시작일/종료일 조정
        switch (newStatus) {
            case UPCOMING:
                // 시작일을 미래로 설정 (현재 시간 + 1일)
                LocalDateTime newStartDate = now.plusDays(1);
                // 종료일도 그 이후로 설정
                LocalDateTime newEndDate = newStartDate.plusDays(7);

                event.updateEvent(
                        event.getTitle(),
                        event.getDescription(),
                        newStartDate,
                        newEndDate
                );
                log.debug("이벤트 시작일/종료일 변경 (UPCOMING으로): startDate={}, endDate={}", newStartDate, newEndDate);
                break;

            case ACTIVE:
                // 시작일을 과거로, 종료일을 미래로 설정
                LocalDateTime pastDate = now.minusDays(1);
                LocalDateTime futureDate = now.plusDays(7);

                event.updateEvent(
                        event.getTitle(),
                        event.getDescription(),
                        pastDate,
                        futureDate
                );
                log.debug("이벤트 시작일/종료일 변경 (ACTIVE로): startDate={}, endDate={}", pastDate, futureDate);
                break;

            case ENDED:
                // 시작일과 종료일 모두 과거로 설정
                LocalDateTime pastStartDate = now.minusDays(14);
                LocalDateTime pastEndDate = now.minusDays(1);

                event.updateEvent(
                        event.getTitle(),
                        event.getDescription(),
                        pastStartDate,
                        pastEndDate
                );
                log.debug("이벤트 시작일/종료일 변경 (ENDED로): startDate={}, endDate={}", pastStartDate, pastEndDate);
                break;

            default:
                log.warn("지원하지 않는 이벤트 상태: {}", newStatus);
                break;
        }

        // 변경사항 저장
        eventRepository.save(event);

        // 상태 확인
        EventStatus resultStatus = event.getStatus();
        log.info("이벤트 상태 변경 완료: eventId={}, oldStatus={}, newStatus={}",
                eventId, currentStatus, resultStatus);

        return resultStatus;
    }

    /**
     * 심플 이미지 업데이트 처리
     */
    private void handleSimpleImagesUpdate(Event event, List<String> keepImageFileNames, List<MultipartFile> newImageFiles) {
        log.debug("심플이미지 업데이트 시작: eventId={}", event.getId());

        // 현재 저장된 모든 심플이미지 가져오기
        List<SimpleImage> currentImages = simpleImageQueryService.findByEventId(event.getId());
        log.debug("현재 저장된 심플이미지 개수: {}", currentImages.size());

        // keepImageFileNames가 null이면 빈 리스트로 초기화
        if (keepImageFileNames == null) {
            keepImageFileNames = new ArrayList<>();
        }

        // 유지할 이미지 파일명을 Set으로 변환 (조회 성능 향상)
        Set<String> keepImageFileNameSet = new HashSet<>(keepImageFileNames);
        log.debug("유지할 심플이미지 개수: {}", keepImageFileNameSet.size());

        // 삭제할 이미지 필터링
        List<SimpleImage> imagesToDelete = currentImages.stream()
                .filter(img -> !keepImageFileNameSet.contains(img.getSavedFileName()))
                .collect(Collectors.toList());
        log.debug("삭제할 심플이미지 개수: {}", imagesToDelete.size());

        // 삭제할 이미지 처리
        for (SimpleImage imageToDelete : imagesToDelete) {
            try {
                // 파일 시스템에서 삭제
                String directory = "event/" + event.getId();
                boolean deleted = fileUtils.deleteFile(directory, imageToDelete.getSavedFileName());
                log.debug("이미지 파일 삭제 {}: {}", deleted ? "성공" : "실패", imageToDelete.getSavedFileName());

                // 엔티티 삭제
                simpleImageRepository.delete(imageToDelete);
            } catch (Exception e) {
                log.error("이미지 삭제 중 오류 발생: fileName={}", imageToDelete.getSavedFileName(), e);
                throw new RuntimeException("이미지 삭제 중 오류 발생: " + e.getMessage(), e);
            }
        }

        // 새 이미지 추가
        if (newImageFiles != null && !newImageFiles.isEmpty()) {
            log.debug("새 심플이미지 추가: 개수={}", newImageFiles.size());
            simpleImageCommandService.createSimpleImages(event, newImageFiles);
        }
    }

    /**
     * 썸네일 업데이트 처리
     */
    private void handleThumbnailUpdate(Event event, MultipartFile thumbnailFile) {
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            log.debug("썸네일 업데이트 시작: eventId={}", event.getId());
            try {
                // 기존 썸네일 파일이 있으면 삭제
                if (event.getThumbnailFileName() != null && !event.getThumbnailFileName().isEmpty()) {
                    String directory = "event/" + event.getId();
                    boolean deleted = fileUtils.deleteFile(directory, event.getThumbnailFileName());
                    log.debug("기존 썸네일 삭제 {}: {}", deleted ? "성공" : "실패", event.getThumbnailFileName());
                }

                // 새 썸네일 저장
                String directory = "event/" + event.getId();
                String savedThumbnailName = fileUtils.saveFile(directory,
                        "thumbnail_" + event.getId() + "_", thumbnailFile);
                log.debug("새 썸네일 저장 완료: {}", savedThumbnailName);

                // DB에는 파일명만 업데이트
                event.updateThumbnailFileName(savedThumbnailName);
            } catch (Exception e) {
                log.error("썸네일 업데이트 중 오류 발생", e);
                throw new RuntimeException("썸네일 업데이트 중 오류 발생: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 이벤트 날짜 유효성 검사
     */
    private void validateEventDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            log.error("유효하지 않은 이벤트 날짜: startDate={}, endDate={}", startDate, endDate);
            throw new InvalidEventDateException("시작일과 종료일은 필수입니다.");
        }

        if (startDate.isAfter(endDate)) {
            log.error("종료일이 시작일보다 이전임: startDate={}, endDate={}", startDate, endDate);
            throw new InvalidEventDateException("종료일은 시작일보다 이후여야 합니다.");
        }

        // 추가 비즈니스 규칙 - 최소 이벤트 기간 체크
        if (endDate.isBefore(startDate.plusHours(1))) {
            log.error("이벤트 기간이 너무 짧음: startDate={}, endDate={}", startDate, endDate);
            throw new InvalidEventDateException("이벤트 기간은 최소 1시간 이상이어야 합니다.");
        }
    }

    /**
     * 이벤트 삭제
     */
    public void deleteEvent(Long eventId) {
        log.info("이벤트 삭제 시작: eventId={}", eventId);

        // 1) 이벤트 조회
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.error("이벤트 삭제 실패 - 이벤트를 찾을 수 없음: eventId={}", eventId);
                    return new EventNotFoundException("이벤트 ID: " + eventId);
                });

        log.debug("삭제할 이벤트 상태: {}", event.getStatus().getDisplayName());

        try {
            // 2) 썸네일 파일 삭제
            if (event.getThumbnailFileName() != null && !event.getThumbnailFileName().isEmpty()) {
                String directory = "event/" + eventId;
                boolean deleted = fileUtils.deleteFile(directory, event.getThumbnailFileName());
                log.debug("썸네일 삭제 {}: {}", deleted ? "성공" : "실패", event.getThumbnailFileName());
            }

            // 3) 심플이미지 파일 삭제
            List<SimpleImage> images = simpleImageQueryService.findByEventId(eventId);
            for (SimpleImage image : images) {
                String directory = "event/" + eventId;
                boolean deleted = fileUtils.deleteFile(directory, image.getSavedFileName());
                log.debug("심플이미지 삭제 {}: {}", deleted ? "성공" : "실패", image.getSavedFileName());
            }

            // 4) 이벤트 엔티티 삭제 (cascade로 심플이미지도 함께 삭제됨)
            eventRepository.delete(event);
            log.info("이벤트 삭제 완료: eventId={}", eventId);
        } catch (Exception e) {
            log.error("이벤트 삭제 중 오류 발생: eventId={}", eventId, e);
            throw new RuntimeException("이벤트 삭제 중 오류 발생: " + e.getMessage(), e);
        }
    }
}