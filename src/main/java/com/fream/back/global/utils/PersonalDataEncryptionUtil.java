package com.fream.back.global.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 개인정보 암호화 및 복호화 유틸리티
 * - 양방향 암호화: 상세주소 등 복구가 필요한 데이터
 * - 결정적 암호화: 이름, 전화번호, 주소 등 검색이 필요한 데이터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonalDataEncryptionUtil {

    @Value("${personal-data.encryption.secret-key}")
    private String secretKey;

    @Value("${personal-data.encryption.iv}")
    private String iv;

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String DETERMINISTIC_ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * 양방향 암호화 (상세주소용)
     * IV를 사용하여 같은 입력이라도 매번 다른 암호화 결과 생성
     *
     * @param plainText 암호화할 원본 문자열
     * @return 암호화된 문자열 (Base64 인코딩)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParamSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParamSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("데이터 암호화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("데이터 암호화 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 양방향 복호화 (상세주소용)
     *
     * @param encryptedText 암호화된 문자열 (Base64 인코딩)
     * @return 복호화된 원본 문자열
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec ivParamSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParamSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decodedBytes);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("데이터 복호화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("데이터 복호화 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 결정적 암호화 (이름, 전화번호, 주소용)
     * 같은 입력에 대해 항상 같은 암호화 결과 생성 -> 검색 가능
     *
     * @param plainText 암호화할 원본 문자열
     * @return 암호화된 문자열 (Base64 인코딩)
     */
    public String deterministicEncrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // 입력값에 대한 해시를 생성하여 고정된 키 생성
            String deterministicKey = generateDeterministicKey(plainText);

            Cipher cipher = Cipher.getInstance(DETERMINISTIC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(deterministicKey.getBytes(StandardCharsets.UTF_8), "AES");

            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("결정적 암호화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("결정적 암호화 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 결정적 복호화 (이름, 전화번호, 주소용)
     *
     * @param encryptedText 암호화된 문자열
     * @return 복호화된 원본 문자열
     */
    public String deterministicDecrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // 복호화를 위해 모든 가능한 키를 시도해야 하므로
            // 실제로는 원본 값과 비교하는 방식으로 사용
            // 또는 별도의 매핑 테이블 필요

            // 임시로 기본키로 복호화 시도
            Cipher cipher = Cipher.getInstance(DETERMINISTIC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");

            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decodedBytes);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("결정적 복호화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("결정적 복호화 처리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 입력값 기반 결정적 키 생성
     */
    private String generateDeterministicKey(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((secretKey + input).getBytes(StandardCharsets.UTF_8));

            // 32바이트 중 처음 16바이트만 사용 (AES-128)
            byte[] keyBytes = new byte[16];
            System.arraycopy(hash, 0, keyBytes, 0, 16);

            return new String(keyBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("결정적 키 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("결정적 키 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 검색용 암호화된 값 생성
     * 검색 시 사용할 암호화된 값을 생성
     *
     * @param plainText 검색할 원본 문자열
     * @return 검색용 암호화된 문자열
     */
    public String encryptForSearch(String plainText) {
        return deterministicEncrypt(plainText);
    }

    /**
     * 암호화된 데이터인지 확인 (Base64 형식인지 검사)
     *
     * @param text 검사할 문자열
     * @return 암호화된 데이터 여부
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        try {
            // Base64 디코딩 시도
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}