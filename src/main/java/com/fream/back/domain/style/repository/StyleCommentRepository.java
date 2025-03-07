package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.StyleComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StyleCommentRepository extends JpaRepository<StyleComment, Long> {
    // 특정 스타일 ID로 연결된 루트 댓글 목록 조회 (대댓글 제외, 최신순)
    @Query("SELECT c FROM StyleComment c WHERE c.style.id = :styleId AND c.parentComment IS NULL ORDER BY c.createdDate DESC")
    Page<StyleComment> findRootCommentsByStyleId(@Param("styleId") Long styleId, Pageable pageable);

    // 특정 스타일의 총 댓글 수 조회 (대댓글 포함)
    @Query("SELECT COUNT(c) FROM StyleComment c WHERE c.style.id = :styleId")
    Long countCommentsByStyleId(@Param("styleId") Long styleId);

    // 특정 댓글의 대댓글 목록 조회 (최신순)
    @Query("SELECT c FROM StyleComment c WHERE c.parentComment.id = :parentCommentId ORDER BY c.createdDate DESC")
    List<StyleComment> findRepliesByParentCommentId(@Param("parentCommentId") Long parentCommentId);
    // 특정 스타일 ID로 연결된 댓글 목록 조회
    List<StyleComment> findByStyleId(Long styleId);

    // 특정 프로필 ID로 작성된 댓글 목록 조회
    List<StyleComment> findByProfileId(Long profileId);
}
