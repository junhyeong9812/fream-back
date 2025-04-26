package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * GPT API 요청 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GPTRequestDto {
    private String model;
    private List<GPTMessageDto> messages;
    private double temperature;
    private int max_tokens;

    /**
     * FAQ 데이터만 기반으로 답변하는 GPTRequestDto 생성
     *
     * @param model GPT 모델명
     * @param prompt 사용자 질문
     * @param faqData FAQ 데이터 목록
     * @return GPTRequestDto 객체
     */
    public static GPTRequestDto of(String model, String prompt, List<String> faqData) {
        return of(model, prompt, faqData, false);
    }

    /**
     * FAQ 데이터 기반으로 답변하는 GPTRequestDto 생성 (알 수 없는 질문에 대한 응답 옵션 포함)
     *
     * @param model GPT 모델명
     * @param prompt 사용자 질문
     * @param faqData FAQ 데이터 목록
     * @param answerUnknownQuestions FAQ에 없는 질문에도 답변할지 여부
     * @return GPTRequestDto 객체
     */
    public static GPTRequestDto of(String model, String prompt, List<String> faqData, boolean answerUnknownQuestions) {
        List<GPTMessageDto> messages = new ArrayList<>();

        // 시스템 메시지로 역할과 FAQ 데이터를 설정
        StringBuilder systemContent = new StringBuilder();
        systemContent.append("당신은 온라인 쇼핑몰 상담 도우미입니다. 다음 FAQ 데이터를 기반으로 사용자의 질문에 답변해주세요.\n\n");
        systemContent.append("FAQ 데이터:\n");

        for (String faq : faqData) {
            systemContent.append(faq).append("\n");
        }

        systemContent.append("\n");

        // FAQ에 없는 질문에 대한 응답 방식 설정
        if (answerUnknownQuestions) {
            systemContent.append("FAQ에서 직접적인 답변을 찾을 수 없는 질문에 대해서는 일반적인 지식을 활용하여 도움이 될 만한 정보를 제공해주세요. ");
            systemContent.append("단, 답변 시작 부분에 '해당 질문에 대한 정확한 정보는 FAQ에 없지만, 제가 알고 있는 정보로는...'라고 언급해주세요. ");
            systemContent.append("답변을 확신할 수 없는 경우에는 불확실성을 표현하고, 고객센터 문의를 안내해주세요.\n");
        } else {
            systemContent.append("관련 FAQ가 없는 질문에는 '죄송합니다만, 해당 질문에 대한 정보가 없습니다. 고객센터로 문의해주세요.'라고 응답해주세요.\n");
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