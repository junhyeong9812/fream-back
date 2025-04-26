package com.fream.back.domain.event.exception;

public class InvalidEventDateException extends EventException {
    public InvalidEventDateException() {
        super(EventErrorCode.INVALID_EVENT_DATE);
    }

    public InvalidEventDateException(String message) {
        super(EventErrorCode.INVALID_EVENT_DATE, message);
    }
}