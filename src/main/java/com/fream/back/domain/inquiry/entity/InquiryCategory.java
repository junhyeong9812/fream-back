package com.fream.back.domain.inquiry.entity;

/**
 * 1대1 문의 카테고리를 나타내는 Enum
 */
public enum InquiryCategory {
    PRODUCT("상품문의"),
    ORDER("주문/결제"),
    DELIVERY("배송문의"),
    RETURN("반품/교환"),
    ACCOUNT("계정문의"),
    ETC("기타문의");

    private final String description;

    InquiryCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}