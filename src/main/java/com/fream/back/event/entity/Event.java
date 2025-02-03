package com.fream.back.event.entity;

import com.fream.back.domain.product.entity.Brand;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 이벤트 제목

    @Column(nullable = false)
    @Lob
    private String description; // 이벤트 설명

    @Column(nullable = false)
    private LocalDateTime startDate; // 이벤트 시작 날짜

    @Column(nullable = false)
    private LocalDateTime endDate; // 이벤트 종료 날짜

    private String thumbnailFileName; //이미지 파일명

    @Builder.Default
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SimpleImage> simpleImages = new ArrayList<>(); // 심플 이미지 목록

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand; // 연관된 브랜드

    // 편의 메서드
    public void addSimpleImage(SimpleImage simpleImage) {
        this.simpleImages.add(simpleImage);
        simpleImage.assignEvent(this);
    }

    public void updateThumbnailFileName(String newFileName) {
        this.thumbnailFileName = newFileName;
    }
    public void updateEvent(String title, String description, LocalDateTime startDate, LocalDateTime endDate) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (startDate != null) this.startDate = startDate;
        if (endDate != null) this.endDate = endDate;
    }
}
