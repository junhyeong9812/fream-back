package com.fream.back.domain.product.dto;

import com.fream.back.domain.product.entity.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectionResponseDto {
    private Long id; // 컬렉션 ID
    private String name; // 컬렉션명

    public static CollectionResponseDto fromEntity(Collection collection) {
        return CollectionResponseDto.builder()
                .id(collection.getId())
                .name(collection.getName())
                .build();
    }
}
