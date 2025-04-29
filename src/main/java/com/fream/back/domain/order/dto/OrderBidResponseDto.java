package com.fream.back.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 입찰 응답 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderBidResponseDto {
    private Long orderBidId;        // OrderBid ID
    private Long productId;         // 상품 ID
    private String productName;     // 상품명
    private String productEnglishName; // 상품 영어명
    private String size;            // 상품 사이즈
    private String colorName;       // 색상 이름
    private String imageUrl;        // 상품 이미지 URL
    private int bidPrice;           // 입찰 가격
    private String bidStatus;       // 입찰 상태
    private String orderStatus;     // 주문 상태
    private String shipmentStatus;  // 배송 상태
    private LocalDateTime createdDate;  // 생성일
    private LocalDateTime modifiedDate; // 수정일
}