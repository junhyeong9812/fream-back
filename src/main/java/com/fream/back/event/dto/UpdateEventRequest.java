package com.fream.back.event.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateEventRequest {
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // 썸네일 수정 시 brandId 등 다른 필드가 필요한 경우 여기에 추가
    // 혹은 brand 변경을 허용하지 않는다면 제외
}