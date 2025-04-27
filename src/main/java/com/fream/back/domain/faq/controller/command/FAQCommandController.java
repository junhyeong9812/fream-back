package com.fream.back.domain.faq.controller.command;

import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.exception.FAQFileException;
import com.fream.back.domain.faq.exception.FAQNotFoundException;
import com.fream.back.domain.faq.exception.FAQPermissionException;
import com.fream.back.domain.faq.service.command.FAQCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
@Slf4j
public class FAQCommandController {

    private final FAQCommandService faqCommandService;
    private final UserQueryService userQueryService; // 관리자 권한 확인

    /**
     * FAQ 생성
     */
    @PostMapping
    public ResponseEntity<ResponseDto<FAQResponseDto>> createFAQ(
            @Valid @ModelAttribute FAQCreateRequestDto requestDto
    ) {
        // 인증 정보 추출
        String email = extractEmailFromSecurityContext();
        log.info("FAQ 생성 시도: 사용자={}", email);

        // 관리자 권한 확인
        checkAdminPermission(email);

        // FAQ 생성 서비스 호출
        FAQResponseDto response = faqCommandService.createFAQ(requestDto);
        log.info("FAQ 생성 완료: ID={}, 사용자={}", response.getId(), email);

        return ResponseEntity.ok(ResponseDto.success(response));
    }

    /**
     * FAQ 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<FAQResponseDto>> updateFAQ(
            @PathVariable("id") Long id,
            @Valid @ModelAttribute FAQUpdateRequestDto requestDto
    ) {
        if (id == null) {
            throw new FAQNotFoundException("수정할 FAQ ID가 필요합니다.");
        }

        // 인증 정보 추출
        String email = extractEmailFromSecurityContext();
        log.info("FAQ 수정 시도: ID={}, 사용자={}", id, email);

        // 관리자 권한 확인
        checkAdminPermission(email);

        // FAQ 수정 서비스 호출
        FAQResponseDto response = faqCommandService.updateFAQ(id, requestDto);
        log.info("FAQ 수정 완료: ID={}, 사용자={}", id, email);

        return ResponseEntity.ok(ResponseDto.success(response));
    }

    /**
     * FAQ 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteFAQ(
            @PathVariable("id") Long id
    ) {
        if (id == null) {
            throw new FAQNotFoundException("삭제할 FAQ ID가 필요합니다.");
        }

        // 인증 정보 추출
        String email = extractEmailFromSecurityContext();
        log.info("FAQ 삭제 시도: ID={}, 사용자={}", id, email);

        // 관리자 권한 확인
        checkAdminPermission(email);

        // FAQ 삭제 서비스 호출
        faqCommandService.deleteFAQ(id);
        log.info("FAQ 삭제 완료: ID={}, 사용자={}", id, email);

        return ResponseEntity.ok(ResponseDto.success(null));
    }

    /**
     * 이메일 추출 (SecurityContext)
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("보안 컨텍스트에 인증 정보가 없습니다.");
            throw new FAQPermissionException("인증 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }

        if (!(authentication.getPrincipal() instanceof String)) {
            log.warn("인증 주체가 예상 타입(String)이 아닙니다: {}", authentication.getPrincipal().getClass().getName());
            throw new FAQPermissionException("잘못된 인증 정보입니다. 다시 로그인해주세요.");
        }

        String email = (String) authentication.getPrincipal();
        if (email == null || email.isEmpty()) {
            log.warn("보안 컨텍스트에서 이메일이 비어있습니다.");
            throw new FAQPermissionException("이메일 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }

        return email;
    }

    /**
     * 관리자 권한 확인
     */
    private void checkAdminPermission(String email) {
        try {
            userQueryService.checkAdminRole(email);
        } catch (AccessDeniedException e) {
            log.warn("관리자 권한 없는 사용자의 FAQ 접근 시도: {}", email);
            throw new FAQPermissionException("FAQ 관리는 관리자만 가능합니다.", e);
        }
    }
}