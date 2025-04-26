package com.fream.back.domain.product.exception;

import com.fream.back.domain.event.exception.EventErrorCode;
import com.fream.back.global.exception.ErrorCode;
import com.fream.back.global.exception.GlobalException;

/**
 * 브랜드를 찾을 수 없을 때 발생하는 예외
 */
public class BrandNotFoundException extends GlobalException {
    public BrandNotFoundException() {
        super(EventErrorCode.BRAND_NOT_FOUND);
    }

    public BrandNotFoundException(String message) {
        super(EventErrorCode.BRAND_NOT_FOUND, message);
    }
}