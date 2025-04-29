package com.fream.back.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;



/**
 * 주문 입찰 요청 DTO
 */
@Data
public class OrderBidRequestDto {
    private String userEmail;

    @NotNull(message = "상품 사이즈 정보가 필요합니다.")
    private Long productSizeId;

    @Min(value = 1, message = "입찰 가격은 0보다 커야 합니다.")
    private int bidPrice;
}