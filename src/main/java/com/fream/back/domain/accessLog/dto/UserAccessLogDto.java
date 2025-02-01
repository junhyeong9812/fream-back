package com.fream.back.domain.accessLog.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessLogDto {

    private String refererUrl;       // 참조 URL
    private String userAgent;        // 브라우저 및 OS 정보
    private String os;               // 운영체제 정보
    private String browser;          // 브라우저 정보
    private String deviceType;       // 디바이스 타입

    private String ipAddress;        // 사용자 IP 주소
    private String country;          // 위치 정보 - 나라
    private String region;           // 위치 정보 - 지역
    private String city;             // 위치 정보 - 도시

    private String pageUrl;          // 방문한 페이지 URL
    private String email;            // 사용자 이메일
    private boolean isAnonymous;     // 익명 사용자 여부

    private String networkType;      // 네트워크 타입
    private String browserLanguage;  // 브라우저 언어
    private int screenWidth;         // 전체 화면 너비
    private int screenHeight;        // 전체 화면 높이
    private float devicePixelRatio;  // 픽셀 밀도
}
