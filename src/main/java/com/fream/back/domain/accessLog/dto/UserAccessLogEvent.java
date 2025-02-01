package com.fream.back.domain.accessLog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessLogEvent {

    private String refererUrl;
    private String userAgent;
    private String os;
    private String browser;
    private String deviceType;

    private String ipAddress;
    private String country;
    private String region;
    private String city;

    private String pageUrl;
    private String email;
    private boolean isAnonymous;

    private String networkType;
    private String browserLanguage;
    private int screenWidth;
    private int screenHeight;
    private float devicePixelRatio;

    private LocalDateTime accessTime;
}
