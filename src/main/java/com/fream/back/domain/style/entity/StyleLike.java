package com.fream.back.domain.style.entity;

import com.fream.back.domain.user.entity.Profile;
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
public class StyleLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style; // 좋아요 대상 Style

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile; // 좋아요 한 사용자
    
    //연관관계 메서드
    public void assignStyle(Style style) {
        this.style = style;
    }

    public void assignProfile(Profile profile) {
        this.profile = profile;
    }
}

