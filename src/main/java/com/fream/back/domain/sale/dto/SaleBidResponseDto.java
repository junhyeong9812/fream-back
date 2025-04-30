package com.fream.back.domain.sale.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 판매 입찰 응답 DTO
 * 판매 입찰 정보를 클라이언트에게 전달하기 위한 DTO입니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaleBidResponseDto {

    /**
     * 판매 입찰 ID
     */
    private Long saleBidId;

    /**
     * 상품 ID
     */
    private Long productId;

    /**
     * 상품명 (한글)
     */
    private String productName;

    /**
     * 상품명 (영문)
     */
    private String productEnglishName;

    /**
     * 상품 사이즈
     */
    private String size;

    /**
     * 상품 색상명
     */
    private String colorName;

    /**
     * 썸네일 이미지 URL
     */
    private String thumbnailImageUrl;

    /**
     * 입찰 가격
     */
    private int bidPrice;

    /**
     * 판매 입찰 상태 (PENDING, MATCHED, CANCELLED, COMPLETED)
     */
    private String saleBidStatus;

    /**
     * 판매 상태 (PENDING_SHIPMENT, IN_TRANSIT, IN_INSPECTION 등)
     */
    private String saleStatus;

    /**
     * 배송 상태
     */
    private String shipmentStatus;

    /**
     * 생성 일시
     * ISO 형식으로 포맷팅하여 반환
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdDate;

    /**
     * 수정 일시
     * ISO 형식으로 포맷팅하여 반환
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime modifiedDate;

    /**
     * 즉시 판매 여부
     */
    private boolean isInstantSale;

    /**
     * 가격 정보 문자열 반환
     * "₩10,000" 형식으로 반환
     *
     * @return 포맷팅된 가격 문자열
     */
    public String getFormattedPrice() {
        return String.format("₩%,d", bidPrice);
    }

    /**
     * 판매 입찰 상태의 한글명 반환
     *
     * @return 한글로 표현된 상태명
     */
    public String getSaleBidStatusKorean() {
        if (saleBidStatus == null) return "";

        return switch (saleBidStatus) {
            case "PENDING" -> "대기 중";
            case "MATCHED" -> "매칭 완료";
            case "CANCELLED" -> "취소됨";
            case "COMPLETED" -> "완료됨";
            default -> saleBidStatus;
        };
    }

    /**
     * 판매 상태의 한글명 반환
     *
     * @return 한글로 표현된 상태명
     */
    public String getSaleStatusKorean() {
        if (saleStatus == null) return "";

        return switch (saleStatus) {
            case "PENDING_SHIPMENT" -> "발송 대기";
            case "IN_TRANSIT" -> "배송 중";
            case "IN_INSPECTION" -> "검수 중";
            case "FAILED_INSPECTION" -> "검수 불합격";
            case "IN_STORAGE" -> "보관 중";
            case "ON_AUCTION" -> "경매 중";
            case "SELLING" -> "판매 중";
            case "SOLD" -> "판매 완료";
            case "AUCTION_EXPIRED" -> "경매 만료";
            default -> saleStatus;
        };
    }
}