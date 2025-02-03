package com.fream.back.domain.user.entity;

public enum ShoeSize {
    SIZE_220(220),
    SIZE_225(225),
    SIZE_230(230),
    SIZE_235(235),
    SIZE_240(240),
    SIZE_245(245),
    SIZE_250(250),
    SIZE_255(255),
    SIZE_260(260),
    SIZE_265(265),
    SIZE_270(270),
    SIZE_275(275),
    SIZE_280(280),
    SIZE_285(285),
    SIZE_290(290),
    SIZE_295(295),
    SIZE_300(300);

    private final int value;

    ShoeSize(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
