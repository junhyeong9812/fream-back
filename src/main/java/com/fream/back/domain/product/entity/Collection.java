package com.fream.back.domain.product.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "collection", indexes = {
        @Index(name = "idx_collection_name", columnList = "name")  // 컬렉션명 검색 최적화
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collection extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 컬렉션명

    // 업데이트 메서드
    public void updateName(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }
}
