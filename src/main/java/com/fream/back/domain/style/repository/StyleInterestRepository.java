package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StyleInterestRepository extends JpaRepository<StyleInterest, Long> {
    // 특정 스타일 ID와 프로필 ID로 관심 등록 여부 확인
    boolean existsByStyleIdAndProfileId(Long styleId, Long profileId);

    // 특정 스타일 ID로 연결된 관심 목록 조회
    List<StyleInterest> findByStyleId(Long styleId);

    // 특정 스타일과 프로필로 관심 엔티티 조회
    Optional<StyleInterest> findByStyleAndProfile(Style style, Profile profile);

    // 특정 프로필이 관심 등록한 여러 스타일 조회 (배치 처리용)
    List<StyleInterest> findByProfileIdAndStyleIdIn(Long profileId, List<Long> styleIds);
}