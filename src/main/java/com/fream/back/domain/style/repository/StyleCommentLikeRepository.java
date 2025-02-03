package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.entity.StyleCommentLike;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StyleCommentLikeRepository extends JpaRepository<StyleCommentLike, Long> {

    // 특정 댓글과 프로필로 좋아요 여부 확인
    Optional<StyleCommentLike> findByCommentAndProfile(StyleComment comment, Profile profile);

    // 특정 댓글 ID와 프로필 ID로 좋아요 여부 확인
    boolean existsByCommentIdAndProfileId(Long commentId, Long profileId);

    // 특정 댓글 ID로 좋아요 목록 조회
    List<StyleCommentLike> findByCommentId(Long commentId);
}
