package com.fream.back.domain.user.validate;


import com.fream.back.domain.user.dto.BankAccount.BankAccountDto;

import java.util.HashMap;
import java.util.Map;


public class BankAccountControllerValidator {

    private static final Map<String, String> BANK_ACCOUNT_REGEX_MAP = new HashMap<>();

    static {
        // 예시로 질문에서 주어진 은행명 & 정규식
        // (구)KB국민은행 -> 3자리-2자리-4자리-3자리 => 총 12자리
        BANK_ACCOUNT_REGEX_MAP.put("(구)KB국민은행", "^\\d{3}-\\d{2}-\\d{4}-\\d{3}$");

        // (신)KB국민은행 -> 6자리-2자리-6자리 => 14자리
        BANK_ACCOUNT_REGEX_MAP.put("(신)KB국민은행", "^\\d{6}-\\d{2}-\\d{6}$");

        // IBK기업은행(14)
        BANK_ACCOUNT_REGEX_MAP.put("IBK기업은행", "^\\d{3}-\\d{6}-\\d{2}-\\d{3}$");

        // ... 기타 은행 추가
        BANK_ACCOUNT_REGEX_MAP.put("NH농협은행", "^\\d{3}-\\d{4}-\\d{4}-\\d{2}$");
        BANK_ACCOUNT_REGEX_MAP.put("(구)신한은행", "^\\d{3}-\\d{2}-\\d{6}$");
        BANK_ACCOUNT_REGEX_MAP.put("(신)신한은행", "^\\d{3}-\\d{3}-\\d{6}$");
        BANK_ACCOUNT_REGEX_MAP.put("우리은행", "^\\d{4}-\\d{3}-\\d{6}$");
        BANK_ACCOUNT_REGEX_MAP.put("KEB하나은행", "^\\d{3}-\\d{6}-\\d{5}$");
        BANK_ACCOUNT_REGEX_MAP.put("(구)외환은행", "^\\d{3}-\\d{6}-\\d{3}$");
        BANK_ACCOUNT_REGEX_MAP.put("씨티은행", "^\\d{3}-\\d{6}-\\d{3}$");
        BANK_ACCOUNT_REGEX_MAP.put("DGB대구은행", "^\\d{3}-\\d{2}-\\d{6}-\\d{1}$");
        BANK_ACCOUNT_REGEX_MAP.put("BNK부산은행", "^\\d{3}-\\d{4}-\\d{4}-\\d{2}$");
        BANK_ACCOUNT_REGEX_MAP.put("SC제일은행", "^\\d{3}-\\d{2}-\\d{6}$");
        BANK_ACCOUNT_REGEX_MAP.put("케이뱅크", "^\\d{3}-\\d{3}-\\d{6}$");
        BANK_ACCOUNT_REGEX_MAP.put("카카오뱅크", "^\\d{4}-\\d{2}-\\d{7}$");
        // 필요한 것 추가
    }

    public static void validateBankAccountDto(BankAccountDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("계좌 정보가 없습니다.");
        }

        // 1) 은행명
        String bankName = dto.getBankName();
        if (bankName == null || bankName.isBlank()) {
            throw new IllegalArgumentException("은행명을 입력해주세요.");
        }
        if (!BANK_ACCOUNT_REGEX_MAP.containsKey(bankName)) {
            throw new IllegalArgumentException("지원되지 않는 은행명입니다: " + bankName);
        }

        // 2) 계좌번호
        String accountNumber = dto.getAccountNumber();
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("계좌번호를 입력해주세요.");
        }
        String regex = BANK_ACCOUNT_REGEX_MAP.get(bankName);
        if (!accountNumber.matches(regex)) {
            throw new IllegalArgumentException(
                    String.format("'%s' 형식에 맞지 않는 계좌번호입니다. (입력값: %s)", bankName, accountNumber)
            );
        }

        // 3) 예금주
        String accountHolder = dto.getAccountHolder();
        if (accountHolder == null || accountHolder.isBlank()) {
            throw new IllegalArgumentException("예금주를 입력해주세요.");
        }
    }
}
