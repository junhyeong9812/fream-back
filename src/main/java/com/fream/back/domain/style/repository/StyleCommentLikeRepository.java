package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.entity.StyleCommentLike;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StyleCommentLikeRepository extends JpaRepository<StyleCommentLike, Long> {

    /**
     * 특정 댓글과 프로필로 좋아요 여부 조회
     */
    Optional<StyleCommentLike> findByCommentAndProfile(StyleComment comment, Profile profile);

    /**
     * 특정 댓글 ID와 프로필 ID로 좋아요 여부 직접 조회 (최적화)
     */
    @Query("SELECT scl FROM StyleCommentLike scl WHERE scl.comment.id = :commentId AND scl.profile.id = :profileId")
    Optional<StyleCommentLike> findByCommentIdAndProfileId(
            @Param("commentId") Long commentId,
            @Param("profileId") Long profileId);

    /**
     * 특정 댓글 ID와 프로필 ID로 좋아요 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(scl) > 0 THEN true ELSE false END FROM StyleCommentLike scl " +
            "WHERE scl.comment.id = :commentId AND scl.profile.id = :profileId")
    boolean existsByCommentIdAndProfileId(
            @Param("commentId") Long commentId,
            @Param("profileId") Long profileId);

    /**
     * 특정 댓글 ID로 좋아요 목록 조회
     */
    @Query("SELECT scl FROM StyleCommentLike scl WHERE scl.comment.id = :commentId")
    List<StyleCommentLike> findByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 프로필이 좋아요한 댓글 ID 목록 조회 (최적화)
     */
    @Query("SELECT scl FROM StyleCommentLike scl WHERE scl.profile.id = :profileId AND scl.comment.id IN :commentIds")
    List<StyleCommentLike> findByProfileIdAndCommentIdIn(
            @Param("profileId") Long profileId,
            @Param("commentIds") List<Long> commentIds);

    /**
     * 특정 프로필이 좋아요한 댓글 ID Set 조회 (스타일 엔티티 조회 최소화)
     */
    @Query("SELECT scl.comment.id FROM StyleCommentLike scl WHERE scl.profile.id = :profileId AND scl.comment.id IN :commentIds")
    Set<Long> findCommentIdsByProfileIdAndCommentIdIn(
            @Param("profileId") Long profileId,
            @Param("commentIds") List<Long> commentIds);

    /**
     * 특정 댓글의 좋아요 수 계산 (count 쿼리 최적화)
     */
    @Query("SELECT COUNT(scl) FROM StyleCommentLike scl WHERE scl.comment.id = :commentId")
    Long countByCommentId(@Param("commentId") Long commentId);

    /**
     * 특정 프로필의 특정 댓글 좋아요 삭제 (벌크 연산 최적화)
     */
    @Modifying
    @Query("DELETE FROM StyleCommentLike scl WHERE scl.profile.id = :profileId AND scl.comment.id = :commentId")
    void deleteByProfileIdAndCommentId(
            @Param("profileId") Long profileId,
            @Param("commentId") Long commentId);
}