package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleCommentLike;
import com.fream.back.domain.style.repository.StyleCommentLikeRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleCommentLikeQueryService {

    private final StyleCommentLikeRepository styleCommentLikeRepository;
    private final ProfileQueryService profileQueryService;

    /**
     * 특정 댓글 ID와 프로필 ID로 좋아요 여부 확인
     */
    public boolean isCommentLikedByProfile(Long commentId, Long profileId) {
        return styleCommentLikeRepository.existsByCommentIdAndProfileId(commentId, profileId);
    }

    /**
     * 특정 댓글 ID로 연결된 좋아요 목록 조회
     */
    public List<StyleCommentLike> findByCommentId(Long commentId) {
        return styleCommentLikeRepository.findByCommentId(commentId);
    }

    /**
     * 여러 댓글에 대한 사용자의 좋아요 상태를 한 번에 확인합니다.
     *
     * @param email    사용자 이메일
     * @param commentIds 댓글 ID 목록
     * @return 사용자가 좋아요한 댓글 ID 집합
     */
    public Set<Long> getLikedCommentIds(String email, List<Long> commentIds) {
        if (email == null || commentIds.isEmpty()) {
            return Set.of();
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            List<StyleCommentLike> likes = styleCommentLikeRepository.findByProfileIdAndCommentIdIn(
                    profile.getId(), commentIds);

            return likes.stream()
                    .map(like -> like.getComment().getId())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }
}
