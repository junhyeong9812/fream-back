package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.dto.AddCommentRequestDto;
import com.fream.back.domain.style.dto.UpdateCommentRequestDto;
import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.command.StyleCommentCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/styles/comments/commands")
@RequiredArgsConstructor
public class StyleCommentCommandController {

    private final StyleCommentCommandService styleCommentCommandService;

    /**
     * 댓글 생성 API
     *
     * @param addCommentRequestDto 댓글 생성 요청 DTO
     * @return 생성된 댓글 객체
     */
    @PostMapping
    public ResponseEntity<StyleComment> addComment(
            @RequestBody AddCommentRequestDto addCommentRequestDto
    ) {
        log.info("댓글 생성 요청: styleId={}, parentCommentId={}",
                addCommentRequestDto.getStyleId(),
                addCommentRequestDto.getParentCommentId());

        // 요청 유효성 검사
        if (addCommentRequestDto.getStyleId() == null) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 ID가 필요합니다.");
        }

        if (addCommentRequestDto.getContent() == null || addCommentRequestDto.getContent().trim().isEmpty()) {
            throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID,
                    "댓글 내용이 필요합니다.");
        }

        try {
            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();
            if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
                log.error("인증되지 않은 사용자의 댓글 생성 시도: styleId={}",
                        addCommentRequestDto.getStyleId());
                throw new StyleException(StyleErrorCode.STYLE_ACCESS_DENIED,
                        "로그인이 필요한 기능입니다.");
            }
            log.debug("사용자 이메일 추출: {}", email);

            StyleComment comment = styleCommentCommandService.addComment(
                    email,
                    addCommentRequestDto.getStyleId(),
                    addCommentRequestDto.getContent(),
                    addCommentRequestDto.getParentCommentId()
            );

            log.info("댓글 생성 완료: commentId={}, styleId={}, parentCommentId={}",
                    comment.getId(),
                    addCommentRequestDto.getStyleId(),
                    addCommentRequestDto.getParentCommentId());

            return ResponseEntity.ok(comment);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 생성 중 예상치 못한 오류 발생: styleId={}",
                    addCommentRequestDto.getStyleId(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 수정 API
     *
     * @param commentId 수정할 댓글 ID
     * @param updateCommentRequestDto 댓글 수정 요청 DTO
     * @return 빈 응답
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable("commentId") Long commentId,
            @RequestBody UpdateCommentRequestDto updateCommentRequestDto
    ) {
        log.info("댓글 수정 요청: commentId={}", commentId);

        // 요청 유효성 검사
        if (updateCommentRequestDto.getUpdatedContent() == null ||
                updateCommentRequestDto.getUpdatedContent().trim().isEmpty()) {
            throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID,
                    "수정할 댓글 내용이 필요합니다.");
        }

        try {
            // SecurityContext에서 이메일 추출 (여기선 실제 권한 체크는 서비스 레이어에서 수행)
            String email = SecurityUtils.extractEmailFromSecurityContext();
            if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
                log.error("인증되지 않은 사용자의 댓글 수정 시도: commentId={}", commentId);
                throw new StyleException(StyleErrorCode.STYLE_ACCESS_DENIED,
                        "로그인이 필요한 기능입니다.");
            }

            styleCommentCommandService.updateComment(commentId, updateCommentRequestDto.getUpdatedContent());
            log.info("댓글 수정 완료: commentId={}", commentId);

            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 수정 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 삭제 API
     *
     * @param commentId 삭제할 댓글 ID
     * @return 빈 응답
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") Long commentId) {
        log.info("댓글 삭제 요청: commentId={}", commentId);

        try {
            // SecurityContext에서 이메일 추출 (여기선 실제 권한 체크는 서비스 레이어에서 수행)
            String email = SecurityUtils.extractEmailFromSecurityContext();
            if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
                log.error("인증되지 않은 사용자의 댓글 삭제 시도: commentId={}", commentId);
                throw new StyleException(StyleErrorCode.STYLE_ACCESS_DENIED,
                        "로그인이 필요한 기능입니다.");
            }

            styleCommentCommandService.deleteComment(commentId);
            log.info("댓글 삭제 완료: commentId={}", commentId);

            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 삭제 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 삭제 중 오류가 발생했습니다.", e);
        }
    }
}