package com.fream.back.domain.address.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressResponseDto {
    private Long id;
    private String recipientName;
    private String phoneNumber;
    private String zipCode;
    private String address;
    private String detailedAddress;
    private Boolean isDefault;
}
