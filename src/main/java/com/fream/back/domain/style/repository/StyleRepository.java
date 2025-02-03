package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.Style;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StyleRepository extends JpaRepository<Style, Long>, StyleRepositoryCustom {
    // 특정 프로필 ID로 스타일 목록 조회
    List<Style> findByProfileId(Long profileId);


}
