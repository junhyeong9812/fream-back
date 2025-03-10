package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StyleDetailResponseDto {
    private Long id; // Style ID
    private Long profileId; // 프로필ID
    private String profileName; // 작성자 이름
    private String profileImageUrl; // 작성자 프로필 이미지
    private String content; // 텍스트 컨텐츠

    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>(); // 미디어 URL 목록

    private Long likeCount; // 좋아요 수
    private Long commentCount; // 댓글 수
    private Long interestCount; // 관심 수

    @Builder.Default
    private Boolean liked = false; // 좋아요 상태 필드

    @Builder.Default
    private Boolean interested = false; // 관심 상태 필드

    @Builder.Default
    private List<ProductInfoDto> productInfos = new ArrayList<>(); // 상품 정보 목록

    @Builder.Default
    private List<HashtagResponseDto> hashtags = new ArrayList<>(); // 해시태그 목록 추가

    private LocalDateTime createdDate; // BaseTimeEntity의 필드명에 맞춤
    private LocalDateTime modifiedDate; // BaseTimeEntity의 필드명에 맞춤

    // 기존 필드만 사용하는 생성자 추가 (호환성을 위해)
    public StyleDetailResponseDto(Long id, Long profileId, String profileName,
                                  String profileImageUrl, String content,
                                  List<String> mediaUrls, Long likeCount,
                                  Long commentCount, List<ProductInfoDto> productInfos,
                                  LocalDateTime createdDate) {
        this.id = id;
        this.profileId = profileId;
        this.profileName = profileName;
        this.profileImageUrl = profileImageUrl;
        this.content = content;
        this.mediaUrls = mediaUrls != null ? mediaUrls : new ArrayList<>();
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.productInfos = productInfos != null ? productInfos : new ArrayList<>();
        this.createdDate = createdDate;
        this.liked = false;
        this.interested = false;
        this.hashtags = new ArrayList<>(); // 빈 리스트로 초기화
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductInfoDto {
        private Long productId;
        private String productName; // 상품명
        private String productEnglishName; // 상품 영어명
        private String colorName; // 색상명
        private String thumbnailImageUrl; // 대표 이미지
        private Integer minSalePrice; // 최저 판매가
    }
}