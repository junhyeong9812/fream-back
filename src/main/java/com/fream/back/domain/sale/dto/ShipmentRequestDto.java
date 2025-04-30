package com.fream.back.domain.sale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 배송 정보 요청 DTO
 * 판매자의 배송 정보 등록에 필요한 정보를 담고 있습니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentRequestDto {

    /**
     * 택배사명
     * 필수값입니다.
     */
    @NotBlank(message = "택배사는 필수입니다.")
    private String courier;

    /**
     * 운송장 번호
     * 필수값이며, 특정 형식을 따라야 합니다.
     */
    @NotBlank(message = "운송장 번호는 필수입니다.")
    @Pattern(regexp = "^[A-Za-z0-9]{9,13}$", message = "운송장 번호 형식이 올바르지 않습니다. 9~13자리의 영문자와 숫자 조합이어야 합니다.")
    private String trackingNumber;
}