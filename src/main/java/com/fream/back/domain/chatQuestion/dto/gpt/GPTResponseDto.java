package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GPT API 응답 정보를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPTResponseDto {
    private String id;              // 응답 ID
    private String object;          // 객체 타입
    private long created;           // 생성 타임스탬프
    private String model;           // 사용된 모델명
    private List<GPTChoiceDto> choices;  // 선택지 목록
    private GPTUsageDto usage;      // 토큰 사용량 정보

    /**
     * GPT 응답에서 실제 답변 내용 추출
     *
     * @return 첫 번째 선택지의 메시지 내용, 없을 경우 오류 메시지
     */
    public String getAnswer() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().getContent();
        }
        return "응답을 처리하는 중 오류가 발생했습니다.";
    }
}