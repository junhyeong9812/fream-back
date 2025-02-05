package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleCommentLike;
import com.fream.back.domain.style.repository.StyleCommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleCommentLikeQueryService {

    private final StyleCommentLikeRepository styleCommentLikeRepository;


    // 특정 댓글 ID와 프로필 ID로 좋아요 여부 확인
    public boolean isCommentLikedByProfile(Long commentId, Long profileId) {
        return styleCommentLikeRepository.existsByCommentIdAndProfileId(commentId, profileId);
    }

    // 특정 댓글 ID로 연결된 좋아요 목록 조회
    public List<StyleCommentLike> findByCommentId(Long commentId) {
        return styleCommentLikeRepository.findByCommentId(commentId);
    }
}
