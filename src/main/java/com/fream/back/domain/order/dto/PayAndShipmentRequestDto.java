package com.fream.back.domain.order.dto;

import com.fream.back.domain.payment.dto.PaymentRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayAndShipmentRequestDto {

    private PaymentRequestDto paymentRequest; // 결제 정보
    private String receiverName;             // 배송 수령인 이름
    private String receiverPhone;            // 배송 수령인 전화번호
    private String postalCode;               // 배송 우편번호
    private String address;                  // 배송 주소
    private boolean warehouseStorage;        // 창고 보관 여부
}
