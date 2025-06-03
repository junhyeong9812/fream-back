package com.fream.back.domain.product.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "brand", indexes = {
        @Index(name = "idx_brand_name", columnList = "name")  // 브랜드명 검색 최적화
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 브랜드명

    // 필요한 필드만 변경
    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }
}
