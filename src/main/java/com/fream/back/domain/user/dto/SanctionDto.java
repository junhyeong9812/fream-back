package com.fream.back.domain.user.dto;

import com.fream.back.domain.user.entity.SanctionStatus;
import com.fream.back.domain.user.entity.SanctionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SanctionDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionSearchRequestDto {
        private Long userId;
        private String email;
        private SanctionStatus status;
        private SanctionType type;
        private LocalDateTime startDateStart;
        private LocalDateTime startDateEnd;
        private LocalDateTime endDateStart;
        private LocalDateTime endDateEnd;
        private LocalDateTime createdDateStart;
        private LocalDateTime createdDateEnd;
        private String sortField;
        private String sortDirection;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionResponseDto {
        private Long id;
        private Long userId;
        private String userEmail;
        private String userProfileName;
        private Long targetId;
        private String targetType;
        private String reason;
        private String details;
        private SanctionType type;
        private SanctionStatus status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String approvedBy;
        private String rejectedBy;
        private String rejectionReason;
        private String createdBy;
        private LocalDateTime createdDate;
        private LocalDateTime updatedDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionCreateRequestDto {
        private Long userId;
        private Long targetId;
        private String targetType;
        private String reason;
        private String details;
        private SanctionType type;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionUpdateRequestDto {
        private String reason;
        private String details;
        private SanctionType type;
        private SanctionStatus status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionReviewRequestDto {
        private Boolean approved;
        private String rejectionReason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionStatisticsDto {
        private long total;
        private long active;
        private long expired;
        private long pending;
    }
}
