package com.fream.back.domain.warehouseStorage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 창고 보관 기간 연장 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageExtensionResponseDto {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 창고 보관 ID
     */
    private Long storageId;

    /**
     * 새로운 종료일
     */
    private LocalDate newEndDate;

    /**
     * 연장된 일수
     */
    private int extendedDays;

    /**
     * 성공 응답 생성
     *
     * @param storageId 창고 보관 ID
     * @param newEndDate 새 종료일
     * @param previousEndDate 이전 종료일
     * @return 성공 응답 DTO
     */
    public static StorageExtensionResponseDto success(Long storageId, LocalDate newEndDate, LocalDate previousEndDate) {
        int extendedDays = (int) java.time.temporal.ChronoUnit.DAYS.between(previousEndDate, newEndDate);

        return StorageExtensionResponseDto.builder()
                .success(true)
                .message("창고 보관 기간이 성공적으로 연장되었습니다.")
                .storageId(storageId)
                .newEndDate(newEndDate)
                .extendedDays(extendedDays)
                .build();
    }

    /**
     * 실패 응답 생성
     *
     * @param message 오류 메시지
     * @return 실패 응답 DTO
     */
    public static StorageExtensionResponseDto fail(String message) {
        return StorageExtensionResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }
}