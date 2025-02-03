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
@Table(name = "follows")
public class Follow extends BaseTimeEntity { // BaseTimeEntity 상속

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_profile_id", nullable = false)
    private Profile follower; // 팔로우를 한 프로필

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_profile_id", nullable = false)
    private Profile following; // 팔로우된 프로필

    // **연관관계 메서드**

    // **연관관계 메서드**
    public void addFollower(Profile follower) {
        this.follower = follower;
    }

    public void addFollowing(Profile following) {
        this.following = following;
    }
}
