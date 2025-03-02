package com.fream.back.domain.chatQuestion.entity;

import com.fream.back.domain.user.entity.User;
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
public class ChatQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)  // 로그인 사용자만 질문 가능하므로 nullable = false
    private User user;

    @Column(nullable = false)
    private Boolean isAnswered;

    @Column
    private String clientIp;

    // 답변 설정 메서드
    public void setAnswer(String answer) {
        this.answer = answer;
        this.isAnswered = true;
    }
}
