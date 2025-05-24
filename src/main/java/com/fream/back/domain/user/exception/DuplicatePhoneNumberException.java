package com.fream.back.domain.user.exception;

/**
 * 전화번호 중복 시 발생하는 예외
 */
public class DuplicatePhoneNumberException extends UserException {

    private final String phoneNumber;

    /**
     * 중복된 전화번호를 기반으로 한 생성자
     *
     * @param phoneNumber 중복된 전화번호
     */
    public DuplicatePhoneNumberException(String phoneNumber) {
        super(UserErrorCode.DUPLICATE_PHONE_NUMBER, "이미 사용 중인 전화번호입니다: " + phoneNumber);
        this.phoneNumber = phoneNumber;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param phoneNumber 중복된 전화번호
     * @param message 커스텀 메시지
     */
    public DuplicatePhoneNumberException(String phoneNumber, String message) {
        super(UserErrorCode.DUPLICATE_PHONE_NUMBER, message);
        this.phoneNumber = phoneNumber;
    }

    /**
     * 중복된 전화번호 반환
     *
     * @return 중복된 전화번호
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }
}