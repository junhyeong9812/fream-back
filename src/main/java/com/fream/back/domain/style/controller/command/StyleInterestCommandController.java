package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.command.StyleInterestCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/styles/interests/commands")
@RequiredArgsConstructor
public class StyleInterestCommandController {

    private final StyleInterestCommandService styleInterestCommandService;

    /**
     * 스타일 관심 토글 API
     *
     * @param styleId 관심 토글할 스타일 ID
     * @return 빈 응답
     */
    @PostMapping("/{styleId}/toggle")
    public ResponseEntity<Void> toggleInterest(
            @PathVariable("styleId") Long styleId
    ) {
        log.info("스타일 관심 토글 요청: styleId={}", styleId);

        try {
            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();
            if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
                log.error("인증되지 않은 사용자의 관심 토글 시도: styleId={}", styleId);
                throw new StyleException(StyleErrorCode.STYLE_ACCESS_DENIED,
                        "로그인이 필요한 기능입니다.");
            }
            log.debug("사용자 이메일 추출: {}", email);

            styleInterestCommandService.toggleStyleInterest(email, styleId);
            log.info("스타일 관심 토글 완료: styleId={}, email={}", styleId, email);

            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 관심 토글 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.INTEREST_OPERATION_FAILED,
                    "스타일 관심 등록 처리 중 오류가 발생했습니다.", e);
        }
    }
}