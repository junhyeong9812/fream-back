package com.fream.back.domain.user.exception;

/**
 * 이메일 중복 시 발생하는 예외
 */
public class DuplicateEmailException extends UserException {

    private final String email;

    /**
     * 중복된 이메일을 기반으로 한 생성자
     *
     * @param email 중복된 이메일 주소
     */
    public DuplicateEmailException(String email) {
        super(UserErrorCode.DUPLICATE_EMAIL, "이미 사용 중인 이메일입니다: " + email);
        this.email = email;
    }

    /**
     * 커스텀 메시지를 포함한 생성자
     *
     * @param email 중복된 이메일 주소
     * @param message 커스텀 메시지
     */
    public DuplicateEmailException(String email, String message) {
        super(UserErrorCode.DUPLICATE_EMAIL, message);
        this.email = email;
    }

    /**
     * 중복된 이메일 주소 반환
     *
     * @return 중복된 이메일 주소
     */
    public String getEmail() {
        return email;
    }
}