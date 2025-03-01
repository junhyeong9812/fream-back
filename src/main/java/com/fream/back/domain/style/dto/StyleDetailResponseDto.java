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
    private List<ProductInfoDto> productInfos; // 상품 정보 목록
    private LocalDateTime createdDate;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductInfoDto {
        private String productName; // 상품명
        private String productEnglishName; // 상품 영어명
        private String thumbnailImageUrl; // 대표 이미지
        private Integer minSalePrice; // 최저 판매가
    }
}
