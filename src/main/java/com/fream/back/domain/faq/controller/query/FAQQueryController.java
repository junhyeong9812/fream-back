package com.fream.back.domain.faq.controller.query;

import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.exception.FAQFileException;
import com.fream.back.domain.faq.exception.FAQNotFoundException;
import com.fream.back.domain.faq.service.query.FAQQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
@Slf4j
public class FAQQueryController {

    private final FAQQueryService faqQueryService;
    private final FileUtils fileUtils;
    private static final String BASE_DIR = "/home/ubuntu/fream";

    /**
     * FAQ 목록 조회 (페이징, 카테고리별 필터링 지원)
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<FAQResponseDto>>> getFAQs(
            @RequestParam(name = "category", required = false) FAQCategory category,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        try {
            Page<FAQResponseDto> response;

            if (category != null) {
                log.info("카테고리별 FAQ 조회: category={}", category);
                // 카테고리별 조회
                response = faqQueryService.getFAQsByCategory(category, pageable);
            } else {
                log.info("전체 FAQ 조회: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
                // 전체 조회
                response = faqQueryService.getFAQs(pageable);
            }

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (FAQException e) {
            // 이미 정의된 FAQ 예외는 그대로 던짐
            log.error("FAQ 목록 조회 중 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * FAQ 단일 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<FAQResponseDto>> getFAQ(
            @PathVariable("id") Long id
    ) {
        try {
            if (id == null) {
                throw new FAQNotFoundException("조회할 FAQ ID가 필요합니다.");
            }

            log.info("단일 FAQ 조회: ID={}", id);
            FAQResponseDto response = faqQueryService.getFAQ(id);
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (FAQException e) {
            log.error("FAQ 조회 중 오류: ID={}, 메시지={}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * FAQ 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<FAQResponseDto>>> searchFAQs(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        try {
            log.info("FAQ 검색: keyword={}. page={}, size={}",
                    keyword != null ? keyword : "전체",
                    pageable.getPageNumber(),
                    pageable.getPageSize());

            Page<FAQResponseDto> results = faqQueryService.searchFAQs(keyword, pageable);
            return ResponseEntity.ok(ResponseDto.success(results));
        } catch (FAQException e) {
            log.error("FAQ 검색 중 오류: keyword={}, 메시지={}", keyword, e.getMessage());
            throw e;
        }
    }

    /**
     * 전체 FAQ 목록 조회 (페이징 없음)
     */
    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<FAQResponseDto>>> getAllFAQs() {
        try {
            log.info("전체 FAQ 목록 조회 (페이징 없음)");
            List<FAQResponseDto> results = faqQueryService.getAllFAQs();
            return ResponseEntity.ok(ResponseDto.success(results));
        } catch (FAQException e) {
            log.error("전체 FAQ 목록 조회 중 오류: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * FAQ 첨부 파일 다운로드
     */
    @GetMapping("/files/{faqId}/{fileName}")
    public ResponseEntity<Resource> downloadFAQFile(
            @PathVariable Long faqId,
            @PathVariable String fileName
    ) {
        try {
            if (faqId == null) {
                throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND, "FAQ ID가 필요합니다.");
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND, "파일 이름이 필요합니다.");
            }

            log.info("FAQ 파일 다운로드 요청: faqId={}, fileName={}", faqId, fileName);

            // FAQ 디렉토리 경로
            String fileDirectory = "faq/" + faqId;
            Path dirPath = Paths.get(BASE_DIR, fileDirectory);
            Path filePath = dirPath.resolve(fileName).normalize();

            // 경로 검증 (디렉토리 탐색 방지)
            if (!filePath.startsWith(dirPath)) {
                log.warn("잘못된 파일 경로 접근: {}", filePath);
                throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND, "잘못된 파일 경로입니다.");
            }

            // 파일 존재 확인
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("파일을 찾을 수 없음: {}", filePath);
                throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND, "요청한 파일을 찾을 수 없습니다.");
            }

            // 리소스 생성
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 읽을 수 없음: {}", filePath);
                throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND, "파일을 읽을 수 없습니다.");
            }

            // 파일 타입 확인
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 파일명 인코딩 (한글 파일명 지원)
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            String contentDisposition = "attachment; filename=\"" + encodedFileName + "\"";

            log.info("FAQ 파일 다운로드 성공: faqId={}, fileName={}", faqId, fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (FAQFileException e) {
            log.error("FAQ 파일 다운로드 중 파일 관련 오류: faqId={}, fileName={}, 메시지={}",
                    faqId, fileName, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("FAQ 파일 다운로드 중 IO 오류: faqId={}, fileName={}", faqId, fileName, e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND,
                    "파일 접근 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 파일 다운로드 중 예상치 못한 오류: faqId={}, fileName={}", faqId, fileName, e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_NOT_FOUND,
                    "파일 다운로드 중 오류가 발생했습니다.", e);
        }
    }
}