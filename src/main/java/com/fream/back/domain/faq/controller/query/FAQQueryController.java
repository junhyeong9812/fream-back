package com.fream.back.domain.faq.controller.query;

import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.service.query.FAQQueryService;
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
@RequestMapping("/faq")
@RequiredArgsConstructor
public class FAQQueryController {

    private final FAQQueryService faqQueryService;

    // === FAQ 목록 조회 ===
    @GetMapping
    public ResponseEntity<Page<FAQResponseDto>> getFAQs(
            @RequestParam(name = "category", required = false) FAQCategory category,
            Pageable pageable
    ) {
        Page<FAQResponseDto> response;

        if (category != null) {
            // 카테고리별 조회
            response = faqQueryService.getFAQsByCategory(category, pageable);
        } else {
            // 전체 조회
            response = faqQueryService.getFAQs(pageable);
        }

        return ResponseEntity.ok(response);
    }

    // === FAQ 단일 조회 ===
    @GetMapping("/{id}")
    public ResponseEntity<FAQResponseDto> getFAQ(@PathVariable("id") Long id) {
        FAQResponseDto response = faqQueryService.getFAQ(id);
        return ResponseEntity.ok(response);
    }

    // === FAQ 검색 ===
    @GetMapping("/search")
    public ResponseEntity<Page<FAQResponseDto>> searchFAQs(
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable
    ) {
        Page<FAQResponseDto> results = faqQueryService.searchFAQs(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    // 파일 다운로드 -> {faqId}/{fileName} 형태
    @GetMapping("/files/{faqId}/{fileName}")
    public ResponseEntity<Resource> downloadFAQFile(@PathVariable Long faqId,
                                                    @PathVariable String fileName) {
        try {
            // baseDir: /home/ubuntu/fream/faq
            // subDir: faq_{faqId}
            // 최종: /home/ubuntu/fream/faq/faq_123/파일.png
            String baseDir = "/home/ubuntu/fream/faq";
            Path filePath = Paths.get(baseDir, "faq_" + faqId, fileName).normalize();

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String contentDisposition = "attachment; filename=\"" + fileName + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

