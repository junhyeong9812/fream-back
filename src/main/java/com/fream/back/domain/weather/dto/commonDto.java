package com.fream.back.domain.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class commonDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageDto<T> {
        private List<T> content;      // 실제 데이터 목록
        private long totalElements;   // 전체 개수
        private int totalPages;       // 전체 페이지
        private int page;            // 현재 페이지
        private int size;            // 페이지 당 개수
    }

}