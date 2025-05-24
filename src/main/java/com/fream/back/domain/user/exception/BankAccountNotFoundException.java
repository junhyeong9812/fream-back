package com.fream.back.domain.user.exception;

/**
 * 은행 계좌 정보를 찾을 수 없을 때 발생하는 예외
 */
public class BankAccountNotFoundException extends UserException {

    private final String userEmail;

    /**
     * 기본 생성자
     */
    public BankAccountNotFoundException() {
        super(UserErrorCode.BANK_ACCOUNT_NOT_FOUND);
        this.userEmail = null;
    }

    /**
     * 사용자 이메일을 기반으로 한 생성자
     *
     * @param userEmail 계좌 정보가 없는 사용자의 이메일
     */
    public BankAccountNotFoundException(String userEmail) {
        super(UserErrorCode.BANK_ACCOUNT_NOT_FOUND, "등록된 계좌 정보가 없습니다. 사용자: " + userEmail);
        this.userEmail = userEmail;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public BankAccountNotFoundException(String message, Throwable cause) {
        super(UserErrorCode.BANK_ACCOUNT_NOT_FOUND, message, cause);
        this.userEmail = null;
    }

    /**
     * 사용자 이메일 반환
     *
     * @return 사용자 이메일
     */
    public String getUserEmail() {
        return userEmail;
    }
}