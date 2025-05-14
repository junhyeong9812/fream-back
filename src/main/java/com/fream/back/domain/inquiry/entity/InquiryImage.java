package com.fream.back.domain.inquiry.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1대1 문의에 첨부된 이미지 엔티티
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "inquiry_images")
public class InquiryImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 이미지 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private Inquiry inquiry; // 연관된 문의

    @Column(nullable = false)
    private String imageUrl; // 이미지 URL (저장된 파일명)

    @Column
    private String originalFileName; // 원본 파일명

    @Column
    private String fileSize; // 파일 크기

    @Column
    private boolean isAnswer; // 답변에 포함된 이미지인지 여부 (true: 답변 이미지, false: 질문 이미지)

    // 연관관계 편의 메서드
    public void setInquiry(Inquiry inquiry) {
        this.inquiry = inquiry;
    }
}