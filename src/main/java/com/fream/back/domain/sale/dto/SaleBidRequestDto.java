package com.fream.back.domain.sale.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 판매 입찰 생성 요청 DTO
 * 판매 입찰 생성에 필요한 정보를 담고 있습니다.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaleBidRequestDto {

    /**
     * 상품 사이즈 ID
     * 필수값입니다.
     */
    @NotNull(message = "상품 사이즈 ID는 필수입니다.")
    private Long productSizeId;

    /**
     * 입찰 가격
     * 최소 1000원 이상이어야 합니다.
     */
    @Min(value = 1000, message = "입찰 가격은 최소 1,000원 이상이어야 합니다.")
    private int bidPrice;

    /**
     * 반송 주소
     * 필수값입니다.
     */
    @NotBlank(message = "반송 주소는 필수입니다.")
    private String returnAddress;

    /**
     * 우편번호
     * 숫자로만 구성된 5자리 문자열이어야 합니다.
     */
    @NotBlank(message = "우편번호는 필수입니다.")
    @Pattern(regexp = "\\d{5}", message = "우편번호는 5자리 숫자여야 합니다.")
    private String postalCode;

    /**
     * 수신자 전화번호
     * 숫자, 하이픈으로 구성된 10~11자리 문자열이어야 합니다.
     */
    @NotBlank(message = "수신자 전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String receiverPhone;

    /**
     * 창고 보관 여부
     * 기본값은 false입니다.
     */
    private boolean warehouseStorage = false;
}