package com.fream.back.domain.event.repository;


import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.product.entity.Brand;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByBrand(Brand brand); // 특정 브랜드의 이벤트 조회
    List<Event> findByStartDateBeforeAndEndDateAfter(LocalDateTime startDate, LocalDateTime endDate); // 활성 이벤트 조회

    // 브랜드 정보와 함께 모든 이벤트 조회 (페치 조인)
    @Query("SELECT e FROM Event e JOIN FETCH e.brand")
    List<Event> findAllWithBrand();

    // 브랜드 정보와 함께 모든 이벤트 조회 (페이징)
    @Query("SELECT e FROM Event e JOIN e.brand")
    Page<Event> findAllWithBrandPaging(Pageable pageable);

    // 단일 이벤트 조회 (브랜드 정보 포함)
    @Query("SELECT e FROM Event e JOIN FETCH e.brand WHERE e.id = :eventId")
    Optional<Event> findByIdWithBrand(@Param("eventId") Long eventId);

    // 활성화된 이벤트를 브랜드 정보와 함께 조회
    @Query("SELECT e FROM Event e JOIN FETCH e.brand WHERE e.startDate <= :now AND e.endDate >= :now")
    List<Event> findActiveEventsWithBrand(@Param("now") LocalDateTime now);

    // 활성화된 이벤트를 브랜드 정보와 함께 조회 (페이징)
    @Query("SELECT e FROM Event e JOIN e.brand WHERE e.startDate <= :now AND e.endDate >= :now")
    Page<Event> findActiveEventsWithBrandPaging(@Param("now") LocalDateTime now, Pageable pageable);

    // 특정 브랜드의 이벤트를 조회 (페치 조인으로 브랜드 정보 포함)
    @Query("SELECT e FROM Event e JOIN FETCH e.brand WHERE e.brand.id = :brandId")
    List<Event> findByBrandIdWithBrand(@Param("brandId") Long brandId);

    // 특정 브랜드의 이벤트를 조회 (페이징)
    @Query("SELECT e FROM Event e JOIN e.brand WHERE e.brand.id = :brandId")
    Page<Event> findByBrandIdWithBrandPaging(@Param("brandId") Long brandId, Pageable pageable);

    // 이벤트와 심플 이미지까지 모두 함께 조회 (다중 페치 조인)
    @Query("SELECT DISTINCT e FROM Event e JOIN FETCH e.brand LEFT JOIN FETCH e.simpleImages WHERE e.id = :eventId")
    Optional<Event> findByIdWithBrandAndSimpleImages(@Param("eventId") Long eventId);
}