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

    private int amount; // 초기 포인트 금액

    private int remainingAmount; // 남은 포인트 금액

    private String reason; // 포인트 적립/사용 이유

    private LocalDate expirationDate; // 포인트 유효기간

    @Enumerated(EnumType.STRING)
    private PointStatus status; // 포인트 상태 (사용가능/사용완료/포인트소멸)

    // 기본 생성자 업데이트
    public Point(User user, int amount, String reason) {
        this.user = user;
        this.amount = amount;
        this.remainingAmount = amount; // 초기 남은 금액은 전체 금액
        this.reason = reason;
        this.expirationDate = LocalDate.now().plusDays(90); // 생성 시점부터 90일 후
        this.status = PointStatus.AVAILABLE; // 기본 상태는 사용가능
    }

    // 편의 메서드
    public void assignUser(User user) {
        this.user = user;
    }

    public void unassignUser() {
        this.user = null;
    }

    // 포인트 사용 메서드
    public int use(int amountToUse) {
        if (this.status != PointStatus.AVAILABLE) {
            return 0; // 사용 불가능한 포인트는 0 반환
        }

        if (amountToUse >= this.remainingAmount) {
            int used = this.remainingAmount;
            this.remainingAmount = 0;
            this.status = PointStatus.USED; // 전부 사용 완료
            return used;
        } else {
            this.remainingAmount -= amountToUse;
            return amountToUse;
        }
    }

    // 포인트 소멸 처리 메서드
    public void expire() {
        if (this.status == PointStatus.AVAILABLE) {
            this.status = PointStatus.EXPIRED;
            this.remainingAmount = 0;
        }
    }

    // 포인트 사용 이유 설정
    public void setReason(String reason) {
        this.reason = reason;
    }

    // 포인트 유효성 체크
    public boolean isValid() {
        return this.status == PointStatus.AVAILABLE &&
                expirationDate.isAfter(LocalDate.now());
    }
    /**
     * 포인트 만료일 설정
     */
    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    /**
     * 포인트 상태 설정
     */
    public void setStatus(PointStatus status) {
        this.status = status;
    }
}