package com.fream.back.domain.address.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AddressListResponseDto {
    private List<AddressResponseDto> addresses;
}
