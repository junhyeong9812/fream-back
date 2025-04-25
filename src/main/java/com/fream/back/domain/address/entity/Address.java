package com.fream.back.domain.address.entity;

import com.fream.back.domain.user.entity.User;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_is_default", columnList = "is_default")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 주소록 소유 사용자

    @Column(nullable = false)
    private String recipientName; // 수령인 이름

    @Column(nullable = false)
    private String phoneNumber; // 전화번호

    @Column(nullable = false, length = 5)
    private String zipCode; // 우편번호

    @Column(nullable = false)
    private String address; // 주소

    private String detailedAddress; // 상세 주소

    @Column(name = "is_default")
    private boolean isDefault; // 기본 배송지 여부

    // **편의 메서드 - 값 업데이트**
    public void updateAddress(String recipientName, String phoneNumber, String zipCode,
                              String address, String detailedAddress, Boolean isDefault) {
        if (recipientName != null) {
            this.recipientName = recipientName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
        if (zipCode != null) {
            this.zipCode = zipCode;
        }
        if (address != null) {
            this.address = address;
        }
        if (detailedAddress != null) {
            this.detailedAddress = detailedAddress;
        }
        if (isDefault != null) {
            this.isDefault = isDefault;
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