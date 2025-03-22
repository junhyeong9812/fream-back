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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
@Slf4j
public class InspectionQueryController {

    private final InspectionStandardQueryService queryService;
    private static final String INSPECTION_BASE_DIR = "/home/ubuntu/fream/inspection";

    /**
     * 검수 기준 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<InspectionStandardResponseDto>>> getStandards(
            @RequestParam(name = "category", required = false) InspectionCategory category,
            Pageable pageable) {
        try {
            Page<InspectionStandardResponseDto> response;

            if (category != null) {
                log.info("카테고리별 검수 기준 조회: category={}, page={}, size={}",
                        category, pageable.getPageNumber(), pageable.getPageSize());
                response = queryService.getStandardsByCategory(category, pageable);
            } else {
                log.info("전체 검수 기준 조회: page={}, size={}",
                        pageable.getPageNumber(), pageable.getPageSize());
                response = queryService.getStandards(pageable);
            }

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (InspectionNotFoundException e) {
            log.warn("검수 기준 목록 조회 중 리소스를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (InspectionFileException e) {
            log.error("검수 기준 목록 조회 중 파일 관련 오류: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            log.error("검수 기준 목록 조회 중 오류: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("검수 기준 목록 조회 중 예상치 못한 오류: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 단일 조회 API
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<InspectionStandardResponseDto>> getStandard(@PathVariable("id") Long id) {
        try {
            if (id == null) {
                throw new InspectionNotFoundException("조회할 검수 기준 ID가 필요합니다.");
            }

            log.info("검수 기준 단일 조회: ID={}", id);
            InspectionStandardResponseDto response = queryService.getStandard(id);
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (InspectionNotFoundException e) {
            log.warn("검수 기준 조회 중 기준을 찾을 수 없음: ID={}", id);
            throw e;
        } catch (InspectionFileException e) {
            log.error("검수 기준 조회 중 파일 관련 오류: ID={}, 메시지={}", id, e.getMessage());
            throw e;
        } catch (InspectionException e) {
            log.error("검수 기준 조회 중 오류: ID={}, 메시지={}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("검수 기준 조회 중 예상치 못한 오류: ID={}", id, e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 파일 다운로드 API
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

            // 파일 경로 구성
            Path filePath = Paths.get(INSPECTION_BASE_DIR, "inspection_" + inspectionId, fileName).normalize();

            // 경로 검증 (디렉토리 탐색 방지)
            Path basePath = Paths.get(INSPECTION_BASE_DIR, "inspection_" + inspectionId).normalize();
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
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");

            // inline으로 설정하여 브라우저에서 바로 표시
            String contentDisposition = "inline; filename=\"" + encodedFileName + "\"";

            log.debug("파일 다운로드 응답 생성: contentType={}, fileName={}", contentType, encodedFileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (InspectionFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("검수 기준 파일 다운로드 중 파일 관련 오류: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            // IO 예외는 파일 예외로 변환
            log.error("검수 기준 파일 다운로드 중 IO 오류: ", e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_NOT_FOUND,
                    "파일 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            // 기타 예외는 일반 검수 예외로 변환
            log.error("검수 기준 파일 다운로드 중 예상치 못한 오류: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "파일 다운로드 중 오류가 발생했습니다.", e);
        }
    }
}