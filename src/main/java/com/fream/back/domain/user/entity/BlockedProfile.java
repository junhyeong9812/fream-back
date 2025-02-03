package com.fream.back.domain.user.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockedProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile; // 차단을 설정한 프로필

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_profile_id", nullable = false)
    private Profile blockedProfile; // 차단된 프로필

    // 연관관계 메서드
    public void assignProfile(Profile profile) {
        this.profile = profile;
    }

    public void assignBlockedProfile(Profile blockedProfile) {
        this.blockedProfile = blockedProfile;
    }
}

