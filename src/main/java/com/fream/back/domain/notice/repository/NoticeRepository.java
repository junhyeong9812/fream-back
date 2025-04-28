package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 공지사항 레포지토리
 */
public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeRepositoryCustom {

    /**
     * 이미지 정보와 함께 단일 공지사항 조회
     *
     * @param id 공지사항 ID
     * @return 공지사항 엔티티 (이미지 포함)
     */
    @EntityGraph(attributePaths = {"images"})
    Optional<Notice> findWithImagesById(Long id);

    /**
     * 카테고리별 공지사항 목록 조회
     *
     * @param category 공지사항 카테고리
     * @param pageable 페이징 정보
     * @return 공지사항 목록
     */
    Page<Notice> findByCategory(NoticeCategory category, Pageable pageable);

    /**
     * 최근 공지사항 목록 조회 (생성일 기준 내림차순)
     *
     * @param pageable 페이징 정보
     * @return 공지사항 목록
     */
    @Query("SELECT n FROM Notice n ORDER BY n.createdDate DESC")
    Page<Notice> findAllOrderByCreatedDateDesc(Pageable pageable);

    /**
     * 카테고리별 최근 공지사항 목록 조회 (생성일 기준 내림차순)
     *
     * @param category 공지사항 카테고리
     * @param pageable 페이징 정보
     * @return 공지사항 목록
     */
    @Query("SELECT n FROM Notice n WHERE n.category = :category ORDER BY n.createdDate DESC")
    Page<Notice> findByCategoryOrderByCreatedDateDesc(@Param("category") NoticeCategory category, Pageable pageable);
}