package com.fream.back.domain.event.controller.query;

import com.fream.back.domain.event.dto.EventDetailDto;
import com.fream.back.domain.event.dto.EventListDto;
import com.fream.back.domain.event.dto.EventSearchCondition;
import com.fream.back.domain.event.service.query.EventQueryService;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventQueryController {

    private final EventQueryService eventQueryService;
    private final FileUtils fileUtils;
    private static final String BASE_DIR = "/home/ubuntu/fream";

    /**
     * 모든 이벤트 목록 조회 (페이징)
     * GET /events/page
     */
    @GetMapping("/page")
    public ResponseEntity<Page<EventListDto>> getAllEventsPaging(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(eventQueryService.findAllEventsWithPaging(pageable));
    }

    /**
     * 이벤트 검색 (페이징)
     * GET /events/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<EventListDto>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        EventSearchCondition condition = EventSearchCondition.builder()
                .keyword(keyword)
                .brandId(brandId)
                .isActive(isActive)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        return ResponseEntity.ok(eventQueryService.searchEvents(condition, pageable));
    }

    /**
     * 활성 이벤트 목록 조회 (페이징)
     * GET /events/active
     */
    @GetMapping("/active")
    public ResponseEntity<Page<EventListDto>> getActiveEvents(
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(eventQueryService.findActiveEventsWithPaging(pageable));
    }
    /**
     * 모든 이벤트 목록 조회
     * GET /events
     */
    @GetMapping
    public ResponseEntity<List<EventListDto>> getAllEvents() {
        return ResponseEntity.ok(eventQueryService.findAllEvents());
    }


    /**
     * 특정 브랜드의 이벤트 목록 조회
     * GET /events/brands/{brandId}
     */
    @GetMapping("/brands/{brandId}")
    public ResponseEntity<List<EventListDto>> getEventsByBrand(@PathVariable Long brandId) {
        return ResponseEntity.ok(eventQueryService.findEventsByBrandId(brandId));
    }

    /**
     * 이벤트 상세 정보 조회
     * GET /events/{eventId}
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailDto> getEventDetail(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventQueryService.findEventDetail(eventId));
    }

    // 서버 내부에 저장된 파일을 직접 읽어서 ResponseEntity<Resource>로 응답
    @GetMapping("/{eventId}/images/{fileName}")
    public ResponseEntity<?> getEventImage(
            @PathVariable("eventId") Long eventId,
            @PathVariable("fileName") String fileName
    ) {
        // 디렉토리 + 파일명
        String directory = "event/" + eventId;

        // FileUtils를 사용해 파일 존재 확인
        if (!fileUtils.existsFile(directory, fileName)) {
            return ResponseEntity.notFound().build();
        }

        // 파일 경로 생성
        File file = new File(BASE_DIR + File.separator + directory + File.separator + fileName);

        // 파일 확장자에 따라 ContentType 설정
        HttpHeaders headers = new HttpHeaders();
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            headers.setContentType(MediaType.IMAGE_JPEG);
        } else if (fileName.toLowerCase().endsWith(".png")) {
            headers.setContentType(MediaType.IMAGE_PNG);
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            headers.setContentType(MediaType.IMAGE_GIF);
        } else {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
