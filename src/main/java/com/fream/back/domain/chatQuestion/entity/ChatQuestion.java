package com.fream.back.domain.chatQuestion.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "user_id", nullable = false)
    private Long userId; // 질문 작성자 ID (user 모듈 결합 제거 — ID 참조)

    @Column(nullable = false)
    private Boolean isAnswered;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column
    private String clientIp;

    @PrePersist
    protected void onCreateAt() {
        this.createdAt = LocalDateTime.now();
    }

    // 답변 설정 메서드
    public void setAnswer(String answer) {
        this.answer = answer;
        this.isAnswered = true;
    }
}
