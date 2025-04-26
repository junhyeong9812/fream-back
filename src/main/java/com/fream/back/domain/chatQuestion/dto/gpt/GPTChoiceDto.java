package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GPT API 응답 선택지 정보를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPTChoiceDto {
    private int index;                    // 선택지 인덱스
    private GPTMessageDto message;        // 선택지 메시지
    private String finish_reason;         // 응답 종료 이유 (stop, length, content_filter 등)
}