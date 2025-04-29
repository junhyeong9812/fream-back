package com.fream.back.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 결제 민감 정보 암호화 서비스
 * 카드 번호, 생년월일 등 민감 정보를 암호화 및 복호화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEncryptionService {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    @Value("${payment.encryption.secret-key}")
    private String secretKey;

    @Value("${payment.encryption.salt}")
    private String salt;

    @Value("${payment.encryption.iv}")
    private String iv;

    /**
     * 문자열 암호화
     * @param plainText 암호화할 평문
     * @return 암호화된 문자열
     */
    public String encrypt(String plainText) {
        try {
            SecretKey key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("암호화 중 오류 발생: {}", e.getMessage(), e);
            // 암호화 실패 시 IllegalStateException 발생 - 저장 불가능한 상태로 처리
            throw new IllegalStateException("민감 정보 암호화 실패", e);
        }
    }

    /**
     * 암호화된 문자열 복호화
     * @param encryptedText 복호화할 암호문
     * @return 복호화된 평문
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKey key = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));

            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("복호화 중 오류 발생: {}", e.getMessage(), e);
            // 복호화 실패 시 IllegalStateException 발생
            throw new IllegalStateException("민감 정보 복호화 실패", e);
        }
    }

    /**
     * 비밀키 생성
     * @return 생성된 비밀키
     */
    private SecretKey generateKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                secretKey.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                ITERATION_COUNT,
                KEY_LENGTH
        );
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}