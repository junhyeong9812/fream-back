package com.fream.back.domain.sale.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstantSaleRequestDto {

    private Long orderBidId; // 주문 입찰 ID
    private String returnAddress; // 반송 주소
    private String postalCode; // 우편번호
    private String receiverPhone; // 수령인 전화번호
}

