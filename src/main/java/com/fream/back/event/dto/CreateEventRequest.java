package com.fream.back.event.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateEventRequest {
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long brandId; // brand 연관관계 식별

    // 기본 생성자, 필요 시 @Builder or @AllArgsConstructor
    // @NotBlank, @NotNull 등 Validation 어노테이션도 권장
}