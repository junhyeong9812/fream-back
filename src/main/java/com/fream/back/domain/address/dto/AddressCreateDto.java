package com.fream.back.domain.address.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Data
public class AddressCreateDto {
    @NotBlank(message = "수령인 이름은 필수 입력사항입니다.")
    private String recipientName;

    @NotBlank(message = "연락처는 필수 입력사항입니다.")
    @Pattern(regexp = "\\d{10,11}", message = "전화번호 형식이 올바르지 않습니다. 숫자 10-11자리로 입력해주세요.")
    private String phoneNumber;

    @NotBlank(message = "우편번호는 필수 입력사항입니다.")
    @Pattern(regexp = "\\d{5}", message = "우편번호 형식이 올바르지 않습니다. 5자리 숫자로 입력해주세요.")
    private String zipCode;

    @NotBlank(message = "주소는 필수 입력사항입니다.")
    private String address;

    private String detailedAddress;
    private Boolean isDefault; // 기본 배송지 여부
}