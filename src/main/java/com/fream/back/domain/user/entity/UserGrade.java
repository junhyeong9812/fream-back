package com.fream.back.domain.user.entity;

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
@Table(name = "user_grades")
public class UserGrade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer level; // 등급 레벨 (1, 2, 3, ...)

    @Column(nullable = false, unique = true)
    private String name; // 등급명 (브론즈, 실버, 골드, ...)

    private String description; // 등급 설명

    private Integer minPurchaseAmount; // 최소 구매액 조건

    @Column(nullable = false)
    private Double pointRate; // 포인트 적립률 (%)

    private String benefits; // 혜택 설명

    @Builder.Default
    @OneToMany(mappedBy = "grade", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    // 편의 메서드 - 값 업데이트
    public void updateGrade(String name, String description, Integer minPurchaseAmount,
                            Double pointRate, String benefits) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        this.minPurchaseAmount = minPurchaseAmount;
        if (pointRate != null) {
            this.pointRate = pointRate;
        }
        if (benefits != null) {
            this.benefits = benefits;
        }
    }

    // 편의 메서드 - 사용자 추가
    public void addUser(User user) {
        this.users.add(user);
        user.addGrade(this);
    }

    // 편의 메서드 - 사용자 제거
    public void removeUser(User user) {
        this.users.remove(user);
        user.addGrade(null);
    }
}