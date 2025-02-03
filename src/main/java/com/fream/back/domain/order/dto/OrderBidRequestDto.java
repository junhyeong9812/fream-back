package com.fream.back.domain.order.dto;

import lombok.Data;

@Data
public class OrderBidRequestDto {
    private String userEmail;
    private Long productSizeId;
    private int bidPrice;
}
