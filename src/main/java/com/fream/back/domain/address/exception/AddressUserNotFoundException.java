package com.fream.back.domain.address.exception;

/**
 * 주소와 관련된 사용자를 찾을 수 없을 때 발생하는 예외
 */
public class AddressUserNotFoundException extends AddressException {

    /**
     * 기본 에러 코드로 예외 생성
     */
    public AddressUserNotFoundException() {
        super(AddressErrorCode.ADDRESS_USER_NOT_FOUND);
    }

    /**
     * 사용자 정의 메시지와 함께 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     */
    public AddressUserNotFoundException(String message) {
        super(AddressErrorCode.ADDRESS_USER_NOT_FOUND, message);
    }

    /**
     * 원인 예외와 함께 예외 생성
     *
     * @param cause 원인이 되는 예외
     */
    public AddressUserNotFoundException(Throwable cause) {
        super(AddressErrorCode.ADDRESS_USER_NOT_FOUND, cause);
    }

    /**
     * 사용자 정의 메시지와 원인 예외로 예외 생성
     *
     * @param message 사용자 정의 에러 메시지
     * @param cause 원인이 되는 예외
     */
    public AddressUserNotFoundException(String message, Throwable cause) {
        super(AddressErrorCode.ADDRESS_USER_NOT_FOUND, message, cause);
    }
}