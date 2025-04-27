package com.fream.back.domain.faq.entity;

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
public class FAQImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl; // 이미지 파일명 (예: "thumbnail_abc123.jpg")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faq_id")
    private FAQ faq; // FAQ와의 관계

    /**
     * 이미지의 전체 URL 경로 반환
     * @return 전체 URL 경로 (예: "/api/faq/files/10/thumbnail_abc123.jpg")
     */
    public String getFullImageUrl() {
        if (faq == null) {
            return imageUrl;
        }
        return faq.getImageUrlPath(imageUrl);
    }
}