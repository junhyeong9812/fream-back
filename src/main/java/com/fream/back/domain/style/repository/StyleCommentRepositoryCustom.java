package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.dto.StyleCommentResponseDto;
import com.fream.back.domain.style.entity.StyleComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface StyleCommentRepositoryCustom {
    // 특정 스타일의 댓글 목록을 DTO로 조회 (대댓글 포함)
    List<StyleCommentResponseDto> findCommentDtosByStyleId(Long styleId, String email);

    // 특정 댓글 ID 목록에 대해 사용자가 좋아요한 ID 집합 조회
    Set<Long> getLikedCommentIds(String email, List<Long> commentIds);
}