package com.fream.back.domain.chatQuestion.dto.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 모델별 GPT 토큰 사용량 정보를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPTModelUsageDto {
    private String modelName;
    private Integer tokenCount;
}