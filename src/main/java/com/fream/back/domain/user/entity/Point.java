package com.fream.back.domain.user.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Point extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 포인트 소유 사용자

    private int amount; // 포인트 금액

    private LocalDate expirationDate; // 포인트 유효기간

    // 생성자에서 기본 유효기간 설정
    public Point(User user, int amount) {
        this.user = user;
        this.amount = amount;
        this.expirationDate = LocalDate.now().plusYears(1); // 생성 시점부터 1년 후
    }

    // 편의 메서드
    public void assignUser(User user) {
        this.user = user;
    }

    public void unassignUser() {
        this.user = null;
    }

    public boolean isValid() {
        return expirationDate.isAfter(LocalDate.now());
    }
}
