package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.entity.StyleCommentLike;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleCommentLikeRepository;
import com.fream.back.domain.style.service.query.StyleCommentQueryService;
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
public class StyleCommentLikeCommandService {

    private final StyleCommentLikeRepository styleCommentLikeRepository;
    private final ProfileQueryService profileQueryService;
    private final StyleCommentQueryService styleCommentQueryService;

    /**
     * 댓글 좋아요 상태 토글
     *
     * @param email 사용자 이메일
     * @param commentId 댓글 ID
     * @throws StyleException 좋아요 처리 실패 시
     */
    public void toggleCommentLike(String email, Long commentId) {
        log.debug("댓글 좋아요 토글 시작: commentId={}, email={}", commentId, email);

        // 입력값 검증
        if (email == null || email.isEmpty()) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "사용자 이메일이 필요합니다.");
        }

        if (commentId == null) {
            throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
        }

        try {
            // 1. 프로필 조회
            Profile profile = profileQueryService.getProfileByEmail(email);
            log.debug("프로필 조회 성공: profileId={}, profileName={}",
                    profile.getId(), profile.getProfileName());

            // 2. 댓글 조회
            StyleComment comment = styleCommentQueryService.findById(commentId);
            log.debug("댓글 조회 성공: commentId={}, styleId={}", commentId, comment.getStyle().getId());

            // 3. 기존 좋아요 엔티티 조회
            StyleCommentLike existingLike = styleCommentLikeRepository
                    .findByCommentAndProfile(comment, profile).orElse(null);

            if (existingLike != null) {
                // 이미 좋아요 상태인 경우 삭제
                log.debug("기존 댓글 좋아요 삭제: likeId={}, commentId={}, profileId={}",
                        existingLike.getId(), commentId, profile.getId());
                styleCommentLikeRepository.delete(existingLike);
                log.info("댓글 좋아요 취소 완료: commentId={}, profileId={}", commentId, profile.getId());
            } else {
                // 좋아요 추가
                StyleCommentLike commentLike = StyleCommentLike.builder()
                        .comment(comment)
                        .profile(profile)
                        .build();
                StyleCommentLike savedLike = styleCommentLikeRepository.save(commentLike);
                log.info("댓글 좋아요 추가 완료: likeId={}, commentId={}, profileId={}",
                        savedLike.getId(), commentId, profile.getId());
            }
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 좋아요 토글 중 예상치 못한 오류 발생: commentId={}, email={}", commentId, email, e);
            throw new StyleException(StyleErrorCode.LIKE_OPERATION_FAILED,
                    "댓글 좋아요 처리 중 오류가 발생했습니다.", e);
        }
    }
}