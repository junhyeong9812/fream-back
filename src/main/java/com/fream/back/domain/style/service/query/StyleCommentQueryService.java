package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.repository.StyleCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleCommentQueryService {

    private final StyleCommentRepository styleCommentRepository;

    // 특정 스타일 ID로 연결된 모든 댓글 조회
    public List<StyleComment> findByStyleId(Long styleId) {
        return styleCommentRepository.findByStyleId(styleId);
    }

    // 댓글 ID로 조회
    public StyleComment findById(Long commentId) {
        return styleCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글을 찾을 수 없습니다: " + commentId));
    }

    // 특정 프로필 ID로 작성된 댓글 조회
    public List<StyleComment> findByProfileId(Long profileId) {
        return styleCommentRepository.findByProfileId(profileId);
    }
}

