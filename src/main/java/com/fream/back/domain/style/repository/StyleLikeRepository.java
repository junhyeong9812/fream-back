package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleLike;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StyleLikeRepository extends JpaRepository<StyleLike, Long> {
    // 특정 스타일과 프로필로 좋아요 여부 확인
    Optional<StyleLike> findByStyleAndProfile(Style style, Profile profile);

    // 특정 스타일 ID와 프로필 ID로 좋아요 여부 확인
    boolean existsByStyleIdAndProfileId(Long styleId, Long profileId);

    // 특정 스타일 ID로 좋아요 목록 조회
    List<StyleLike> findByStyleId(Long styleId);
    // 특정 프로필의 특정 스타일에 대한 좋아요 조회
    Optional<StyleLike> findByProfileIdAndStyleId(Long profileId, Long styleId);

    // 특정 프로필의 특정 스타일에 대한 좋아요 존재 여부 확인
    boolean existsByProfileIdAndStyleId(Long profileId, Long styleId);

    // 특정 프로필이 좋아요한 여러 스타일 조회
    List<StyleLike> findByProfileIdAndStyleIdIn(Long profileId, List<Long> styleIds);

    // 특정 스타일의 좋아요 수 계산
    Long countByStyleId(Long styleId);

    // 특정 프로필 & 스타일에 대한 좋아요 삭제
    void deleteByProfileIdAndStyleId(Long profileId, Long styleId);
}
