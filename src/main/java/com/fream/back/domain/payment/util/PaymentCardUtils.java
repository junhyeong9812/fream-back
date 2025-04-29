package com.fream.back.domain.payment.util;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 카드 관련 유틸리티 클래스
 * 카드 정보 검증 및 마스킹 기능 제공
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE) // 인스턴스화 방지
public class PaymentCardUtils {

    /**
     * 카드 정보 유효성 검증
     * @param dto 검증할 카드 정보 DTO
     * @throws PaymentException 유효하지 않은 카드 정보인 경우
     */
    public static void validateCardInfo(PaymentInfoCreateDto dto) {
        if (dto == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "결제 정보가 없습니다.");
        }

        // 필수 입력값 검증
        if (dto.getCardNumber() == null || dto.getCardNumber().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 번호는 필수 입력 값입니다.");
        }

        if (dto.getExpirationDate() == null || dto.getExpirationDate().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 유효기간은 필수 입력 값입니다.");
        }

        if (dto.getBirthDate() == null || dto.getBirthDate().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "생년월일은 필수 입력 값입니다.");
        }

        if (dto.getCardPassword() == null || dto.getCardPassword().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 비밀번호 앞 2자리는 필수 입력 값입니다.");
        }

        // 형식 검증
        if (!dto.getCardNumber().matches("^[0-9]{15,16}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 번호는 15-16자리 숫자여야 합니다.");
        }

        if (!dto.getExpirationDate().matches("^(0[1-9]|1[0-2])/[0-9]{2}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 유효기간은 MM/YY 형식이어야 합니다.");
        }

        if (!dto.getBirthDate().matches("^[0-9]{6}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "생년월일은 YYMMDD 형식의 6자리 숫자여야 합니다.");
        }

        if (!dto.getCardPassword().matches("^[0-9]{2}$")) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 비밀번호는 앞 2자리 숫자여야 합니다.");
        }

        // 유효기간 유효성 검증 (현재 날짜보다 미래인지)
        validateExpirationDate(dto.getExpirationDate());
    }

    /**
     * 카드 유효기간이 현재 날짜보다 미래인지 검증
     * @param expirationDate MM/YY 형식의 유효기간
     * @throws PaymentException 만료된 카드인 경우
     */
    private static void validateExpirationDate(String expirationDate) {
        try {
            String[] parts = expirationDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]) + 2000; // YY를 20YY로 변환

            java.time.YearMonth expiration = java.time.YearMonth.of(year, month);
            java.time.YearMonth now = java.time.YearMonth.now();

            if (expiration.isBefore(now)) {
                throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "만료된 카드입니다.");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "유효기간 형식이 올바르지 않습니다.");
        }
    }

    /**
     * 카드번호 마스킹 처리
     * 앞 4자리와 뒤 4자리를 제외한 나머지 부분을 '*'로 마스킹
     * @param cardNumber 마스킹할 카드 번호
     * @return 마스킹된 카드 번호
     */
    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "Invalid";
        }
        return cardNumber.substring(0, 4) + "*".repeat(cardNumber.length() - 8) +
                cardNumber.substring(cardNumber.length() - 4);
    }

    /**
     * 카드 비밀번호 마스킹 처리
     * @param password 마스킹할 카드 비밀번호
     * @return 마스킹된 카드 비밀번호
     */
    public static String maskCardPassword(String password) {
        return "**";
    }

    /**
     * 생년월일 마스킹 처리
     * 가운데 2자리만 표시하고 나머지는 '*'로 마스킹
     * @param birthDate 마스킹할 생년월일
     * @return 마스킹된 생년월일
     */
    public static String maskBirthDate(String birthDate) {
        if (birthDate == null || birthDate.length() < 6) {
            return "******";
        }
        return "**" + birthDate.substring(2, 4) + "**";
    }

    /**
     * 로깅용 마스킹 처리 - 카드번호
     * 앞 4자리와 뒤 4자리만 표시하고 나머지는 '*'로 마스킹
     * @param cardNumber 마스킹할 카드 번호
     * @return 마스킹된 카드 번호
     */
    public static String maskCardNumberForLogging(String cardNumber) {
        return maskCardNumber(cardNumber);
    }
}