package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.StyleHashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StyleHashtagRepository extends JpaRepository<StyleHashtag, Long> {
    // 특정 스타일의 해시태그 조회
    List<StyleHashtag> findByStyleId(Long styleId);

    // 여러 스타일의 해시태그 조회
    List<StyleHashtag> findByStyleIdIn(List<Long> styleIds);

    // 특정 해시태그의 스타일 ID 페이징 조회
    @Query("SELECT sh.style.id FROM StyleHashtag sh WHERE sh.hashtag.id = :hashtagId")
    Page<Long> findStyleIdsByHashtagId(@Param("hashtagId") Long hashtagId, Pageable pageable);

    // 특정 스타일의 스타일-해시태그 연결 삭제
    void deleteByStyleId(Long styleId);

    // 특정 스타일, 해시태그 쌍으로 StyleHashtag 조회
    Optional<StyleHashtag> findByStyleIdAndHashtagId(Long styleId, Long hashtagId);

    // 특정 해시태그의 연결 개수 조회
    long countByHashtagId(Long hashtagId);
}