package com.fream.back.domain.event.exception;

public class ImageNotFoundException extends EventException {
    public ImageNotFoundException() {
        super(EventErrorCode.IMAGE_NOT_FOUND);
    }

    public ImageNotFoundException(String message) {
        super(EventErrorCode.IMAGE_NOT_FOUND, message);
    }
}