package com.fream.back.domain.payment.dto.paymentInfo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 결제 정보 생성 DTO
 * 카드 정보 등록 시 사용
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInfoCreateDto {

    /**
     * 카드 번호 (15-16자리)
     * 숫자만 허용
     */
    @NotBlank(message = "카드 번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^[0-9]{15,16}$", message = "카드 번호는 15-16자리 숫자여야 합니다.")
    private String cardNumber;

    /**
     * 카드 비밀번호 앞 두 자리
     * 숫자 2자리만 허용
     */
    @NotBlank(message = "카드 비밀번호 앞 2자리는 필수 입력 값입니다.")
    @Pattern(regexp = "^[0-9]{2}$", message = "카드 비밀번호는 앞 2자리 숫자여야 합니다.")
    private String cardPassword;

    /**
     * 카드 유효기간 (MM/YY 형식)
     */
    @NotBlank(message = "카드 유효기간은 필수 입력 값입니다.")
    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "카드 유효기간은 MM/YY 형식이어야 합니다.")
    private String expirationDate;

    /**
     * 생년월일 (YYMMDD 형식, 6자리)
     */
    @NotBlank(message = "생년월일은 필수 입력 값입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "생년월일은 YYMMDD 형식의 6자리 숫자여야 합니다.")
    private String birthDate;
}