package com.fream.back.domain.user.exception;

/**
 * 잘못된 은행 계좌 정보일 때 발생하는 예외
 */
public class InvalidBankAccountException extends UserException {

    private final String bankName;
    private final String accountNumber;

    /**
     * 기본 생성자
     */
    public InvalidBankAccountException() {
        super(UserErrorCode.INVALID_BANK_ACCOUNT);
        this.bankName = null;
        this.accountNumber = null;
    }

    /**
     * 은행명과 계좌번호를 기반으로 한 생성자
     *
     * @param bankName 은행명
     * @param accountNumber 계좌번호
     */
    public InvalidBankAccountException(String bankName, String accountNumber) {
        super(UserErrorCode.INVALID_BANK_ACCOUNT,
                String.format("잘못된 계좌 정보입니다. 은행명: %s, 계좌번호: %s****",
                        bankName,
                        accountNumber != null && accountNumber.length() > 4
                                ? accountNumber.substring(0, 4) : "****"));
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param message 에러 메시지
     */
    public InvalidBankAccountException(String message) {
        super(UserErrorCode.INVALID_BANK_ACCOUNT, message);
        this.bankName = null;
        this.accountNumber = null;
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 에러 메시지
     * @param cause 원인 예외
     */
    public InvalidBankAccountException(String message, Throwable cause) {
        super(UserErrorCode.INVALID_BANK_ACCOUNT, message, cause);
        this.bankName = null;
        this.accountNumber = null;
    }

    /**
     * 은행명 반환
     *
     * @return 은행명
     */
    public String getBankName() {
        return bankName;
    }

    /**
     * 계좌번호 반환 (마스킹된 형태)
     *
     * @return 마스킹된 계좌번호
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return accountNumber.substring(0, 4) + "****";
    }
}