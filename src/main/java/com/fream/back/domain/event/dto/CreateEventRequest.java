package com.fream.back.domain.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateEventRequest {
    @NotBlank(message = "이벤트 제목은 필수입니다.")
    @Size(min = 1, max = 100, message = "이벤트 제목은 1~100자 이내여야 합니다.")
    private String title;

    @NotBlank(message = "이벤트 설명은 필수입니다.")
    private String description;

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDateTime startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDateTime endDate;

    @NotNull(message = "브랜드ID는 필수입니다.")
    private Long brandId;
}