package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    @Query("SELECT p FROM Profile p JOIN FETCH p.user u WHERE u.email = :email")
    Optional<Profile> findByUserEmailWithFetchJoin(@Param("email") String email);// 사용자 이메일로 프로필 조회
}
