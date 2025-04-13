package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.SanctionStatus;
import com.fream.back.domain.user.entity.SanctionType;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.entity.UserSanction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Long>, JpaSpecificationExecutor<UserSanction> {

    // 특정 사용자의 모든 제재 내역 조회
    List<UserSanction> findByUserOrderByCreatedDateDesc(User user);

    // 특정 사용자의 활성 상태 제재 조회
    List<UserSanction> findByUserAndStatusOrderByStartDateDesc(User user, SanctionStatus status);

    // 특정 상태의 제재 내역 조회
    Page<UserSanction> findByStatus(SanctionStatus status, Pageable pageable);

    // 특정 유형의 제재 내역 조회
    Page<UserSanction> findByType(SanctionType type, Pageable pageable);

    // 만료되어야 할 제재 조회 (현재 시간 이후에 종료되는 활성 제재)
    @Query("SELECT s FROM UserSanction s WHERE s.status = 'ACTIVE' AND s.endDate IS NOT NULL AND s.endDate <= :now")
    List<UserSanction> findExpirableSanctions(@Param("now") LocalDateTime now);

    // 특정 기간에 생성된 제재 조회
    Page<UserSanction> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // 특정 사용자 이메일로 제재 조회
    @Query("SELECT s FROM UserSanction s JOIN s.user u WHERE u.email = :email")
    Page<UserSanction> findByUserEmail(@Param("email") String email, Pageable pageable);

    // 제재 통계 조회
    @Query("SELECT COUNT(s) FROM UserSanction s WHERE s.status = 'ACTIVE'")
    long countActiveSanctions();

    @Query("SELECT COUNT(s) FROM UserSanction s WHERE s.status = 'EXPIRED'")
    long countExpiredSanctions();

    @Query("SELECT COUNT(s) FROM UserSanction s WHERE s.status = 'PENDING'")
    long countPendingSanctions();

    @Query("SELECT COUNT(s) FROM UserSanction s")
    long countTotalSanctions();
}