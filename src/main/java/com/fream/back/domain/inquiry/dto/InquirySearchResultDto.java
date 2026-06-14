package com.fream.back.domain.inquiry.dto;

import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.user.service.query.UserSummary;
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
     * QueryDSL Projections.constructor 전용 생성자 (문의 필드 + userId만, 10개).
     * user 모듈과의 조인을 제거했으므로 작성자 상세(email·profileName·name)는
     * 서비스에서 {@link #applyAuthor(UserSummary)}로 별도 enrich한다.
     */
    public InquirySearchResultDto(Long id, String title, String content, String answer,
                                  InquiryStatus status, InquiryCategory category, boolean isPrivate,
                                  LocalDateTime createdDate, LocalDateTime modifiedDate, Long userId) {
        this(id, title, content, answer, status, category, isPrivate, createdDate, modifiedDate,
                userId, null, null, null, List.of());
    }

    /**
     * user 모듈 요약 정보로 작성자 상세를 채운다(검색 결과 enrich).
     */
    public void applyAuthor(UserSummary author) {
        if (author == null) {
            return;
        }
        this.email = author.email();
        this.profileName = author.profileName();
        this.name = author.name();
    }

    /**
     * 엔티티에서 DTO 생성 (문의 + userId만, 작성자 상세는 {@link #applyAuthor(UserSummary)}로 enrich).
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
                .userId(inquiry.getUserId())
                .build();
    }

    /**
     * 이미지 URL 설정
     */
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}