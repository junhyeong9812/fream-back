package com.fream.back.global.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 개인정보 암호화 및 복호화 유틸리티
 * 양방향 암호화를 지원하여 필요시 원본 데이터 복구 가능
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

    /**
     * 문자열 데이터 암호화
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
     * 암호화된 문자열 복호화
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