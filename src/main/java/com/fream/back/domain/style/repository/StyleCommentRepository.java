package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.StyleComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StyleCommentRepository extends JpaRepository<StyleComment, Long> {
    // 특정 스타일 ID로 연결된 댓글 목록 조회
    List<StyleComment> findByStyleId(Long styleId);

    // 특정 프로필 ID로 작성된 댓글 목록 조회
    List<StyleComment> findByProfileId(Long profileId);
}
