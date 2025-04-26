package com.fream.back.domain.event.repository;

import com.fream.back.domain.event.dto.EventSearchCondition;
import com.fream.back.domain.event.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 이벤트 레포지토리
 * 기본 CRUD 기능을 제공하는 JpaRepository와
 * 커스텀 메소드를 제공하는 EventRepositoryCustom을 상속
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    /**
     * 브랜드 ID로 이벤트 목록 조회 (시작일 기준 내림차순)
     */
    List<Event> findByBrandIdOrderByStartDateDesc(Long brandId);

    /**
     * 현재 활성화된 이벤트 목록 조회 (현재 시간이 시작일과 종료일 사이인 경우)
     */
    @Query("SELECT e FROM Event e WHERE e.startDate <= :now AND e.endDate > :now ORDER BY e.startDate DESC")
    List<Event> findActiveEvents(@Param("now") LocalDateTime now);

    /**
     * 시작일이 지났고 종료일이 지나지 않은 이벤트 조회 (스케줄러용)
     * (UPCOMING → ACTIVE 자동 변경 대상)
     */
    List<Event> findByStartDateBeforeAndEndDateAfter(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 종료일이 지난 이벤트 조회 (스케줄러용)
     * (ACTIVE → ENDED 자동 변경 대상)
     */
    List<Event> findByEndDateLessThanEqual(LocalDateTime endDate);

    /**
     * 예정된 이벤트 목록 조회 (시작일이 현재 이후인 경우, 시작일 기준 오름차순)
     */
    List<Event> findByStartDateAfterOrderByStartDateAsc(LocalDateTime now);

    /**
     * 종료된 이벤트 목록 조회 (종료일이 현재 이전인 경우, 종료일 기준 내림차순)
     */
    List<Event> findByEndDateBeforeOrderByEndDateDesc(LocalDateTime now);

    /**
     * 브랜드 정보와 함께 모든 이벤트 조회 (페치 조인)
     */
    @Query("SELECT e FROM Event e JOIN FETCH e.brand")
    List<Event> findAllWithBrand();

    /**
     * 브랜드 정보와 함께 모든 이벤트 조회 (페이징)
     */
    @Query("SELECT e FROM Event e JOIN e.brand")
    Page<Event> findAllWithBrandPaging(Pageable pageable);

    /**
     * 단일 이벤트 조회 (브랜드 정보 포함)
     */
    @Query("SELECT e FROM Event e JOIN FETCH e.brand WHERE e.id = :eventId")
    Optional<Event> findByIdWithBrand(@Param("eventId") Long eventId);

    /**
     * 활성화된 이벤트를 브랜드 정보와 함께 조회 (페이징)
     */
    @Query("SELECT e FROM Event e JOIN e.brand WHERE e.startDate <= :now AND e.endDate > :now")
    Page<Event> findActiveEventsWithBrandPaging(@Param("now") LocalDateTime now, Pageable pageable);

    /**
     * 특정 브랜드의 이벤트를 조회 (페치 조인으로 브랜드 정보 포함)
     */
    @Query("SELECT e FROM Event e JOIN FETCH e.brand WHERE e.brand.id = :brandId")
    List<Event> findByBrandIdWithBrand(@Param("brandId") Long brandId);

    /**
     * 특정 브랜드의 이벤트를 조회 (페이징)
     */
    @Query("SELECT e FROM Event e JOIN e.brand WHERE e.brand.id = :brandId")
    Page<Event> findByBrandIdWithBrandPaging(@Param("brandId") Long brandId, Pageable pageable);

    /**
     * 이벤트와 심플 이미지까지 모두 함께 조회 (다중 페치 조인)
     */
    @Query("SELECT DISTINCT e FROM Event e JOIN FETCH e.brand LEFT JOIN FETCH e.simpleImages WHERE e.id = :eventId")
    Optional<Event> findByIdWithBrandAndSimpleImages(@Param("eventId") Long eventId);
}