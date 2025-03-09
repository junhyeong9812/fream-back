package com.fream.back.domain.style.entity;

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
@Table(indexes = {
        @Index(name = "idx_hashtag_name", columnList = "name", unique = true),
        @Index(name = "idx_hashtag_count", columnList = "count DESC")
})
public class Hashtag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // 해시태그 이름 (# 제외)

    @Column
    @Builder.Default
    private Long count = 0L; // 사용 횟수

    // 사용 횟수 증가
    public void incrementCount() {
        this.count++;
    }

    // 사용 횟수 감소
    public void decrementCount() {
        if (this.count > 0) {
            this.count--;
        }
    }
}