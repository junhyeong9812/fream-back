package com.fream.back.domain.warehouseStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 창고 보관 기간 연장 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageExtensionRequestDto {

    /**
     * 창고 보관 ID
     */
    private Long storageId;

    /**
     * 새로운 종료일
     */
    private LocalDate newEndDate;
}