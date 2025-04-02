package com.fream.back.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 로그 파일 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogFileDTO {
    private String fileName;
    private Long fileSize;
    private String lastModified;
}