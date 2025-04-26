package com.fream.back.domain.event.dto;

import com.fream.back.domain.event.entity.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDetailDto {
    private Long id;                    // 이벤트 ID
    private String title;               // 이벤트 제목
    private String description;         // 이벤트 상세 설명
    private LocalDateTime startDate;    // 시작 날짜
    private LocalDateTime endDate;      // 종료 날짜
    private String thumbnailUrl;        // 썸네일 URL
    private List<String> simpleImageUrls; // 심플 이미지 URL 목록
    private Long brandId;               // 브랜드 ID
    private String brandName;           // 브랜드 이름
    private boolean isActive;           // 활성 상태 여부
    private EventStatus status;         // 이벤트 상태 (UPCOMING, ACTIVE, ENDED)
    private String statusDisplayName;   // 이벤트 상태 표시명 ("예정", "진행 중", "종료")
    private LocalDateTime createdDate;  // 생성일시
    private LocalDateTime modifiedDate; // 수정일시
}