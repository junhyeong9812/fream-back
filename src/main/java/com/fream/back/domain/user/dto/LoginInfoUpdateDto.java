package com.fream.back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginInfoUpdateDto {
    private String newEmail; // 변경할 이메일
    private String Password; //현재 사용중인 비밀번호
    private String newPassword; // 변경할 비밀번호
    private String newPhoneNumber; // 변경할 휴대폰 번호
    private String newShoeSize; // 변경할 신발 사이즈
    private Boolean adConsent; // 광고성 정보 수신 동의 여부
    private Boolean privacyConsent; // 개인정보 수집 및 이용 동의 여부
    private Boolean smsConsent; // 문자 메세지 수신 동의 여부
    private Boolean emailConsent; // 이메일 수신 동의 여부
}
