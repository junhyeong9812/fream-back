package com.fream.back.domain.chatQuestion.dto.gpt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPTResponseDto {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<GPTChoiceDto> choices;
    private GPTUsageDto usage;

    public String getAnswer() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).getMessage().getContent();
        }
        return "응답을 처리하는 중 오류가 발생했습니다.";
    }
}