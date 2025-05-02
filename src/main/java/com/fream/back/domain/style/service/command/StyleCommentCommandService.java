package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleCommentRepository;
import com.fream.back.domain.style.service.query.StyleQueryService;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StyleCommentCommandService {

    private final StyleCommentRepository styleCommentRepository;
    private final ProfileQueryService profileQueryService;
    private final StyleQueryService styleQueryService;

    /**
     * 댓글 생성
     *
     * @param email 사용자 이메일
     * @param styleId 스타일 ID
     * @param content 댓글 내용
     * @param parentCommentId 부모 댓글 ID (대댓글인 경우)
     * @return 생성된 댓글 객체
     * @throws StyleException 댓글 생성 실패 시
     */
    public StyleComment addComment(String email, Long styleId, String content, Long parentCommentId) {
        log.debug("댓글 생성 시작: styleId={}, parentCommentId={}, email={}",
                styleId, parentCommentId, email);

        // 입력값 검증
        if (email == null || email.isEmpty()) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "사용자 이메일이 필요합니다.");
        }

        if (styleId == null) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID, "댓글 내용이 필요합니다.");
        }

        try {
            // 1. 프로필 조회
            Profile profile = profileQueryService.getProfileByEmail(email);
            log.debug("프로필 조회 성공: profileId={}, profileName={}",
                    profile.getId(), profile.getProfileName());

            // 2. 스타일 조회
            Style style = styleQueryService.findStyleById(styleId);
            log.debug("스타일 조회 성공: styleId={}", styleId);

            // 3. 부모 댓글 조회 (Optional)
            StyleComment parentComment = null;
            if (parentCommentId != null) {
                try {
                    parentComment = styleCommentRepository.findById(parentCommentId)
                            .orElseThrow(() -> new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                                    "부모 댓글을 찾을 수 없습니다: " + parentCommentId));
                    log.debug("부모 댓글 조회 성공: parentCommentId={}", parentCommentId);

                    // 대댓글은 2단계까지만 허용 (대댓글의 대댓글 불가)
                    if (parentComment.getParentComment() != null) {
                        throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID,
                                "대댓글에는 댓글을 달 수 없습니다.");
                    }

                    // 대댓글은 같은 스타일의 댓글에만 달 수 있음
                    if (!parentComment.getStyle().getId().equals(styleId)) {
                        throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID,
                                "다른 스타일의 댓글에 대댓글을 달 수 없습니다.");
                    }
                } catch (StyleException e) {
                    // StyleException은 그대로 던짐
                    throw e;
                } catch (Exception e) {
                    log.error("부모 댓글 조회 중 오류 발생: parentCommentId={}", parentCommentId, e);
                    throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                            "부모 댓글 조회 중 오류가 발생했습니다.", e);
                }
            }

            // 4. 댓글 생성
            StyleComment comment = StyleComment.builder()
                    .style(style)
                    .profile(profile)
                    .content(content)
                    .parentComment(parentComment)
                    .build();

            // 5. 연관관계 설정
            style.addComment(comment);          // Style -> Comment
            if (parentComment != null) {
                parentComment.addChildComment(comment); // Parent -> Child Comment
            }

            // 6. 댓글 저장
            StyleComment savedComment = styleCommentRepository.save(comment);
            log.info("댓글 생성 완료: commentId={}, styleId={}, parentCommentId={}",
                    savedComment.getId(), styleId, parentCommentId);

            return savedComment;

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 생성 중 예상치 못한 오류 발생: styleId={}, email={}", styleId, email, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 수정
     *
     * @param commentId 수정할 댓글 ID
     * @param updatedContent 수정할 내용
     * @throws StyleException 댓글 수정 실패 시
     */
    public void updateComment(Long commentId, String updatedContent) {
        log.debug("댓글 수정 시작: commentId={}", commentId);

        // 입력값 검증
        if (commentId == null) {
            throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
        }

        if (updatedContent == null || updatedContent.trim().isEmpty()) {
            throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID, "댓글 내용이 필요합니다.");
        }

        try {
            StyleComment comment = styleCommentRepository.findById(commentId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                            "댓글을 찾을 수 없습니다: " + commentId));

            // 댓글 내용 업데이트
            comment.updateContent(updatedContent);
            styleCommentRepository.save(comment);
            log.info("댓글 수정 완료: commentId={}", commentId);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (IllegalArgumentException e) {
            // StyleComment.updateContent()에서 발생할 수 있는 예외
            log.error("댓글 수정 검증 실패: commentId={}, 원인={}", commentId, e.getMessage());
            throw new StyleException(StyleErrorCode.COMMENT_CONTENT_INVALID, e.getMessage(), e);
        } catch (Exception e) {
            log.error("댓글 수정 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 삭제
     *
     * @param commentId 삭제할 댓글 ID
     * @throws StyleException 댓글 삭제 실패 시
     */
    public void deleteComment(Long commentId) {
        log.debug("댓글 삭제 시작: commentId={}", commentId);

        if (commentId == null) {
            throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
        }

        try {
            StyleComment comment = styleCommentRepository.findById(commentId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                            "댓글을 찾을 수 없습니다: " + commentId));

            // 대댓글 확인
            int replyCount = comment.getChildComments().size();
            if (replyCount > 0) {
                log.debug("댓글에 달린 대댓글 존재: commentId={}, 대댓글 수={}", commentId, replyCount);
            }

            styleCommentRepository.delete(comment); // 자식 댓글은 cascade로 자동 삭제
            log.info("댓글 삭제 완료: commentId={}", commentId);

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