package com.fream.back.domain.accessLog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 일자별 카운트를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyAccessCountDto {
    // 예: "2025-02-01"
    private String dateString;
    // 해당 날짜의 접속자 수
    private long count;
}
