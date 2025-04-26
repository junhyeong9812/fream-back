package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPT API 토큰 사용량 정보를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPTUsageDto {
    private int prompt_tokens;        // 입력(질문) 토큰 수
    private int completion_tokens;    // 출력(응답) 토큰 수
    private int total_tokens;         // 총 토큰 수
}