package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// 요청 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GPTRequestDto {
    private String model;
    private List<GPTMessageDto> messages;
    private double temperature;
    private int max_tokens;

    public static GPTRequestDto of(String model, String prompt, List<String> faqData) {
        List<GPTMessageDto> messages = new ArrayList<>();

        // 시스템 메시지로 역할과 FAQ 데이터를 설정
        StringBuilder systemContent = new StringBuilder();
        systemContent.append("당신은 온라인 쇼핑몰 상담 도우미입니다. 다음 FAQ 데이터를 기반으로 사용자의 질문에 답변해주세요. ");
        systemContent.append("관련 FAQ가 없는 질문에는 '죄송합니다만, 해당 질문에 대한 정보가 없습니다. 고객센터로 문의해주세요.'라고 응답해주세요.\n\n");
        systemContent.append("FAQ 데이터:\n");

        for (String faq : faqData) {
            systemContent.append(faq).append("\n");
        }

        messages.add(GPTMessageDto.builder()
                .role("system")
                .content(systemContent.toString())
                .build());

        // 사용자 질문 추가
        messages.add(GPTMessageDto.builder()
                .role("user")
                .content(prompt)
                .build());

        return GPTRequestDto.builder()
                .model(model)
                .messages(messages)
                .temperature(0.7)
                .max_tokens(800)
                .build();
    }
}

