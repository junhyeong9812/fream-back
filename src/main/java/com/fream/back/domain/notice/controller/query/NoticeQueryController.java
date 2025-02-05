package com.fream.back.domain.notice.controller.query;

import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.service.query.NoticeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeQueryController {

    private final NoticeQueryService noticeQueryService;

    // === 단일 공지 조회 ===
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponseDto> getNotice(@PathVariable("noticeId") Long noticeId) {
        NoticeResponseDto response = noticeQueryService.getNotice(noticeId);
        return ResponseEntity.ok(response);
    }

    // === 공지사항 검색 ===
    @GetMapping("/search")
    public ResponseEntity<Page<NoticeResponseDto>> searchNotices(
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable
    ) {
        Page<NoticeResponseDto> results = noticeQueryService.searchNotices(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    // === 공지사항 카테고리별 조회 ===
    @GetMapping
    public ResponseEntity<Page<NoticeResponseDto>> getNoticesByCategory(
            @RequestParam(name = "category", required = false) String category,
            Pageable pageable
    ) {
        Page<NoticeResponseDto> notices;
        if (category != null) {
            NoticeCategory noticeCategory = NoticeCategory.valueOf(category);
            notices = noticeQueryService.getNoticesByCategory(noticeCategory, pageable);
        } else {
            notices = noticeQueryService.getNotices(pageable);
        }
        return ResponseEntity.ok(notices);
    }

    // 파일 다운로드: /notices/files/{noticeId}/{fileName}
    @GetMapping("/files/{noticeId}/{fileName}")
    public ResponseEntity<Resource> getNoticeFile(@PathVariable Long noticeId,
                                                  @PathVariable String fileName) {
        try {
            // base: /home/ubuntu/fream/notice
            // subdir: notice_{noticeId}
            Path filePath = Paths.get("/home/ubuntu/fream/notice", "notice_" + noticeId, fileName).normalize();

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

