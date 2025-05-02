package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleLike;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StyleLikeRepository extends JpaRepository<StyleLike, Long> {

    /**
     * 특정 스타일과 프로필로 좋아요 조회
     */
    Optional<StyleLike> findByStyleAndProfile(Style style, Profile profile);

    /**
     * 특정 스타일 ID와 프로필 ID로 좋아요 여부 확인 (최적화)
     */
    @Query("SELECT CASE WHEN COUNT(sl) > 0 THEN true ELSE false END FROM StyleLike sl " +
            "WHERE sl.style.id = :styleId AND sl.profile.id = :profileId")
    boolean existsByStyleIdAndProfileId(@Param("styleId") Long styleId, @Param("profileId") Long profileId);

    /**
     * 특정 스타일 ID로 좋아요 목록 조회
     */
    List<StyleLike> findByStyleId(Long styleId);

    /**
     * 특정 프로필의 특정 스타일에 대한 좋아요 조회 (ID 기반)
     */
    @Query("SELECT sl FROM StyleLike sl WHERE sl.profile.id = :profileId AND sl.style.id = :styleId")
    Optional<StyleLike> findByProfileIdAndStyleId(
            @Param("profileId") Long profileId,
            @Param("styleId") Long styleId);

    /**
     * 특정 프로필이 좋아요한 여러 스타일 조회 (ID 기반, IN 쿼리 최적화)
     */
    @Query("SELECT sl FROM StyleLike sl WHERE sl.profile.id = :profileId AND sl.style.id IN :styleIds")
    List<StyleLike> findByProfileIdAndStyleIdIn(
            @Param("profileId") Long profileId,
            @Param("styleIds") List<Long> styleIds);

    /**
     * 특정 프로필이 좋아요한 스타일 ID 목록 조회 (스타일 엔티티 조회 최소화)
     */
    @Query("SELECT sl.style.id FROM StyleLike sl WHERE sl.profile.id = :profileId AND sl.style.id IN :styleIds")
    Set<Long> findStyleIdsByProfileIdAndStyleIdIn(
            @Param("profileId") Long profileId,
            @Param("styleIds") List<Long> styleIds);

    /**
     * 특정 스타일의 좋아요 수 계산 (사용 시 count 쿼리 최적화)
     */
    @Query("SELECT COUNT(sl) FROM StyleLike sl WHERE sl.style.id = :styleId")
    Long countByStyleId(@Param("styleId") Long styleId);

    /**
     * 특정 프로필 & 스타일에 대한 좋아요 삭제 (벌크 연산 최적화)
     */
    @Modifying
    @Query("DELETE FROM StyleLike sl WHERE sl.profile.id = :profileId AND sl.style.id = :styleId")
    void deleteByProfileIdAndStyleId(
            @Param("profileId") Long profileId,
            @Param("styleId") Long styleId);
}