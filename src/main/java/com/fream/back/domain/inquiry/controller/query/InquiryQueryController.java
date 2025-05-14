package com.fream.back.domain.inquiry.controller.query;

import com.fream.back.domain.inquiry.dto.InquiryResponseDto;
import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.exception.InquiryErrorCode;
import com.fream.back.domain.inquiry.exception.InquiryException;
import com.fream.back.domain.inquiry.service.query.InquiryQueryService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.FileUtils;
import com.fream.back.global.utils.SecurityUtils;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 1대1 문의 Query 컨트롤러
 * 조회 작업 담당
 */
@RestController
@RequestMapping("/inquiry")
@RequiredArgsConstructor
@Slf4j
public class InquiryQueryController {

    private final InquiryQueryService inquiryQueryService;
    private final UserQueryService userQueryService;
    private final FileUtils fileUtils;
    private static final String BASE_DIR = "/home/ubuntu/fream";

    /**
     * 1대1 문의 상세 조회
     *
     * @param inquiryId 문의 ID
     * @return 문의 상세 정보
     */
    @GetMapping("/{inquiryId}")
    public ResponseEntity<ResponseDto<InquiryResponseDto>> getInquiry(@PathVariable Long inquiryId) {

        // 로그인한 사용자 정보 가져오기
        String email = SecurityUtils.extractEmailFromSecurityContext();
        User user = userQueryService.findByEmail(email);
        Long userId = user.getId();
        boolean isAdmin = user.getRole() != null && user.getRole().name().equals("ADMIN");

        log.info("문의 상세 조회 요청: 문의 ID={}, 사용자 ID={}", inquiryId, userId);

        InquiryResponseDto response = inquiryQueryService.getInquiry(inquiryId, userId, isAdmin);

        log.info("문의 상세 조회 완료: 문의 ID={}", inquiryId);

        return ResponseEntity.ok(ResponseDto.success(response, "문의 조회가 완료되었습니다."));
    }

    /**
     * 내 문의 목록 조회
     *
     * @param pageable 페이징 정보
     * @return 내 문의 목록
     */
    @GetMapping("/my")
    public ResponseEntity<ResponseDto<Page<InquirySearchResultDto>>> getMyInquiries(
            @PageableDefault(size = 10) Pageable pageable) {

        // 로그인한 사용자 정보 가져오기
        String email = SecurityUtils.extractEmailFromSecurityContext();
        User user = userQueryService.findByEmail(email);
        Long userId = user.getId();

        log.info("내 문의 목록 조회 요청: 사용자 ID={}", userId);

        Page<InquirySearchResultDto> response = inquiryQueryService.getUserInquiries(userId, pageable);

        log.info("내 문의 목록 조회 완료: 사용자 ID={}, 총 문의 수={}",
                userId, response.getTotalElements());

        return ResponseEntity.ok(ResponseDto.success(response, "내 문의 목록 조회가 완료되었습니다."));
    }

    /**
     * 1대1 문의 목록 조회 (관리자용)
     *
     * @param status 문의 상태 (필터링 조건)
     * @param category 문의 카테고리 (필터링 조건)
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 문의 목록
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Page<InquirySearchResultDto>>> getInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryCategory category,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        // 관리자 권한 확인
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        log.info("문의 목록 조회 요청: 상태={}, 카테고리={}, 키워드={}",
                status != null ? status : "전체",
                category != null ? category : "전체",
                keyword != null ? keyword : "없음");

        // 검색 조건 생성
        InquirySearchCondition condition = InquirySearchCondition.builder()
                .status(status)
                .category(category)
                .keyword(keyword)
                .isAdmin(true) // 관리자 권한으로 조회
                .build();

        Page<InquirySearchResultDto> response = inquiryQueryService.getInquiries(condition, pageable);

        log.info("문의 목록 조회 완료: 총 문의 수={}", response.getTotalElements());

        return ResponseEntity.ok(ResponseDto.success(response, "문의 목록 조회가 완료되었습니다."));
    }

    /**
     * 상태별 문의 목록 조회 (관리자용)
     *
     * @param status 문의 상태
     * @param pageable 페이징 정보
     * @return 상태별 문의 목록
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ResponseDto<Page<InquirySearchResultDto>>> getInquiriesByStatus(
            @PathVariable InquiryStatus status,
            @PageableDefault(size = 10) Pageable pageable) {

        // 관리자 권한 확인
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        log.info("상태별 문의 목록 조회 요청: 상태={}", status);

        Page<InquirySearchResultDto> response = inquiryQueryService.getInquiriesByStatus(status, pageable);

        log.info("상태별 문의 목록 조회 완료: 상태={}, 총 문의 수={}",
                status, response.getTotalElements());

        return ResponseEntity.ok(ResponseDto.success(response, status.getDescription() + " 문의 목록 조회가 완료되었습니다."));
    }

    /**
     * 카테고리별 문의 목록 조회
     *
     * @param category 문의 카테고리
     * @param pageable 페이징 정보
     * @return 카테고리별 문의 목록
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ResponseDto<Page<InquirySearchResultDto>>> getInquiriesByCategory(
            @PathVariable InquiryCategory category,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("카테고리별 문의 목록 조회 요청: 카테고리={}", category);

        Page<InquirySearchResultDto> response = inquiryQueryService.getInquiriesByCategory(category, pageable);

        log.info("카테고리별 문의 목록 조회 완료: 카테고리={}, 총 문의 수={}",
                category, response.getTotalElements());

        return ResponseEntity.ok(ResponseDto.success(response, category.getDescription() + " 문의 목록 조회가 완료되었습니다."));
    }

    /**
     * 키워드로 문의 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<InquirySearchResultDto>>> searchInquiries(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        log.info("문의 검색 요청: 키워드={}", keyword);

        Page<InquirySearchResultDto> response = inquiryQueryService.searchInquiries(keyword, pageable);

        log.info("문의 검색 완료: 키워드={}, 검색 결과 수={}",
                keyword, response.getTotalElements());

        return ResponseEntity.ok(ResponseDto.success(response, "문의 검색이 완료되었습니다."));
    }

    /**
     * 답변 대기 중인 문의 목록 조회 (관리자용)
     *
     * @param pageable 페이징 정보
     * @return 답변 대기 중인 문의 목록
     */
    @GetMapping("/pending")
    public ResponseEntity<ResponseDto<Page<InquirySearchResultDto>>> getPendingInquiries(
            @PageableDefault(size = 10) Pageable pageable) {

        // 관리자 권한 확인
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        log.info("답변 대기 중인 문의 목록 조회 요청");

        Page<InquirySearchResultDto> response = inquiryQueryService.getPendingInquiries(pageable);

        log.info("답변 대기 중인 문의 목록 조회 완료: 총 문의 수={}", response.getTotalElements());

        return ResponseEntity.ok(ResponseDto.success(response, "답변 대기 중인 문의 목록 조회가 완료되었습니다."));
    }

    /**
     * 문의 통계 조회 (관리자용)
     *
     * @return 문의 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<ResponseDto<Object>> getInquiryStatistics() {

        // 관리자 권한 확인
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        log.info("문의 통계 조회 요청");

        Object response = inquiryQueryService.getInquiryStatistics();

        log.info("문의 통계 조회 완료");

        return ResponseEntity.ok(ResponseDto.success(response, "문의 통계 조회가 완료되었습니다."));
    }

    /**
     * 문의 첨부 이미지 다운로드
     *
     * @param inquiryId 문의 ID
     * @param fileName 파일 이름
     * @return 이미지 리소스
     */
    @GetMapping("/files/{inquiryId}/{fileName}")
    public ResponseEntity<Resource> downloadInquiryFile(
            @PathVariable Long inquiryId,
            @PathVariable String fileName) {
        try {
            if (inquiryId == null) {
                throw new InquiryException(InquiryErrorCode.INQUIRY_INVALID_INPUT, "문의 ID가 필요합니다.");
            }

            if (fileName == null || fileName.trim().isEmpty()) {
                throw new InquiryException(InquiryErrorCode.INQUIRY_INVALID_INPUT, "파일 이름이 필요합니다.");
            }

            log.info("문의 이미지 다운로드 요청: 문의 ID={}, 파일명={}", inquiryId, fileName);

            // 권한 확인 - 비공개 문의의 이미지는 작성자와 관리자만 조회 가능
            // 로그인한 사용자 정보 가져오기
            String email = SecurityUtils.extractEmailFromSecurityContext();
            User user = userQueryService.findByEmail(email);
            Long userId = user.getId();
            boolean isAdmin = user.getRole() != null && user.getRole().name().equals("ADMIN");

            // 권한 체크를 위한 문의 조회
            inquiryQueryService.getInquiry(inquiryId, userId, isAdmin);

            // 문의 디렉토리 경로
            String fileDirectory = "inquiry/" + inquiryId;
            Path dirPath = Paths.get(BASE_DIR, fileDirectory);
            Path filePath = dirPath.resolve(fileName).normalize();

            // 경로 검증 (디렉토리 탐색 방지)
            if (!filePath.startsWith(dirPath)) {
                log.warn("잘못된 파일 경로 접근: {}", filePath);
                throw new InquiryException(InquiryErrorCode.INQUIRY_ACCESS_DENIED, "잘못된 파일 경로입니다.");
            }

            // 파일 존재 확인
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                log.warn("파일을 찾을 수 없음: {}", filePath);
                throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "요청한 파일을 찾을 수 없습니다.");
            }

            // 리소스 생성
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 읽을 수 없음: {}", filePath);
                throw new InquiryException(InquiryErrorCode.INQUIRY_SAVE_ERROR, "파일을 읽을 수 없습니다.");
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

            log.info("문의 이미지 다운로드 성공: 문의 ID={}, 파일명={}", inquiryId, fileName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (InquiryException e) {
            log.error("문의 이미지 다운로드 중 오류: 문의 ID={}, 파일명={}, 메시지={}",
                    inquiryId, fileName, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("문의 이미지 다운로드 중 IO 오류: 문의 ID={}, 파일명={}", inquiryId, fileName, e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_SAVE_ERROR,
                    "파일 접근 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("문의 이미지 다운로드 중 예상치 못한 오류: 문의 ID={}, 파일명={}", inquiryId, fileName, e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_SAVE_ERROR,
                    "파일 다운로드 중 오류가 발생했습니다.", e);
        }
    }
}