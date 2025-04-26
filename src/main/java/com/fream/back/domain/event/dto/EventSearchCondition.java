package com.fream.back.domain.event.dto;

import com.fream.back.domain.event.entity.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventSearchCondition {
    private String keyword;       // 검색어 (제목)
    private Long brandId;         // 브랜드 ID
    private Boolean isActive;     // 활성 상태 여부
    private EventStatus status;   // 이벤트 상태 (UPCOMING, ACTIVE, ENDED)
    private LocalDateTime startDate; // 시작일 필터
    private LocalDateTime endDate;   // 종료일 필터
}