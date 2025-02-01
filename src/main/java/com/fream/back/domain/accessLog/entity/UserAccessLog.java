package com.fream.back.domain.accessLog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_access_log")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_access_log_seq")
    @SequenceGenerator(name = "user_access_log_seq", sequenceName = "USER_ACCESS_LOG_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "REFERER_URL")
    private String refererUrl;

    @Column(name = "USER_AGENT")
    private String userAgent;

    private String os;
    private String browser;

    @Column(name = "DEVICE_TYPE")
    private String deviceType;

    @Builder.Default
    @Column(name = "ACCESS_TIME")
    private LocalDateTime accessTime = LocalDateTime.now();

    private String ipAddress;
    private String country;
    private String region;
    private String city;
    private String pageUrl;
    private String email;

    @Builder.Default
    @Column(name = "IS_ANONYMOUS", nullable = false)
    private boolean isAnonymous = true;

    private String networkType;
    private String browserLanguage;
    private int screenWidth;
    private int screenHeight;
    private float devicePixelRatio;
}
