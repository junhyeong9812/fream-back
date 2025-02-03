package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    //추천인 유무 확인
    Optional<User> findByReferralCode(String referralCode);

    //이메일로 사용자를 검색
    Optional<User> findByEmail(String email);
    
    //휴대폰 번호로 사용자를 검색
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    //이메일과 휴대전화번호로 사용자를 검색
    Optional<User> findByEmailAndPhoneNumber(String email, String phoneNumber);

    boolean existsByEmail(String email);
}


