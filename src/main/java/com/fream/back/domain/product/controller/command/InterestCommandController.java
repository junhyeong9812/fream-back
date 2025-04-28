package com.fream.back.domain.product.controller.command;

import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.interest.InterestCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관심 상품 관련 명령 컨트롤러
 * 관심 상품의 추가/삭제(토글) 기능을 제공합니다.
 */
@RestController
@RequestMapping("/interests")
@RequiredArgsConstructor
@Slf4j
public class InterestCommandController {

    private final InterestCommandService interestCommandService;

    /**
     * SecurityContext에서 이메일 추출
     * 로그인된 사용자의 이메일을 반환합니다.
     *
     * @return 사용자 이메일
     * @throws ProductException 인증된 사용자가 없는 경우
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new ProductException(ProductErrorCode.INTEREST_TOGGLE_FAILED, "인증된 사용자가 없습니다.");
    }

    /**
     * 관심 상품 토글 API (추가/삭제)
     *
     * @param productColorId 관심 상품의 ProductColor ID
     * @return 상태 메시지
     */
    @PostMapping("/{productColorId}/toggle")
    public ResponseEntity<String> toggleInterest(@PathVariable("productColorId") Long productColorId) {
        log.info("관심 상품 토글 요청 - 상품 색상ID: {}", productColorId);

        try {
            String userEmail = extractEmailFromSecurityContext();
            log.debug("인증된 사용자 확인 - 이메일: {}", userEmail);

            interestCommandService.toggleInterest(userEmail, productColorId);

            log.info("관심 상품 토글 성공 - 사용자: {}, 상품 색상ID: {}", userEmail, productColorId);
            return ResponseEntity.ok("관심 상품 토글 처리 완료");
        } catch (IllegalArgumentException e) {
            log.error("관심 상품 토글 실패 - 상품 색상ID: {}, 오류: {}", productColorId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.INTEREST_TOGGLE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("관심 상품 토글 중 예상치 못한 오류 발생 - 상품 색상ID: {}", productColorId, e);
            throw new ProductException(ProductErrorCode.INTEREST_TOGGLE_FAILED, "관심 상품 처리 중 오류가 발생했습니다.", e);
        }
    }
}