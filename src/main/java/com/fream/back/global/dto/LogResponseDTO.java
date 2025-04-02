package com.fream.back.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDTO {
    private String fileName;
    private int page;
    private int size;
    private int totalLines;
    private int totalFilteredLines;
    private List<LogLineDTO> content;
    private String error;
}
