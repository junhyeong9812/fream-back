package com.fream.back.domain.chatQuestion.entity;

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
public class GPTUsageLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;  // 사용자 ID (null=관리자/시스템, user 모듈 결합 제거)

    @Column(nullable = false)
    private String requestType;  // 요청 유형 (채팅 응답, 데이터 분석 등)

    @Column(nullable = false)
    private int promptTokens;  // 입력 토큰 수

    @Column(nullable = false)
    private int completionTokens;  // 출력 토큰 수

    @Column(nullable = false)
    private int totalTokens;  // 총 토큰 수

    @Column
    private String modelName;  // 사용된 모델명

    // requestId를 추가하여 GPT API 요청 ID를 기록할 수 있음
    @Column
    private String requestId;

    // 특정 채팅 질문과 연결 (선택적)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_question_id")
    private ChatQuestion chatQuestion;
}