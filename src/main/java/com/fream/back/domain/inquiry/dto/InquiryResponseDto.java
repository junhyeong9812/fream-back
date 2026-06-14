package com.fream.back.domain.inquiry.dto;

import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryImage;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.user.service.query.UserSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 1대1 문의 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponseDto {

    private Long id;
    private String title;
    private String content;
    private String answer;
    private InquiryStatus status;
    private InquiryCategory category;
    private boolean isPrivate;
    private boolean pushNotification;
    private String answeredBy;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    // 사용자 정보
    private Long userId;
    private String userEmail;
    private String userProfileName; // Profile.profileName
    private String userName;        // Profile.Name

    // 이미지 URL 리스트
    @Builder.Default
    private List<String> questionImageUrls = new ArrayList<>();

    @Builder.Default
    private List<String> answerImageUrls = new ArrayList<>();

    /**
     * 엔티티와 이미지 리스트에서 DTO 생성.
     * 작성자 정보는 user 모듈의 {@link UserSummary}로 주입받는다(엔티티 직접 참조 제거).
     */
    public static InquiryResponseDto from(Inquiry inquiry, List<InquiryImage> images, UserSummary author) {
        // 질문 이미지와 답변 이미지 분리
        List<String> questionImages = images.stream()
                .filter(img -> !img.isAnswer())
                .map(InquiryImage::getImageUrl)
                .collect(Collectors.toList());

        List<String> answerImages = images.stream()
                .filter(InquiryImage::isAnswer)
                .map(InquiryImage::getImageUrl)
                .collect(Collectors.toList());

        return InquiryResponseDto.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus())
                .category(inquiry.getCategory())
                .isPrivate(inquiry.isPrivate())
                .pushNotification(inquiry.isPushNotification())
                .answeredBy(inquiry.getAnsweredBy())
                .createdDate(inquiry.getCreatedDate())
                .modifiedDate(inquiry.getModifiedDate())
                .userId(inquiry.getUserId())
                .userEmail(author != null ? author.email() : null)
                .userProfileName(author != null ? author.profileName() : null)
                .userName(author != null ? author.name() : null)
                .questionImageUrls(questionImages)
                .answerImageUrls(answerImages)
                .build();
    }

    /**
     * 엔티티에서 DTO 생성 (이미지 없이).
     */
    public static InquiryResponseDto from(Inquiry inquiry, UserSummary author) {
        return InquiryResponseDto.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus())
                .category(inquiry.getCategory())
                .isPrivate(inquiry.isPrivate())
                .pushNotification(inquiry.isPushNotification())
                .answeredBy(inquiry.getAnsweredBy())
                .createdDate(inquiry.getCreatedDate())
                .modifiedDate(inquiry.getModifiedDate())
                .userId(inquiry.getUserId())
                .userEmail(author != null ? author.email() : null)
                .userProfileName(author != null ? author.profileName() : null)
                .userName(author != null ? author.name() : null)
                .build();
    }
}