package com.fream.back.domain.event.exception;

import com.fream.back.global.exception.ErrorCode;
import com.fream.back.global.exception.GlobalException;

public class EventException extends GlobalException {
    public EventException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EventException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}