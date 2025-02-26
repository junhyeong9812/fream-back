package com.fream.back.domain.user.dto;

import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class PointDto {

    // 포인트 적립 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddPointRequest {
        private int amount;
        private String reason;
    }

    // 포인트 사용 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsePointRequest {
        private int amount;
        private String reason;
    }

    // 포인트 내역 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointResponse {
        private Long id;
        private int amount;
        private int remainingAmount;
        private String reason;
        private LocalDate expirationDate;
        private PointStatus status;
        private LocalDateTime createdDate;

        // Point 엔티티를 DTO로 변환하는 정적 메서드
        public static PointResponse from(Point point) {
            return PointResponse.builder()
                    .id(point.getId())
                    .amount(point.getAmount())
                    .remainingAmount(point.getRemainingAmount())
                    .reason(point.getReason())
                    .expirationDate(point.getExpirationDate())
                    .status(point.getStatus())
                    .createdDate(point.getCreatedDate())
                    .build();
        }
    }

    // 포인트 합계 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PointSummaryResponse {
        private int totalAvailablePoints;
        private List<PointResponse> pointDetails;
        private List<PointResponse> expiringPoints; // 만료 예정 포인트 (30일 이내)
    }

    // 포인트 사용 응답 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsePointResponse {
        private int usedPoints;
        private int remainingTotalPoints;
        private String message;
    }
}