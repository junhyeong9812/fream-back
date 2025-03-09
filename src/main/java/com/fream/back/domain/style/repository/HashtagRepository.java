package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.Hashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    // 이름으로 해시태그 조회
    Optional<Hashtag> findByName(String name);

    // 이름이 특정 접두사로 시작하는 해시태그 조회 (자동완성 기능)
    List<Hashtag> findByNameStartingWithOrderByCountDesc(String prefix, Pageable pageable);

    // 한글 포함 검색을 위한 추가 메서드 (JPQL 또는 네이티브 쿼리)
    @Query("SELECT h FROM Hashtag h WHERE FUNCTION('LOWER', h.name) LIKE LOWER(CONCAT(:keyword, '%')) OR " +
            "FUNCTION('LOWER', h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY h.count DESC")
    List<Hashtag> findByNameContainingKeywordOrderByCountDesc(@Param("keyword") String keyword, Pageable pageable);

    // 사용 빈도순으로 인기 해시태그 조회
    Page<Hashtag> findAllByOrderByCountDesc(Pageable pageable);

    // 이름과 사용 빈도로 해시태그 검색
    Page<Hashtag> findByNameContainingOrderByCountDesc(String keyword, Pageable pageable);

    // 존재 여부 확인
    boolean existsByName(String name);
}