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
public class BankAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 정산 계좌 소유 사용자

    private String bankName; // 은행명
    private String accountNumber; // 계좌 번호
    private String accountHolder; // 예금주 이름

    // **편의 메서드 - 값 업데이트**
    public void updateBankAccount(String bankName, String accountNumber, String accountHolder) {
        if (bankName != null) {
            this.bankName = bankName;
        }
        if (accountNumber != null) {
            this.accountNumber = accountNumber;
        }
        if (accountHolder != null) {
            this.accountHolder = accountHolder;
        }
    }
    // 편의 메서드
    public void assignUser(User user) {
        this.user = user;
    }

    public void unassignUser() {
        this.user = null;
    }
}
