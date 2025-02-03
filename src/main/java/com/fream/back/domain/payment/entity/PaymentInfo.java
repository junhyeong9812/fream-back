package com.fream.back.domain.payment.entity;

import com.fream.back.domain.user.entity.User;
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
public class PaymentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 결제 정보 소유 사용자

    private String cardNumber; // 카드 번호
    private String cardPassword; // 카드 비밀번호 앞 두 자리
    private String expirationDate; // 유효기간
    private String birthDate; // 생년월일

    // 편의 메서드
    public void assignUser(User user) {
        this.user = user;
    }

    public void unassignUser() {
        this.user = null;
    }

    @Builder
    public PaymentInfo(User user, String cardNumber, String cardPassword, String expirationDate, String birthDate) {
        this.user = user;
        this.cardNumber = cardNumber;
        this.cardPassword = cardPassword;
        this.expirationDate = expirationDate;
        this.birthDate = birthDate;
    }
}
