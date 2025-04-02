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
public class LogLineDTO {
    private int lineNumber;
    private String timestamp;
    private String thread;
    private String level;
    private String logger;
    private String message;
    private String rawLine;
}