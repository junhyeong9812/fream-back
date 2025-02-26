package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import com.fream.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PointRepository extends JpaRepository<Point, Long> {

    // 사용자의 모든 포인트 내역 조회 (최근 적립 순)
    List<Point> findByUserOrderByCreatedDateDesc(User user);

    // 사용자의 사용 가능한 포인트 내역 조회 (유효기간 임박 순)
    List<Point> findByUserAndStatusOrderByExpirationDateAsc(User user, PointStatus status);

    // 특정 날짜 이전에 만료되는 사용 가능한 포인트 조회
    List<Point> findByStatusAndExpirationDateBefore(PointStatus status, LocalDate date);

    // 사용자의 총 사용가능 포인트 합계 조회
    @Query("SELECT SUM(p.remainingAmount) FROM Point p WHERE p.user = :user AND p.status = 'AVAILABLE'")
    Integer getTotalAvailablePoints(@Param("user") User user);

    // 사용자의 만료 예정 포인트 조회 (N일 이내 만료)
    @Query("SELECT p FROM Point p WHERE p.user = :user AND p.status = 'AVAILABLE' AND p.expirationDate BETWEEN :today AND :expirationDate ORDER BY p.expirationDate ASC")
    List<Point> findExpiringPoints(@Param("user") User user, @Param("today") LocalDate today, @Param("expirationDate") LocalDate expirationDate);
}