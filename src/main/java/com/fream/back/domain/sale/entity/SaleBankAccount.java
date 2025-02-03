package com.fream.back.domain.sale.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class SaleBankAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale; // 연결된 판매 정보

    private String bankName; // 은행명
    private String accountNumber; // 계좌 번호
    private String accountHolder; // 예금주 이름

    public SaleBankAccount(String bankName, String accountNumber, String accountHolder, Sale sale) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.sale = sale;
    }
    // 연관관계 편의 메서드
    public void assignSale(Sale sale) {
        this.sale = sale;
        if (sale != null && sale.getSaleBankAccount() != this) {
            sale.assignSaleBankAccount(this); // Sale에서 SaleBankAccount를 설정
        }
    }
}
