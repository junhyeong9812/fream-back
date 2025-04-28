package com.fream.back.domain.notice.controller.query;

import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.exception.NoticeErrorCode;
import com.fream.back.domain.notice.exception.NoticeException;
import com.fream.back.domain.notice.exception.NoticeFileException;
import com.fream.back.domain.notice.exception.NoticeNotFoundException;
import com.fream.back.domain.notice.service.query.NoticeQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 공지사항 조회 컨트롤러
 */
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeQueryController {

    private final NoticeQueryService noticeQueryService;

    /**
     * 단일 공지사항 조회 API
     *
     * @param noticeId 공지사항 ID
     * @return 공지사항 정보
     */
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponseDto> getNotice(@PathVariable("noticeId") Long noticeId) {
        log.debug("단일 공지사항 조회 API 요청: id={}", noticeId);

        try {
            if (noticeId == null) {
                throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "공지사항 ID는 필수입니다.");
            }

            NoticeResponseDto response = noticeQueryService.getNotice(noticeId);
            log.debug("단일 공지사항 조회 API 완료: id={}, title={}", response.getId(), response.getTitle());
            return ResponseEntity.ok(response);
        } catch (NoticeNotFoundException e) {
            log.warn("공지사항을 찾을 수 없음: id={}, error={}", noticeId, e.getMessage());
            throw e;
        } catch (NoticeException e) {
            log.error("공지사항 조회 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 조회 중 예상치 못한 오류: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR, "공지사항 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 공지사항 검색 API
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 목록
     */
    @GetMapping("/search")
    public ResponseEntity<Page<NoticeResponseDto>> searchNotices(
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdDate,desc") Pageable pageable
    ) {
        log.debug("공지사항 검색 API 요청: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<NoticeResponseDto> results = noticeQueryService.searchNotices(keyword, pageable);
            log.debug("공지사항 검색 API 완료: keyword={}, count={}", keyword, results.getTotalElements());
            return ResponseEntity.ok(results);
        } catch (NoticeException e) {
            log.error("공지사항 검색 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 검색 중 예상치 못한 오류: keyword={}", keyword, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR, "공지사항 검색 중 오류가 발생했습니다.");
        }
    }

    /**
     * 공지사항 카테고리별 조회 API
     *
     * @param category 카테고리명 (전체 조회 시 미입력)
     * @param pageable 페이징 정보
     * @return 공지사항 목록
     */
    @GetMapping
    public ResponseEntity<Page<NoticeResponseDto>> getNoticesByCategory(
            @RequestParam(name = "category", required = false) String category,
            @PageableDefault(size = 10, sort = "createdDate,desc") Pageable pageable
    ) {
        log.debug("공지사항 카테고리별 조회 API 요청: category={}, page={}, size={}",
                category, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<NoticeResponseDto> notices;

            if (category != null && !category.trim().isEmpty()) {
                try {
                    NoticeCategory noticeCategory = NoticeCategory.valueOf(category.toUpperCase());
                    notices = noticeQueryService.getNoticesByCategory(noticeCategory, pageable);
                    log.debug("카테고리별 공지사항 조회 완료: category={}, count={}",
                            category, notices.getTotalElements());
                } catch (IllegalArgumentException e) {
                    log.warn("유효하지 않은 카테고리: {}", category);
                    throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_CATEGORY,
                            "유효하지 않은 카테고리: " + category);
                }
            } else {
                notices = noticeQueryService.getNotices(pageable);
                log.debug("전체 공지사항 조회 완료: count={}", notices.getTotalElements());
            }

            return ResponseEntity.ok(notices);
        } catch (NoticeException e) {
            log.error("공지사항 조회 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 조회 중 예상치 못한 오류: category={}", category, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR, "공지사항 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 파일 다운로드 API: /notices/files/{noticeId}/{fileName}
     *
     * @param noticeId 공지사항 ID
     * @param fileName 파일명
     * @return 파일 리소스
     */
    @GetMapping("/files/{noticeId}/{fileName}")
    public ResponseEntity<Resource> getNoticeFile(
            @PathVariable Long noticeId,
            @PathVariable String fileName
    ) {
        log.debug("공지사항 파일 다운로드 API 요청: id={}, fileName={}", noticeId, fileName);

        try {
            // 입력값 검증
            if (noticeId == null) {
                throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "공지사항 ID는 필수입니다.");
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND, "파일명은 필수입니다.");
            }

            // 파일 조회 및 응답 처리
            Resource resource = noticeQueryService.getNoticeFileResource(noticeId, fileName);
            String contentType = determineContentType(fileName);

            log.debug("파일 다운로드 API 완료: id={}, fileName={}", noticeId, fileName);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (NoticeFileException e) {
            log.error("파일 요청 중 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            log.error("파일 요청 중 공지사항 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("파일 요청 처리 중 예상치 못한 오류: fileName={}", fileName, e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND, "파일 요청 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 파일 확장자에 따른 Content-Type 결정
     *
     * @param fileName 파일명
     * @return 컨텐츠 타입
     */
    private String determineContentType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.toLowerCase().endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileName.toLowerCase().endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (fileName.toLowerCase().endsWith(".mov")) {
            return "video/quicktime";
        } else {
            return "application/octet-stream";
        }
    }
}