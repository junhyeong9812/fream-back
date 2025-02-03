package com.fream.back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // 기본 생성자
@AllArgsConstructor // 모든 필드를 초기화하는 생성자
public class EmailFindRequestDto {
    private String phoneNumber;
}
