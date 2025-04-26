package com.fream.back.domain.event.exception;

public class EventNotFoundException extends EventException {
    public EventNotFoundException() {
        super(EventErrorCode.EVENT_NOT_FOUND);
    }

    public EventNotFoundException(String message) {
        super(EventErrorCode.EVENT_NOT_FOUND, message);
    }
}