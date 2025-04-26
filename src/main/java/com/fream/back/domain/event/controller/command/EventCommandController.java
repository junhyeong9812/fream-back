package com.fream.back.domain.event.controller.command;

import com.fream.back.domain.event.dto.CreateEventRequest;
import com.fream.back.domain.event.dto.UpdateEventRequest;
import com.fream.back.domain.event.entity.EventStatus;
import com.fream.back.domain.event.service.command.EventCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
@Slf4j
public class EventCommandController {

    private final EventCommandService eventCommandService;
    private final UserQueryService userQueryService;

    /**
     * 이벤트 생성
     * @param dto : 제목, 설명, 시작/종료일시, 브랜드ID 등
     * @param thumbnailFile : 썸네일 파일
     * @param simpleImageFiles : 심플이미지 파일 목록
     */
    @PostMapping
    public ResponseEntity<Long> createEvent(
            @Valid @ModelAttribute CreateEventRequest dto,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "simpleImageFiles", required = false) List<MultipartFile> simpleImageFiles
    ) {
        log.debug("이벤트 생성 요청: title={}, brandId={}", dto.getTitle(), dto.getBrandId());

        // 관리자 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        Long eventId = eventCommandService.createEvent(dto, thumbnailFile, simpleImageFiles);
        log.info("이벤트 생성 완료: eventId={}", eventId);

        return ResponseEntity.ok(eventId);
    }

    /**
     * 이벤트 수정
     */
    @PatchMapping("/{eventId}")
    public ResponseEntity<Long> updateEvent(
            @PathVariable("eventId") Long eventId,
            @Valid @ModelAttribute UpdateEventRequest dto,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "simpleImageFiles", required = false) List<MultipartFile> simpleImageFiles,
            @RequestParam(value = "keepImageFileNames", required = false) List<String> keepImageFileNames
    ) {
        log.debug("이벤트 수정 요청: eventId={}, title={}", eventId, dto.getTitle());

        // 관리자 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        Long updatedId = eventCommandService.updateEvent(eventId, dto, thumbnailFile, simpleImageFiles, keepImageFileNames);
        log.info("이벤트 수정 완료: eventId={}", updatedId);

        return ResponseEntity.ok(updatedId);
    }

    /**
     * 이벤트 삭제
     */
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("eventId") Long eventId) {
        log.debug("이벤트 삭제 요청: eventId={}", eventId);

        // 관리자 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        eventCommandService.deleteEvent(eventId);
        log.info("이벤트 삭제 완료: eventId={}", eventId);

        return ResponseEntity.ok().build();
    }

    /**
     * 이벤트 상태 직접 변경 (관리자용)
     */
    @PatchMapping("/{eventId}/status")
    public ResponseEntity<EventStatus> updateEventStatus(
            @PathVariable("eventId") Long eventId,
            @RequestParam("status") EventStatus newStatus
    ) {
        log.debug("이벤트 상태 변경 요청: eventId={}, newStatus={}", eventId, newStatus);

        // 관리자 권한 체크
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        // 상태 업데이트 (현재는 자동 계산되므로 실제로는 시작일/종료일 변경으로 처리)
        EventStatus updatedStatus = eventCommandService.updateEventStatus(eventId, newStatus);
        log.info("이벤트 상태 변경 완료: eventId={}, status={}", eventId, updatedStatus);

        return ResponseEntity.ok(updatedStatus);
    }
}