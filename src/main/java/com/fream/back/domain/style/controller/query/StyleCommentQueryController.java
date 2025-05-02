package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.StyleCommentsResponseDto;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.query.StyleCommentQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/styles/comments/queries")
@RequiredArgsConstructor
public class StyleCommentQueryController {

    private final StyleCommentQueryService styleCommentQueryService;

    /**
     * 특정 스타일의 댓글 목록 조회 API
     *
     * @param styleId 스타일 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 댓글 목록 응답 DTO
     */
    @GetMapping("/{styleId}")
    public ResponseEntity<StyleCommentsResponseDto> getCommentsByStyleId(
            @PathVariable("styleId") Long styleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("스타일 댓글 목록 조회 요청: styleId={}, page={}, size={}", styleId, page, size);

        try {
            // 입력값 검증
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            if (page < 0) {
                log.warn("잘못된 페이지 번호 요청: page={}, 0으로 재설정", page);
                page = 0;
            }

            if (size <= 0 || size > 100) {
                log.warn("잘못된 페이지 크기 요청: size={}, 10으로 재설정", size);
                size = 10;
            }

            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();
            log.debug("사용자 이메일 추출: {}", (email != null ? email : "비로그인 사용자"));

            StyleCommentsResponseDto comments = styleCommentQueryService.getCommentsByStyleId(styleId, email, page, size);
            log.info("스타일 댓글 목록 조회 완료: styleId={}, 댓글 수={}, 총 댓글 수={}",
                    styleId, comments.getComments().size(), comments.getTotalComments());

            return ResponseEntity.ok(comments);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 댓글 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 댓글 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}