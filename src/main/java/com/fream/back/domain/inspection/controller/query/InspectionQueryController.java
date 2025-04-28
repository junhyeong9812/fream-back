package com.fream.back.domain.inspection.controller.query;

import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionFileException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.service.query.InspectionStandardQueryService;
import com.fream.back.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

/**
 * 검수 기준 조회 컨트롤러
 * - 캐싱 적용으로 성능 향상
 * - 파일 보안 강화
 */
@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
@Slf4j
public class InspectionQueryController {

    private final InspectionStandardQueryService queryService;
    private static final String INSPECTION_BASE_DIR = "/home/ubuntu/fream/inspection";

    /**
     * 검수 기준 목록 조회 API
     * - 캐싱이 적용되어 성능 향상
     * - 페이징 기본값 설정 추가
     * - 검색 기능 통합
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<InspectionStandardResponseDto>>> getStandards(
            @RequestParam(name = "category", required = false) InspectionCategory category,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<InspectionStandardResponseDto> response;

            if (keyword != null && !keyword.trim().isEmpty()) {
                // 검색 조건이 있는 경우
                log.info("검수 기준 검색: keyword={}, page={}, size={}",
                        keyword, pageable.getPageNumber(), pageable.getPageSize());
                response = queryService.searchStandards(keyword, pageable);
            } else if (category != null) {
                // 카테고리별 조회
                log.info("카테고리별 검수 기준 조회: category={}, page={}, size={}",
                        category, pageable.getPageNumber(), pageable.getPageSize());
                response = queryService.getStandardsByCategory(category, pageable);
            } else {
                // 전체 조회
                log.info("전체 검수 기준 조회: page={}, size={}",
                        pageable.getPageNumber(), pageable.getPageSize());
                response = queryService.getStandards(pageable);
            }

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            // 예외는 글로벌 예외 핸들러로 위임
            log.error("검수 기준 목록 조회 중 오류: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 단일 조회 API
     * - 캐싱이 적용되어 성능 향상
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<InspectionStandardResponseDto>> getStandard(@PathVariable("id") Long id) {
        if (id == null) {
            throw new InspectionNotFoundException("조회할 검수 기준 ID가 필요합니다.");
        }

        log.info("검수 기준 단일 조회: ID={}", id);
        InspectionStandardResponseDto response = queryService.getStandard(id);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    /**
     * 검수 기준 파일 다운로드 API
     * - 보안 강화를 위해 경로 검증 로직 개선
     * - 예외 처리 간소화
     */
    @GetMapping("/files/{inspectionId}/{fileName}")
    public ResponseEntity<Resource> getInspectionFile(
            @PathVariable Long inspectionId,
            @PathVariable String fileName) {
        try {
            if (inspectionId == null) {
                throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                        "검수 기준 ID가 필요합니다.");
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                        "파일 이름이 필요합니다.");
            }

            log.info("검수 기준 파일 다운로드 요청: inspectionId={}, fileName={}", inspectionId, fileName);

            // 파일 경로 구성 및 검증
            String directoryPath = "inspection_" + inspectionId;
            Path basePath = Paths.get(INSPECTION_BASE_DIR, directoryPath).normalize();
            Path filePath = basePath.resolve(fileName).normalize();

            // 경로 검증 (디렉토리 탐색 방지)
            if (!filePath.startsWith(basePath)) {
                log.warn("잘못된 파일 경로 접근 시도: {}", filePath);
                throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                        "잘못된 파일 경로입니다.");
            }

            // 파일 존재 여부 확인
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("요청된 파일을 찾을 수 없음: {}", filePath);
                throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                        "요청한 파일을 찾을 수 없습니다.");
            }

            // 리소스 생성
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일에 접근할 수 없음: {}", filePath);
                throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                        "파일에 접근할 수 없습니다.");
            }

            // Content-Type 결정
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 파일명 인코딩 (한글 파일명 지원)
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            // inline으로 설정하여 브라우저에서 바로 표시
            String contentDisposition = "inline; filename=\"" + encodedFileName + "\"";

            log.debug("파일 다운로드 응답 생성: contentType={}, fileName={}", contentType, encodedFileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (IOException e) {
            // IO 예외는 파일 예외로 변환
            log.error("검수 기준 파일 다운로드 중 IO 오류: ", e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                    "파일 처리 중 오류가 발생했습니다.", e);
        }
    }
}