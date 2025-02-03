package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



public interface NoticeRepository extends JpaRepository<Notice, Long>,NoticeRepositoryCustom {
    // 단일 조회: Notice와 NoticeImage를 조인하여 조회
    @Query("""
        SELECT DISTINCT n
        FROM Notice n
        LEFT JOIN NoticeImage ni ON ni.notice.id = n.id
        WHERE n.id = :id
    """)
    Notice findByIdWithImages(@Param("id") Long id);

    // 페이징 처리: Notice와 NoticeImage를 조인하여 조회
    @Query("""
        SELECT DISTINCT n
        FROM Notice n
        LEFT JOIN NoticeImage ni ON ni.notice.id = n.id
    """)
    Page<Notice> findAllWithPaging(Pageable pageable);

    @Query("SELECT n FROM Notice n WHERE n.category = :category ORDER BY n.createdDate DESC")
    Page<Notice> findByCategory(@Param("category") NoticeCategory category, Pageable pageable);
}