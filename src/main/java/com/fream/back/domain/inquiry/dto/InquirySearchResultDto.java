package com.fream.back.domain.inquiry.dto;

import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 1대1 문의 검색 결과 DTO
 * 문의 정보와 사용자 정보를 함께 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquirySearchResultDto {

    // 문의 정보
    private Long id;
    private String title;
    private String content;
    private String answer;
    private InquiryStatus status;
    private InquiryCategory category;
    private boolean isPrivate;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    // 사용자 정보
    private Long userId;
    private String email;
    private String profileName; // Profile.profileName
    private String name;        // Profile.Name

    // 이미지 URL 리스트 (별도 설정 필요)
    @Builder.Default
    private List<String> imageUrls = List.of();

    /**
     * 엔티티에서 DTO 생성 (기본 정보만 포함)
     */
    public static InquirySearchResultDto from(Inquiry inquiry) {
        return InquirySearchResultDto.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus())
                .category(inquiry.getCategory())
                .isPrivate(inquiry.isPrivate())
                .createdDate(inquiry.getCreatedDate())
                .modifiedDate(inquiry.getModifiedDate())
                .userId(inquiry.getUser().getId())
                .email(inquiry.getUser().getEmail())
                .profileName(inquiry.getUser().getProfile() != null ?
                        inquiry.getUser().getProfile().getProfileName() : null)
                .name(inquiry.getUser().getProfile() != null ?
                        inquiry.getUser().getProfile().getName() : null)
                .build();
    }

    /**
     * 이미지 URL 설정
     */
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}