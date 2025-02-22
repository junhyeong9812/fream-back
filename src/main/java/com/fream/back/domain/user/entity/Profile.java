package com.fream.back.domain.user.entity;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 프로필 소유 사용자

    @Column(nullable = false, unique = true)
    private String profileName; // 프로필 이름

    private String Name; // 프로필 이름

    private String bio; // 소개글
    private boolean isPublic; // 프로필 공개 여부
    private String profileImageUrl; // 프로필 이미지 URL

    @Builder.Default
    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followings = new ArrayList<>(); // 내가 팔로우한 프로필들

    @Builder.Default
    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Follow> followers = new ArrayList<>(); // 나를 팔로우한 프로필들

    @Builder.Default
    @OneToMany(mappedBy = "blockedProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BlockedProfile> blockedByProfiles = new ArrayList<>(); // 나를 차단한 프로필 목록

    @Builder.Default
    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Style> styles = new ArrayList<>();


    // **편의 메서드 - 값 업데이트**
    public void updateProfile(String profileName,String name ,String bio, Boolean isPublic, String profileImageUrl) {
        if (profileName != null) {
            this.profileName = profileName;
        }
        if (name != null) {
            this.Name = name;
        }
        if (bio != null) {
            this.bio = bio;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    // **연관관계 메서드**
    public void addFollower(Follow follow) {
        this.followers.add(follow);
        follow.addFollowing(this);
    }

    public void addFollowing(Follow follow) {
        this.followings.add(follow);
        follow.addFollower(this);
    }

    public void removeFollower(Follow follow) {
        this.followers.remove(follow);
        follow.addFollowing(null);
    }

    public void removeFollowing(Follow follow) {
        this.followings.remove(follow);
        follow.addFollower(null);
    }

    // 스타일 추가
    public void addStyle(Style style) {
        this.styles.add(style);
        style.assignProfile(this); // 양방향 동기화
    }
    public void addDataStyle(Style style) {
        this.styles.add(style);
//        style.assignProfile(this); // 양방향 동기화
    }

    // 스타일 제거
    public void removeStyle(Style style) {
        this.styles.remove(style);
        style.assignProfile(null); // 양방향 동기화 해제
    }


    // 차단된 프로필 추가
    public void addBlockedProfile(BlockedProfile blockedProfile) {
        this.blockedByProfiles.add(blockedProfile);
        blockedProfile.assignProfile(this);
    }

    // 차단된 프로필 삭제
    public void removeBlockedProfile(BlockedProfile blockedProfile) {
        this.blockedByProfiles.remove(blockedProfile);
        blockedProfile.assignProfile(null);
    }

    // 사용자 설정
    public void addUser(User user) {
        this.user = user;
        if (user != null && user.getProfile() != this) {
            user.addProfile(this);
        }
    }
}