package com.fream.back.domain.product.entity.enumType;

public enum ColorType {
    BLACK("블랙"),
    GREY("그레이"),
    WHITE("화이트"),
    IVORY("아이보리"),
    BEIGE("베이지"),
    BROWN("브라운"),
    KHAKI("카키"),
    GREEN("그린"),
    LIGHT_GREEN("라이트그린"),
    MINT("민트"),
    NAVY("네이비"),
    BLUE("블루"),
    SKY_BLUE("스카이블루"),
    PURPLE("퍼플"),
    PINK("핑크"),
    RED("레드"),
    ORANGE("오렌지"),
    YELLOW("옐로우"),
    SILVER("실버"),
    GOLD("골드"),
    MIX("믹스");

    private final String displayName;

    ColorType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
