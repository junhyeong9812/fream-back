package com.fream.back.domain.product.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SortOption {
    private String field;  // 정렬 기준 (price, releaseDate, interestCount 등)
    private String order;  // 정렬 순서 (asc, desc)
}