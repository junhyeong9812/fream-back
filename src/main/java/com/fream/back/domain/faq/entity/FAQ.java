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

    @Column(nullable = false)
    private String question; // 질문 (Q)

    @Lob
    @Column(nullable = false)
    private String answer; // 답변 (A)

    public void update(FAQCategory category, String question, String answer) {
        this.category = category;
        this.question = question;
        this.answer = answer;
    }
}
