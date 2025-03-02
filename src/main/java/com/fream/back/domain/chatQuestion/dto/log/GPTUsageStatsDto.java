package com.fream.back.domain.chatQuestion.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GPTUsageStatsDto {

    // 총 사용량 요약
    private int totalTokensUsed;
    private int estimatedCost; // 센트 단위 (계산식은 모델별로 다름)

    // 일별 사용량
    private List<DailyUsage> dailyUsage;

    // 모델별 사용량
    private Map<String, Integer> usageByModel;

    // 요청 유형별 사용량
    private Map<String, Integer> usageByRequestType;

    // 일별 사용량 정보를 담는 내부 클래스
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUsage {
        private LocalDate date;
        private int tokenCount;
    }
}
