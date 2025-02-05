package com.fream.back.domain.faq.controller.command;

import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import com.fream.back.domain.faq.service.command.FAQCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/faq")
@RequiredArgsConstructor
public class FAQCommandController {

    private final FAQCommandService faqCommandService;
    private final UserQueryService userQueryService; // 관리자 권한 확인

    // === 이메일 추출 (SecurityContext) ===
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증되지 않은 사용자입니다.");
    }

    // === FAQ 생성 ===
    @PostMapping
    public ResponseEntity<FAQResponseDto> createFAQ(@ModelAttribute FAQCreateRequestDto requestDto) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인

        FAQResponseDto response = faqCommandService.createFAQ(requestDto);
        return ResponseEntity.ok(response);
    }

    // === FAQ 수정 ===
    @PutMapping("/{id}")
    public ResponseEntity<FAQResponseDto> updateFAQ(
            @PathVariable("id") Long id,
            @ModelAttribute FAQUpdateRequestDto requestDto
    ) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        FAQResponseDto response = faqCommandService.updateFAQ(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // === FAQ 삭제 ===
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFAQ(@PathVariable("id") Long id) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        faqCommandService.deleteFAQ(id);
        return ResponseEntity.noContent().build();
    }
}

