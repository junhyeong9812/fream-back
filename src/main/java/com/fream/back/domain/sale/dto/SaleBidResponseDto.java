package com.fream.back.domain.sale.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaleBidResponseDto {
    private Long saleBidId;
    private Long productId;
    private String productName;
    private String productEnglishName;
    private String size;
    private String colorName;
    private String thumbnailImageUrl;
    private int bidPrice;
    private String saleBidStatus;
    private String saleStatus;
    private String shipmentStatus;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
}
