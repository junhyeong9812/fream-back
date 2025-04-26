package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPT API 요청/응답 메시지 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GPTMessageDto {
    private String role;    // system, user, assistant 중 하나
    private String content; // 메시지 내용
}