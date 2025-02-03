package com.fream.back.domain.payment.entity;

public enum Bank {
    INDUSTRIAL("산업은행"),
    CORPORATE("기업은행"),
    KB("국민은행"),
    NH("농협은행"),
    WOORI("우리은행"),
    SC("SC제일은행"),
    DAEGU("대구은행"),
    BUSAN("부산은행"),
    GWANGJU("광주은행"),
    JEJU("제주은행"),
    JEONBUK("전북은행"),
    GYEONGNAM("경남은행"),
    SHINHAN("신한은행"),
    HANA("하나은행"),
    KAKAO("카카오뱅크");

    private final String displayName;

    Bank(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

