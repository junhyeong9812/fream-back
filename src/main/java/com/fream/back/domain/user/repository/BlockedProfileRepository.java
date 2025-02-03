package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.BlockedProfile;
import com.fream.back.domain.user.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockedProfileRepository extends JpaRepository<BlockedProfile, Long> {

    // 특정 프로필에 의해 차단된 프로필 목록 조회
    List<BlockedProfile> findByProfile(Profile profile);

    // 특정 프로필이 차단한 특정 프로필 조회
    Optional<BlockedProfile> findByProfileAndBlockedProfileId(Profile profile, Long blockedProfileId);

    // 특정 프로필이 차단한 모든 차단 프로필 ID 조회
    List<Long> findBlockedProfileIdsByProfile(Profile profile);

    //블럭된 유저의 프로필을 목록으로 조회
    @Query("SELECT bp FROM BlockedProfile bp JOIN FETCH bp.blockedProfile WHERE bp.profile = :profile")
    List<BlockedProfile> findAllByProfileWithBlocked(@Param("profile") Profile profile);

    //이미 차단이 됬는 지 확인하는 로직
    Optional<BlockedProfile> findByProfileAndBlockedProfile(Profile profile, Profile blockedProfile);

    //이메일을 통해 유저의 프로필을 조회하여 정보가져오기
    @Query("SELECT bp FROM BlockedProfile bp JOIN bp.profile p WHERE p.user.email = :email")
    List<BlockedProfile> findAllByProfileEmailWithBlocked(@Param("email") String email);
}

