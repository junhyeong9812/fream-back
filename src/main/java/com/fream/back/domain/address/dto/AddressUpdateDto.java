package com.fream.back.domain.address.dto;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Data
public class AddressUpdateDto {
    @NotNull(message = "주소 ID는 필수 입력사항입니다.")
    private Long addressId;

    private String recipientName;

    @Pattern(regexp = "^$|\\d{10,11}", message = "전화번호 형식이 올바르지 않습니다. 숫자 10-11자리로 입력해주세요.")
    private String phoneNumber;

    @Pattern(regexp = "^$|\\d{5}", message = "우편번호 형식이 올바르지 않습니다. 5자리 숫자로 입력해주세요.")
    private String zipCode;

    private String address;
    private String detailedAddress;
    private Boolean isDefault;
}