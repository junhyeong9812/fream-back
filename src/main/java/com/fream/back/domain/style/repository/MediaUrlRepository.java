package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.MediaUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaUrlRepository extends JpaRepository<MediaUrl, Long> {
    // 특정 Style ID로 MediaUrl 목록 조회
    List<MediaUrl> findByStyleId(Long styleId);

    // URL로 MediaUrl 목록 조회
    List<MediaUrl> findByUrl(String url);
}