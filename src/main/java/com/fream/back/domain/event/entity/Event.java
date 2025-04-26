package com.fream.back.domain.event.entity;

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

    /**
     * 이벤트 상태 필드 추가
     * DB에는 이벤트 상태가 직접 저장되지 않고 시작일/종료일로 계산됨
     */
    @Transient // DB에 저장되지 않는 필드로 지정
    private EventStatus status;

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

    /**
     * 이벤트가 현재 활성 상태인지 확인
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return startDate.isBefore(now) && endDate.isAfter(now);
    }

    /**
     * 이벤트가 아직 시작되지 않았는지 확인
     */
    public boolean isUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        return startDate.isAfter(now);
    }

    /**
     * 이벤트가 종료되었는지 확인
     */
    public boolean isEnded() {
        LocalDateTime now = LocalDateTime.now();
        return endDate.isBefore(now) || endDate.isEqual(now);
    }

    /**
     * 이벤트 상태 계산 및 반환
     * @return EventStatus 열거형 (UPCOMING, ACTIVE, ENDED)
     */
    public EventStatus getStatus() {
        if (isUpcoming()) {
            return EventStatus.UPCOMING;
        } else if (isActive()) {
            return EventStatus.ACTIVE;
        } else {
            return EventStatus.ENDED;
        }
    }

    /**
     * 이벤트 상태 문자열 반환 (표시용)
     * @return 상태 문자열 ("예정", "진행 중", "종료" 중 하나)
     */
    public String getStatusDisplayName() {
        return getStatus().getDisplayName();
    }
}