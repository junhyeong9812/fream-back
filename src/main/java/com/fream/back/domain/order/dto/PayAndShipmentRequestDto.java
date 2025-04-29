package com.fream.back.domain.order.dto;

import com.fream.back.domain.payment.dto.PaymentRequestDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * 결제 및 배송 요청 DTO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayAndShipmentRequestDto {

    @NotNull(message = "결제 정보는 필수입니다.")
    private PaymentRequestDto paymentRequest; // 결제 정보

    @NotBlank(message = "배송 수령인 이름은 필수입니다.")
    private String receiverName;             // 배송 수령인 이름

    @NotBlank(message = "배송 수령인 전화번호는 필수입니다.")
    private String receiverPhone;            // 배송 수령인 전화번호

    @NotBlank(message = "배송 우편번호는 필수입니다.")
    private String postalCode;               // 배송 우편번호

    @NotBlank(message = "배송 주소는 필수입니다.")
    private String address;                  // 배송 주소

    private boolean warehouseStorage;        // 창고 보관 여부
}