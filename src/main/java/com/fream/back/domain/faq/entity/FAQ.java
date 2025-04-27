package com.fream.back.domain.faq.entity;

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
public class FAQ extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // FAQ ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FAQCategory category; // FAQ 카테고리 (ENUM)

    @Column(nullable = false, length = 100)
    private String question; // 질문 (Q)

    @Lob
    @Column(nullable = false)
    private String answer; // 답변 (A)

    public void update(FAQCategory category, String question, String answer) {
        this.category = category;
        this.question = question;
        this.answer = answer;
    }

    /**
     * FAQ 이미지 파일이 저장되는 디렉토리 경로 반환
     * @return 디렉토리 경로 (예: "faq/10")
     */
    public String getFileDirectory() {
        return "faq/" + this.id;
    }

    /**
     * 이미지 URL의 상대 경로를 절대 URL로 변환
     * @param fileName 파일명 (예: "thumbnail_abc123.jpg")
     * @return 절대 URL (예: "/api/faq/files/10/thumbnail_abc123.jpg")
     */
    public String getImageUrlPath(String fileName) {
        return "/api/faq/files/" + this.id + "/" + fileName;
    }
}