package com.fream.back.domain.user.dto;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.ShoeSize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthSignupCompleteDto {
    private String token;    // 임시 토큰

    private String phoneNumber;  // 전화번호
    private Integer age;         // 나이
    private Gender gender;       // 성별
    private ShoeSize shoeSize;   // 신발 사이즈

    private Boolean termsAgreement;  // 이용약관 동의
    private Boolean privacyAgreement; // 개인정보 수집 동의
    private Boolean optionalPrivacyAgreement; // 선택적 개인정보 동의
    private Boolean adConsent;    // 광고성 정보 수신 동의

    private String referralCode;  // 추천인 코드 (선택)
}