package com.fream.back.domain.address.dto;

import lombok.Data;

@Data
public class AddressCreateDto {
    private String recipientName;
    private String phoneNumber;
    private String zipCode;
    private String address;
    private String detailedAddress;
    private Boolean isDefault; // 기본 배송지 여부
}
