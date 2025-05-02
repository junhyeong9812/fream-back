package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleCommentLike;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleCommentLikeRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleCommentLikeQueryService {

    private final StyleCommentLikeRepository styleCommentLikeRepository;
    private final ProfileQueryService profileQueryService;

    /**
     * 특정 댓글 ID와 프로필 ID로 좋아요 여부 확인
     *
     * @param commentId 댓글 ID
     * @param profileId 프로필 ID
     * @return 좋아요 여부
     */
    public boolean isCommentLikedByProfile(Long commentId, Long profileId) {
        log.debug("댓글 좋아요 여부 확인 시작: commentId={}, profileId={}", commentId, profileId);

        try {
            if (commentId == null) {
                throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
            }

            if (profileId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "프로필 ID가 필요합니다.");
            }

            boolean isLiked = styleCommentLikeRepository.existsByCommentIdAndProfileId(commentId, profileId);
            log.debug("댓글 좋아요 여부 확인 완료: commentId={}, profileId={}, 좋아요={}",
                    commentId, profileId, isLiked);

            return isLiked;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 좋아요 여부 확인 중 예상치 못한 오류 발생: commentId={}, profileId={}",
                    commentId, profileId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 좋아요 여부 확인 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 댓글 ID로 연결된 좋아요 목록 조회
     *
     * @param commentId 댓글 ID
     * @return 좋아요 엔티티 목록
     */
    public List<StyleCommentLike> findByCommentId(Long commentId) {
        log.debug("댓글 좋아요 목록 조회 시작: commentId={}", commentId);

        try {
            if (commentId == null) {
                throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
            }

            List<StyleCommentLike> likes = styleCommentLikeRepository.findByCommentId(commentId);
            log.debug("댓글 좋아요 목록 조회 완료: commentId={}, 좋아요 수={}", commentId, likes.size());

            return likes;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 좋아요 목록 조회 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 좋아요 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 여러 댓글에 대한 사용자의 좋아요 상태를 한 번에 확인합니다.
     *
     * @param email    사용자 이메일
     * @param commentIds 댓글 ID 목록
     * @return 사용자가 좋아요한 댓글 ID 집합
     */
    public Set<Long> getLikedCommentIds(String email, List<Long> commentIds) {
        log.debug("사용자의 여러 댓글 좋아요 상태 확인 시작: email={}, 댓글 수={}",
                email, (commentIds != null ? commentIds.size() : 0));

        try {
            if (email == null || commentIds == null || commentIds.isEmpty()) {
                log.debug("이메일 또는 댓글 ID 목록이 비어있어 빈 결과 반환");
                return Collections.emptySet();
            }

            try {
                Profile profile = profileQueryService.getProfileByEmail(email);
                log.debug("프로필 조회 성공: profileId={}, email={}", profile.getId(), email);

                List<StyleCommentLike> likes = styleCommentLikeRepository.findByProfileIdAndCommentIdIn(
                        profile.getId(), commentIds);

                Set<Long> likedCommentIds = likes.stream()
                        .map(like -> like.getComment().getId())
                        .collect(Collectors.toSet());

                log.debug("좋아요 상태 조회 완료: email={}, 좋아요 댓글 수={}/{}",
                        email, likedCommentIds.size(), commentIds.size());

                return likedCommentIds;
            } catch (Exception e) {
                log.warn("프로필 조회 중 오류 발생 - 빈 결과 반환: email={}, 원인={}", email, e.getMessage());
                return Collections.emptySet();
            }
        } catch (Exception e) {
            log.error("여러 댓글 좋아요 상태 확인 중 예상치 못한 오류 발생: email={}", email, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 좋아요 상태 확인 중 오류가 발생했습니다.", e);
        }
    }
}