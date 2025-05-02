package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.command.StyleCommentLikeCommandService;
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
@RequestMapping("/styles/comments/likes/commands")
@RequiredArgsConstructor
public class StyleCommentLikeCommandController {

    private final StyleCommentLikeCommandService styleCommentLikeCommandService;

    /**
     * 댓글 좋아요 토글 API
     *
     * @param commentId 좋아요 토글할 댓글 ID
     * @return 빈 응답
     */
    @PostMapping("/{commentId}/toggle")
    public ResponseEntity<Void> toggleCommentLike(
            @PathVariable("commentId") Long commentId
    ) {
        log.info("댓글 좋아요 토글 요청: commentId={}", commentId);

        try {
            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();
            if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
                log.error("인증되지 않은 사용자의 댓글 좋아요 토글 시도: commentId={}", commentId);
                throw new StyleException(StyleErrorCode.STYLE_ACCESS_DENIED,
                        "로그인이 필요한 기능입니다.");
            }
            log.debug("사용자 이메일 추출: {}", email);

            styleCommentLikeCommandService.toggleCommentLike(email, commentId);
            log.info("댓글 좋아요 토글 완료: commentId={}, email={}", commentId, email);

            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 좋아요 토글 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.LIKE_OPERATION_FAILED,
                    "댓글 좋아요 처리 중 오류가 발생했습니다.", e);
        }
    }
}