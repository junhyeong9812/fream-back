package com.fream.back.domain.product.entity.enumType;

public enum SizeType {
    CLOTHING(new String[]{"XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL", "28", "29", "30", "31", "32", "33", "34", "35", "36"}),
    SHOES(new String[]{"70", "80", "90", "110", "115", "120", "125", "130", "135", "140", "145", "150", "155", "160", "165", "170", "175", "180",
            "185", "190", "195", "200", "205", "210", "215", "220", "225", "230", "235", "240", "245", "250", "255", "260", "265",
            "270", "275", "280", "285", "290", "295", "300", "305", "310", "315", "320", "325", "330"}),
    ACCESSORIES(new String[]{"ONE_SIZE"}); // 단일 사이즈

    private final String[] sizes;

    SizeType(String[] sizes) {
        this.sizes = sizes;
    }

    public String[] getSizes() {
        return sizes;
    }

}
