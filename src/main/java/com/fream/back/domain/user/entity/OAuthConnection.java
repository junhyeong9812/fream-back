package com.fream.back.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class  OAuthConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String provider;     // "google", "naver" 등

    @Column(nullable = false)
    private String providerId;   // 제공자별 고유 ID

    private String accessToken;  // (선택) 액세스 토큰 저장

    private LocalDateTime connectedAt;  // 연결 시간

    // 연관관계 편의 메서드
    public void setUser(User user) {
        this.user = user;
    }
}
