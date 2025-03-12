package com.fream.back.domain.event.controller.command;

import com.fream.back.domain.event.dto.CreateEventRequest;
import com.fream.back.domain.event.dto.UpdateEventRequest;
import com.fream.back.domain.event.service.command.EventCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
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
            @ModelAttribute CreateEventRequest dto,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "simpleImageFiles", required = false) List<MultipartFile> simpleImageFiles
    ) {
        // 1. 이메일 추출
        String email = SecurityUtils.extractEmailFromSecurityContext();
        // 2. 관리자 권한 체크
        userQueryService.checkAdminRole(email);

        // 3. 생성 로직
        Long eventId = eventCommandService.createEvent(dto, thumbnailFile, simpleImageFiles);
        return ResponseEntity.ok(eventId);
    }

    /**
     * 이벤트 수정
     * - 전체 심플이미지 "삭제 후" 새로 등록하는 방식
     */
    @PatchMapping("/{eventId}")
    public ResponseEntity<Long> updateEvent(
            @PathVariable("eventId") Long eventId,
            @ModelAttribute UpdateEventRequest dto,
            @RequestParam(value = "thumbnailFile", required = false) MultipartFile thumbnailFile,
            @RequestParam(value = "simpleImageFiles", required = false) List<MultipartFile> simpleImageFiles
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        Long updatedId = eventCommandService.updateEvent(eventId, dto, thumbnailFile, simpleImageFiles);
        return ResponseEntity.ok(updatedId);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable("eventId") Long eventId) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        eventCommandService.deleteEvent(eventId);
        return ResponseEntity.ok().build();
    }


}
