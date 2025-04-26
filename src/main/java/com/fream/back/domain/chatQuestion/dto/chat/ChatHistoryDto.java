package com.fream.back.domain.chatQuestion.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 기록 정보를 담는 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryDto {
    private Long id;
    private String question;
    private String answer;
    private LocalDateTime createdAt;
}