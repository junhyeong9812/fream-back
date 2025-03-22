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
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeQueryController {

    private final NoticeQueryService noticeQueryService;
    private static final String NOTICE_BASE_DIR = "/home/ubuntu/fream/notice";

    /**
     * 단일 공지사항 조회 API
     */
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeResponseDto> getNotice(@PathVariable("noticeId") Long noticeId) {
        log.debug("단일 공지사항 조회 API 요청: id={}", noticeId);

        try {
            if (noticeId == null) {
                throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                        "공지사항 ID는 필수입니다.");
            }

            NoticeResponseDto response = noticeQueryService.getNotice(noticeId);
            log.debug("단일 공지사항 조회 API 완료: id={}, title={}", response.getId(), response.getTitle());
            return ResponseEntity.ok(response);
        } catch (NoticeNotFoundException e) {
            // 공지사항을 찾을 수 없는 경우
            log.warn("공지사항을 찾을 수 없음: id={}, error={}", noticeId, e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외
            log.error("공지사항 조회 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 조회 중 예상치 못한 오류: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 공지사항 검색 API
     */
    @GetMapping("/search")
    public ResponseEntity<Page<NoticeResponseDto>> searchNotices(
            @RequestParam(name = "keyword", required = false) String keyword,
            Pageable pageable
    ) {
        log.debug("공지사항 검색 API 요청: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<NoticeResponseDto> results = noticeQueryService.searchNotices(keyword, pageable);
            log.debug("공지사항 검색 API 완료: keyword={}, count={}", keyword, results.getTotalElements());
            return ResponseEntity.ok(results);
        } catch (NoticeException e) {
            // 공지사항 관련 예외
            log.error("공지사항 검색 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 검색 중 예상치 못한 오류: keyword={}", keyword, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 검색 중 오류가 발생했습니다.");
        }
    }

    /**
     * 공지사항 카테고리별 조회 API
     */
    @GetMapping
    public ResponseEntity<Page<NoticeResponseDto>> getNoticesByCategory(
            @RequestParam(name = "category", required = false) String category,
            Pageable pageable
    ) {
        log.debug("공지사항 카테고리별 조회 API 요청: category={}, page={}, size={}",
                category, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<NoticeResponseDto> notices;

            if (category != null) {
                try {
                    NoticeCategory noticeCategory = NoticeCategory.valueOf(category);
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
            // 공지사항 관련 예외
            log.error("공지사항 조회 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 조회 중 예상치 못한 오류: category={}", category, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 목록 조회 중 오류가 발생했습니다.");
        }
    }

    /**
     * 파일 다운로드 API: /notices/files/{noticeId}/{fileName}
     */
    @GetMapping("/files/{noticeId}/{fileName}")
    public ResponseEntity<Resource> getNoticeFile(@PathVariable Long noticeId,
                                                  @PathVariable String fileName) {
        log.debug("공지사항 파일 다운로드 API 요청: id={}, fileName={}", noticeId, fileName);

        try {
            // 입력값 검증
            if (noticeId == null) {
                throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                        "공지사항 ID는 필수입니다.");
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                        "파일명은 필수입니다.");
            }

            // base: /home/ubuntu/fream/notice
            // subdir: notice_{noticeId}
            Path filePath = Paths.get(NOTICE_BASE_DIR, "notice_" + noticeId, fileName).normalize();

            // 디렉토리 탐색 방지 (Path Traversal 취약점 방지)
            if (!filePath.startsWith(Paths.get(NOTICE_BASE_DIR))) {
                log.warn("잘못된 파일 경로 요청: {}", filePath);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                        "잘못된 파일 경로 요청");
            }

            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("요청한 파일을 찾을 수 없음: {}", filePath);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                        "요청한 파일을 찾을 수 없습니다: " + fileName);
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 읽을 수 없음: {}", filePath);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                        "파일을 읽을 수 없습니다: " + fileName);
            }

            log.debug("파일 다운로드 API 완료: id={}, fileName={}", noticeId, fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            log.error("파일 요청 중 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 공지사항 관련 예외
            log.error("파일 요청 중 공지사항 오류: {}", e.getMessage());
            throw e;
        } catch (IOException ex) {
            log.error("파일 읽기 중 IO 오류: fileName={}", fileName, ex);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 읽기 중 오류가 발생했습니다: " + fileName, ex);
        } catch (Exception ex) {
            log.error("파일 요청 처리 중 예상치 못한 오류: fileName={}", fileName, ex);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 요청 처리 중 오류가 발생했습니다.", ex);
        }
    }
}