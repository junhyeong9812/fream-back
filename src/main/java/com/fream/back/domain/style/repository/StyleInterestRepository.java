package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StyleInterestRepository extends JpaRepository<StyleInterest, Long> {

    /**
     * 특정 스타일 ID와 프로필 ID로 관심 등록 여부 확인
     */
    @Query("SELECT CASE WHEN COUNT(si) > 0 THEN true ELSE false END FROM StyleInterest si " +
            "WHERE si.style.id = :styleId AND si.profile.id = :profileId")
    boolean existsByStyleIdAndProfileId(@Param("styleId") Long styleId, @Param("profileId") Long profileId);

    /**
     * 특정 스타일 ID로 관심 등록 목록 조회
     */
    List<StyleInterest> findByStyleId(Long styleId);

    /**
     * 특정 스타일과 프로필로 관심 등록 엔티티 조회
     */
    Optional<StyleInterest> findByStyleAndProfile(Style style, Profile profile);

    /**
     * 특정 스타일 ID와 프로필 ID로 관심 등록 엔티티 조회 (최적화)
     */
    @Query("SELECT si FROM StyleInterest si WHERE si.style.id = :styleId AND si.profile.id = :profileId")
    Optional<StyleInterest> findByStyleIdAndProfileId(
            @Param("styleId") Long styleId,
            @Param("profileId") Long profileId);

    /**
     * 특정 프로필이 관심 등록한 여러 스타일 조회 (배치 처리용)
     */
    @Query("SELECT si FROM StyleInterest si WHERE si.profile.id = :profileId AND si.style.id IN :styleIds")
    List<StyleInterest> findByProfileIdAndStyleIdIn(
            @Param("profileId") Long profileId,
            @Param("styleIds") List<Long> styleIds);

    /**
     * 특정 프로필이 관심 등록한 스타일 ID 목록 조회 (스타일 엔티티 조회 최소화)
     */
    @Query("SELECT si.style.id FROM StyleInterest si WHERE si.profile.id = :profileId AND si.style.id IN :styleIds")
    Set<Long> findStyleIdsByProfileIdAndStyleIdIn(
            @Param("profileId") Long profileId,
            @Param("styleIds") List<Long> styleIds);

    /**
     * 특정 스타일의 관심 등록 수 계산
     */
    @Query("SELECT COUNT(si) FROM StyleInterest si WHERE si.style.id = :styleId")
    Long countByStyleId(@Param("styleId") Long styleId);

    /**
     * 특정 프로필과 스타일에 대한 관심 등록 삭제 (벌크 연산 최적화)
     */
    @Modifying
    @Query("DELETE FROM StyleInterest si WHERE si.profile.id = :profileId AND si.style.id = :styleId")
    void deleteByProfileIdAndStyleId(
            @Param("profileId") Long profileId,
            @Param("styleId") Long styleId);
}