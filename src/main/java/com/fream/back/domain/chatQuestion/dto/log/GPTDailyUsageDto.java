package com.fream.back.domain.chatQuestion.dto.log;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일별 GPT 토큰 사용량 정보를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GPTDailyUsageDto {
    private String date;     // 문자열 형태의 날짜
    private Integer tokenCount;

    /**
     * LocalDate로 날짜를 받는 생성자
     * @param date 날짜
     * @param tokenCount 토큰 수
     */
    public GPTDailyUsageDto(LocalDate date, Integer tokenCount) {
        this.date = date != null ? date.toString() : null;
        this.tokenCount = tokenCount;
    }

    /**
     * 문자열 날짜를 LocalDate로 변환하여 반환
     * @return 변환된 LocalDate
     */
    public LocalDate getDateAsLocalDate() {
        return date != null ? LocalDate.parse(date) : null;
    }
}