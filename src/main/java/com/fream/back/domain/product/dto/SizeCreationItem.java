package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.enumType.SizeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SizeCreationItem {
    private final String size;           // 실제 사이즈 문자열
    private final SizeType sizeType;     // CLOTHING, SHOES, ACCESSORIES
    private final Long productColorId;   // 어떤 ProductColor에 연결할지
    private final int releasePrice;      // 가격
}