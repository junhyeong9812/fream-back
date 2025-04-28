package com.fream.back.domain.inspection.controller.command;

import com.fream.back.domain.inspection.dto.InspectionStandardCreateRequestDto;
import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.dto.InspectionStandardUpdateRequestDto;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.exception.InspectionPermissionException;
import com.fream.back.domain.inspection.service.command.InspectionStandardCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 검수 기준 관리 (생성, 수정, 삭제) 컨트롤러
 * - 캐시 무효화 처리 추가
 * - 유효성 검증 (@Valid) 추가
 * - 예외 처리 로직 간소화
 */
@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
@Slf4j
@Validated
public class InspectionCommandController {

    private final InspectionStandardCommandService commandService;
    private final UserQueryService userQueryService;
    private final CacheManager cacheManager;

    // 캐시 이름 목록
    private static final String[] INSPECTION_CACHE_NAMES = {
            "inspectionStandards", "inspectionStandardsByCategory",
            "inspectionStandardDetail", "inspectionStandardSearchResults"
    };

    /**
     * SecurityContext에서 사용자 이메일 추출
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("보안 컨텍스트에 인증 정보가 없습니다.");
            throw new InspectionPermissionException("인증 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }

        if (!(authentication.getPrincipal() instanceof String)) {
            log.warn("인증 주체가 예상 타입(String)이 아닙니다: {}",
                    authentication.getPrincipal() != null
                            ? authentication.getPrincipal().getClass().getName()
                            : "null");
            throw new InspectionPermissionException("인증 정보에 문제가 있습니다. 다시 로그인해주세요.");
        }

        String email = (String) authentication.getPrincipal();
        if (email == null || email.isEmpty()) {
            log.warn("보안 컨텍스트에서 이메일이 비어있습니다.");
            throw new InspectionPermissionException("이메일 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }

        return email;
    }

    /**
     * 모든 검수 기준 관련 캐시 무효화
     */
    private void clearInspectionCaches() {
        for (String cacheName : INSPECTION_CACHE_NAMES) {
            cacheManager.getCache(cacheName).clear();
            log.debug("캐시 무효화: {}", cacheName);
        }
    }

    /**
     * 검수 기준 생성 API
     * - 관리자 권한 검증
     * - 캐시 무효화
     * - 입력값 유효성 검증(@Valid) 추가
     */
    @PostMapping
    public ResponseEntity<ResponseDto<InspectionStandardResponseDto>> createStandard(
            @ModelAttribute @Valid InspectionStandardCreateRequestDto requestDto) {
        String email = null;

        try {
            // 인증 정보 추출
            email = extractEmailFromSecurityContext();
            log.info("검수 기준 생성 시도: 사용자={}", email);

            // 관리자 권한 확인
            userQueryService.checkAdminRole(email);

            // 검수 기준 생성 서비스 호출
            InspectionStandardResponseDto response = commandService.createStandard(requestDto);
            log.info("검수 기준 생성 완료: ID={}, 사용자={}", response.getId(), email);

            // 캐시 무효화
            clearInspectionCaches();

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 검수 기준 생성 시도: {}", email);
            throw new InspectionPermissionException("검수 기준 생성은 관리자만 가능합니다.", e);
        }
    }

    /**
     * 검수 기준 수정 API
     * - 관리자 권한 검증
     * - 캐시 무효화
     * - 입력값 유효성 검증(@Valid) 추가
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<InspectionStandardResponseDto>> updateStandard(
            @PathVariable("id") Long id,
            @ModelAttribute @Valid InspectionStandardUpdateRequestDto requestDto) {
        String email = null;

        try {
            // ID 검증
            if (id == null) {
                throw new InspectionNotFoundException("수정할 검수 기준 ID가 필요합니다.");
            }

            // 인증 정보 추출
            email = extractEmailFromSecurityContext();
            log.info("검수 기준 수정 시도: ID={}, 사용자={}", id, email);

            // 관리자 권한 확인
            userQueryService.checkAdminRole(email);

            // 검수 기준 수정 서비스 호출
            InspectionStandardResponseDto response = commandService.updateStandard(id, requestDto);
            log.info("검수 기준 수정 완료: ID={}, 사용자={}", id, email);

            // 캐시 무효화
            clearInspectionCaches();

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 검수 기준 수정 시도: {}", email);
            throw new InspectionPermissionException("검수 기준 수정은 관리자만 가능합니다.", e);
        }
    }

    /**
     * 검수 기준 삭제 API
     * - 관리자 권한 검증
     * - 캐시 무효화
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteStandard(@PathVariable("id") Long id) {
        String email = null;

        try {
            // 요청 기본 검증
            if (id == null) {
                throw new InspectionNotFoundException("삭제할 검수 기준 ID가 필요합니다.");
            }

            // 인증 정보 추출
            email = extractEmailFromSecurityContext();
            log.info("검수 기준 삭제 시도: ID={}, 사용자={}", id, email);

            // 관리자 권한 확인
            userQueryService.checkAdminRole(email);

            // 검수 기준 삭제 서비스 호출
            commandService.deleteStandard(id);
            log.info("검수 기준 삭제 완료: ID={}, 사용자={}", id, email);

            // 캐시 무효화
            clearInspectionCaches();

            return ResponseEntity.ok(ResponseDto.success(null));
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 검수 기준 삭제 시도: {}", email);
            throw new InspectionPermissionException("검수 기준 삭제는 관리자만 가능합니다.", e);
        }
    }
}