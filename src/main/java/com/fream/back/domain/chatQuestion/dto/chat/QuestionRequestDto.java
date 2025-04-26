package com.fream.back.domain.chatQuestion.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 질문 요청 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequestDto {
    private String question;
}