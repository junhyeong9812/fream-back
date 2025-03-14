package com.fream.back.domain.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerifiedCustomerDto {
    private String name;          // 이름
    private String phoneNumber;   // 전화번호
    private String gender;        // 성별
    private String birthDate;     // 생년월일 (YYYY-MM-DD)
    private Boolean isForeigner;  // 외국인 여부
    private String ci;            // 연계정보
    private String di;            // 중복가입확인정보
}