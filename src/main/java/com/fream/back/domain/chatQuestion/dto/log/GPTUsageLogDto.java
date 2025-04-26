package com.fream.back.domain.chatQuestion.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPT 사용량 로그 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GPTUsageLogDto {
    private Long id;
    private String userName; // 사용자 이름 또는 이메일
    private String requestType;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;
    private String modelName;
    private String requestDate;
    private String questionContent; // 질문 내용 요약 (선택적)
}