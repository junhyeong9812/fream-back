package com.fream.back.domain.chatQuestion.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 질문에 대한 응답 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponseDto {
    private String question;
    private String answer;
    private LocalDateTime createdAt;
}