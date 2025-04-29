package com.fream.back.domain.order.dto;

import com.fream.back.domain.payment.dto.PaymentRequestDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 즉시 구매 요청 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InstantOrderRequestDto {

    @NotNull(message = "판매 입찰 ID는 필수입니다.")
    private Long saleBidId;                 // 판매 입찰 ID

    @NotNull(message = "배송지 ID는 필수입니다.")
    private Long addressId;                 // 주소 ID

    private boolean warehouseStorage;       // 창고 보관 여부

    @NotNull(message = "결제 정보는 필수입니다.")
    private PaymentRequestDto paymentRequest; // 결제 정보
}