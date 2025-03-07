package com.fream.back.domain.style.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StyleDetailResponseDto {
    private Long id; // Style ID
    private Long profileId; //프로필ID
    private String profileName; // 작성자 이름
    private String profileImageUrl; // 작성자 프로필 이미지
    private String content; // 텍스트 컨텐츠
    private List<String> mediaUrls; // 미디어 URL 목록
    private Long likeCount; // 좋아요 수
    private Long commentCount; // 댓글 수
    private Boolean liked = false; // 좋아요 상태 필드
    private Boolean interested = false; // 관심 상태 필드
    private List<ProductInfoDto> productInfos; // 상품 정보 목록
    private LocalDateTime createdDate;

    // 기존 필드만 사용하는 생성자 추가 (필요시)
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
        this.mediaUrls = mediaUrls;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.productInfos = productInfos;
        this.createdDate = createdDate;
        this.liked = false;
        this.interested = false;
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