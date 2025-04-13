package com.fream.back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserGradeDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeRequestDto {
        private Integer level;
        private String name;
        private String description;
        private Integer minPurchaseAmount;
        private Double pointRate;
        private String benefits;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeResponseDto {
        private Long id;
        private Integer level;
        private String name;
        private String description;
        private Integer minPurchaseAmount;
        private Double pointRate;
        private String benefits;
        private Long userCount;
        private LocalDateTime createdDate;
        private LocalDateTime updatedDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeUpdateRequestDto {
        private String name;
        private String description;
        private Integer minPurchaseAmount;
        private Double pointRate;
        private String benefits;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeStatisticsDto {
        private Long id;
        private Integer level;
        private String name;
        private Long userCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AutoAssignResultDto {
        private Integer processed;
        private Integer updated;
    }
}