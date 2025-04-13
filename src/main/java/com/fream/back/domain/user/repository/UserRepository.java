package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    //추천인 유무 확인
    Optional<User> findByReferralCode(String referralCode);

    //이메일로 사용자를 검색
    Optional<User> findByEmail(String email);
    
    //이메일과 휴대전화번호로 사용자를 검색
    Optional<User> findByEmailAndPhoneNumber(String email, String phoneNumber);

    boolean existsByEmail(String email);

    // CI로 사용자 조회 (중복 가입 방지)
    Optional<User> findByCi(String ci);

    // DI로 사용자 조회
    Optional<User> findByDi(String di);

    // 전화번호로 사용자 조회
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * 사용자 ID, 이메일, 전화번호, 역할으로 사용자 찾기
     */
    @Query("SELECT u FROM User u WHERE " +
            "(:id IS NULL OR u.id = :id) AND " +
            "(:email IS NULL OR u.email LIKE %:email%) AND " +
            "(:phoneNumber IS NULL OR u.phoneNumber LIKE %:phoneNumber%) AND " +
            "(:role IS NULL OR u.role = :role)")
    List<User> findByMultipleCriteria(@Param("id") Long id,
                                      @Param("email") String email,
                                      @Param("phoneNumber") String phoneNumber,
                                      @Param("role") Role role);

    /**
     * 여러 기준으로 사용자 검색 (더 복잡한 쿼리)
     */
    @Query("SELECT u FROM User u LEFT JOIN u.profile p WHERE " +
            "(:keyword IS NULL OR " +
            "   u.email LIKE %:keyword% OR " +
            "   u.phoneNumber LIKE %:keyword% OR " +
            "   p.profileName LIKE %:keyword%) AND " +
            "(:isVerified IS NULL OR u.isVerified = :isVerified) AND " +
            "(:isActive IS NULL OR u.isActive = :isActive) AND " +
            "(:gender IS NULL OR u.gender = :gender) AND " +
            "(:createdDateStart IS NULL OR u.createdDate >= :createdDateStart) AND " +
            "(:createdDateEnd IS NULL OR u.createdDate <= :createdDateEnd)")
    Page<User> searchUsers(@Param("keyword") String keyword,
                           @Param("isVerified") Boolean isVerified,
                           @Param("isActive") Boolean isActive,
                           @Param("gender") Gender gender,
                           @Param("createdDateStart") LocalDateTime createdDateStart,
                           @Param("createdDateEnd") LocalDateTime createdDateEnd,
                           Pageable pageable);

    /**
     * 등급별 사용자 수 조회
     */
    @Query("SELECT u.grade.id, COUNT(u) FROM User u WHERE u.grade IS NOT NULL GROUP BY u.grade.id")
    List<Object[]> countUsersByGrade();

    /**
     * 활성 상태 사용자 수 조회
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.isActive = true")
    long countActiveUsers();

    /**
     * 최근 N일 이내 가입한 사용자 조회
     */
    @Query("SELECT u FROM User u WHERE u.createdDate >= :since")
    List<User> findRecentlyJoinedUsers(@Param("since") LocalDateTime since);
}


