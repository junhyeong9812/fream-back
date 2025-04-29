package com.fream.back.domain.payment.portone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.payment.exception.PaymentApiException;
import com.fream.back.domain.payment.exception.PaymentErrorCode;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.exception.PaymentProcessingException;
import com.fream.back.domain.payment.util.PaymentCardUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * PortOne API 클라이언트
 * 외부 결제 API와의 통신 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortOneApiClient {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.iamport.kr";
    private static final String TOKEN_URL = "/users/getToken";
    private static final String ONE_TIME_PAYMENT_URL = "/subscribe/payments/onetime";
    private static final String CANCEL_PAYMENT_URL = "/payments/cancel";

    @Value("${imp.key}")
    private String impKey;

    @Value("${imp.secret}")
    private String impSecret;

    @Value("${imp.pg}")
    private String pg; // PG 설정 값

    @Value("${imp.storeId}")
    private String storeId;

    @Value("${payment.api.timeout:10000}")
    private int apiTimeout; // API 호출 타임아웃 (밀리초)

    @Value("${payment.retry.max-attempts:3}")
    private int maxRetryAttempts; // 최대 재시도 횟수

    @Value("${payment.retry.delay-ms:1000}")
    private int retryDelayMs; // 재시도 간격 (밀리초)

    /**
     * PortOne API 액세스 토큰 발급
     * @return 발급된 액세스 토큰
     * @throws PaymentApiException 토큰 발급 실패 시
     */
    @Retryable(
            value = {PaymentApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String getAccessToken() {
        Instant start = Instant.now();
        String url = BASE_URL + TOKEN_URL;

        log.info("PortOne API 토큰 발급 요청 시작");

        // 요청 본문 생성
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_key", impKey); // PortOne API 키
        requestBody.put("imp_secret", impSecret); // PortOne API Secret

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // JSON 직렬화
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> responseContent = (Map<String, Object>) responseBody.get("response");

                if (responseContent == null || responseContent.get("access_token") == null) {
                    throw new PaymentApiException("토큰 발급 응답에서 access_token을 찾을 수 없습니다.");
                }

                String accessToken = (String) responseContent.get("access_token");
                log.info("PortOne API 토큰 발급 성공: {}", accessToken.substring(0, Math.min(10, accessToken.length())) + "...");

                // 처리 시간 로깅
                Instant end = Instant.now();
                Duration duration = Duration.between(start, end);
                log.debug("PortOne API 토큰 발급 처리 시간: {}ms", duration.toMillis());

                return accessToken; // 발급된 토큰 반환
            } else {
                log.error("PortOne API 토큰 발급 실패: HTTP 상태 코드 {}", response.getStatusCodeValue());
                throw new PaymentApiException("토큰 발급 실패: HTTP 상태 코드 " + response.getStatusCodeValue());
            }
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (RestClientException e) {
            log.error("PortOne API 통신 중 오류 발생: {}", e.getMessage(), e);
            throw new PaymentApiException("결제 서비스 API 통신 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("PortOne API 토큰 발급 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.TOKEN_ISSUANCE_FAILED, e);
        }
    }

    /**
     * 테스트 결제 요청
     * 카드 정보 등록 시 유효성 검증을 위한 테스트 결제
     * @param dto 결제 정보 DTO
     * @return 결제 고유 ID
     * @throws PaymentProcessingException 결제 처리 실패 시
     */
    @Retryable(
            value = {PaymentApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public String requestTestPayment(PaymentInfoCreateDto dto) {
        Instant start = Instant.now();
        String url = BASE_URL + ONE_TIME_PAYMENT_URL;

        log.info("PortOne API 테스트 결제 요청 시작: 카드번호={}", PaymentCardUtils.maskCardNumberForLogging(dto.getCardNumber()));

        // 입력값 검증
        PaymentCardUtils.validateCardInfo(dto);

        String accessToken;
        try {
            accessToken = getAccessToken(); // 인증 토큰 발급
        } catch (PaymentException e) {
            log.error("테스트 결제 요청 중 토큰 발급 실패: {}", e.getMessage());
            throw e; // 토큰 발급 실패 예외 전파
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("pg", "nice");
        requestBody.put("merchant_uid", UUID.randomUUID().toString());
        requestBody.put("method", "card"); // 결제 방식
        requestBody.put("currency", "KRW"); // 결제 통화 설정
        requestBody.put("amount", 100);
        requestBody.put("card_number", dto.getCardNumber());
        requestBody.put("expiry", dto.getExpirationDate());
        requestBody.put("birth", dto.getBirthDate());
        requestBody.put("pwd_2digit", dto.getCardPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer "+ accessToken); // 인증 토큰 포함
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // JSON 직렬화
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            // 디버깅 로그
            log.debug("PortOne API 테스트 결제 요청 정보: URL={}, Headers={}", url, headers);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            // 응답 상태와 내용 로그 출력
            log.debug("PortOne API 테스트 결제 응답: 상태코드={}", response.getStatusCodeValue());

            if (response.getStatusCode() != HttpStatus.OK || !response.getBody().get("code").equals(0)) {
                String errorMessage = "결제 요청 실패: " +
                        (response.getBody().containsKey("message") ? response.getBody().get("message") : "알 수 없는 오류");
                log.error("PortOne API 테스트 결제 요청 실패: {}", errorMessage);
                throw new PaymentProcessingException(errorMessage);
            }

            Map<String, Object> paymentResponse = (Map<String, Object>) response.getBody().get("response");
            String impUid = (String) paymentResponse.get("imp_uid");
            log.info("PortOne API 테스트 결제 성공: impUid={}", impUid);

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("PortOne API 테스트 결제 처리 시간: {}ms", duration.toMillis());

            return impUid;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (RestClientException e) {
            log.error("PortOne API 통신 중 오류 발생: {}", e.getMessage(), e);
            throw new PaymentApiException("결제 서비스 API 통신 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("PortOne API 테스트 결제 요청 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new PaymentProcessingException("테스트 결제 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카드 결제 처리
     * @param paymentInfo 결제 정보 엔티티
     * @param amount 결제 금액
     * @return 결제 응답 정보 맵
     * @throws PaymentProcessingException 결제 처리 실패 시
     */
    @Retryable(
            value = {PaymentApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public Map<String, Object> processCardPayment(PaymentInfo paymentInfo, double amount) {
        Instant start = Instant.now();
        String url = BASE_URL + ONE_TIME_PAYMENT_URL;

        log.info("PortOne API 카드 결제 요청 시작: 금액={}", amount);

        // 입력값 검증
        validatePaymentInfo(paymentInfo, amount);

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (PaymentException e) {
            log.error("카드 결제 요청 중 토큰 발급 실패: {}", e.getMessage());
            throw e; // 토큰 발급 실패 예외 전파
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("merchant_uid", UUID.randomUUID().toString());
        requestBody.put("pg", "nice");
        requestBody.put("method", "card"); // 결제 방식 설정 (수기결제/키인결제)
        requestBody.put("amount", amount);
        requestBody.put("currency", "KRW"); // 결제 통화 설정
        requestBody.put("card_number", paymentInfo.getCardNumber());
        requestBody.put("expiry", paymentInfo.getExpirationDate());
        requestBody.put("birth", paymentInfo.getBirthDate());
        requestBody.put("pwd_2digit", paymentInfo.getCardPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            log.debug("PortOne API 카드 결제 응답: 상태코드={}", response.getStatusCodeValue());

            if (response.getStatusCode() != HttpStatus.OK || !response.getBody().get("code").equals(0)) {
                String errorMessage = "카드 결제 요청 실패: " +
                        (response.getBody().containsKey("message") ? response.getBody().get("message") : "알 수 없는 오류");
                log.error("PortOne API 카드 결제 요청 실패: {}", errorMessage);
                throw new PaymentProcessingException(errorMessage);
            }

            Map<String, Object> paymentResponse = (Map<String, Object>) response.getBody().get("response");
            log.info("PortOne API 카드 결제 성공: impUid={}", paymentResponse.get("imp_uid"));

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("PortOne API 카드 결제 처리 시간: {}ms", duration.toMillis());

            return paymentResponse;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (RestClientException e) {
            log.error("PortOne API 통신 중 오류 발생: {}", e.getMessage(), e);
            throw new PaymentApiException("결제 서비스 API 통신 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("PortOne API 카드 결제 요청 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new PaymentProcessingException("카드 결제 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 결제 환불 처리
     * @param impUid 환불할 결제 고유 ID
     * @return 환불 성공 여부
     * @throws PaymentException 환불 처리 실패 시
     */
    @Retryable(
            value = {PaymentApiException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000)
    )
    public boolean refundPayment(String impUid) {
        Instant start = Instant.now();
        String url = BASE_URL + CANCEL_PAYMENT_URL;

        log.info("PortOne API 결제 환불 요청 시작: impUid={}", impUid);

        if (impUid == null || impUid.isBlank()) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_CANCELLATION_FAILED, "환불할 결제 ID가 없습니다.");
        }

        String accessToken;
        try {
            accessToken = getAccessToken();
        } catch (PaymentException e) {
            log.error("결제 환불 요청 중 토큰 발급 실패: {}", e.getMessage());
            throw e; // 토큰 발급 실패 예외 전파
        }

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_uid", impUid);
        requestBody.put("reason", "사용자 요청에 의한 환불");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            log.debug("PortOne API 결제 환불 응답: 상태코드={}", response.getStatusCodeValue());

            boolean success = response.getStatusCode() == HttpStatus.OK && response.getBody().get("code").equals(0);
            if (success) {
                log.info("PortOne API 결제 환불 성공: impUid={}", impUid);
            } else {
                String errorMessage = "결제 환불 요청 실패: " +
                        (response.getBody().containsKey("message") ? response.getBody().get("message") : "알 수 없는 오류");
                log.error("PortOne API 결제 환불 요청 실패: {}", errorMessage);
            }

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("PortOne API 결제 환불 처리 시간: {}ms", duration.toMillis());

            return success;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (RestClientException e) {
            log.error("PortOne API 통신 중 오류 발생: {}", e.getMessage(), e);
            throw new PaymentApiException("결제 서비스 API 통신 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("PortOne API 결제 환불 요청 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_CANCELLATION_FAILED, "결제 환불 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 테스트 결제 환불
     * @param impUid 환불할 결제 고유 ID
     * @return 환불 성공 여부
     */
    public boolean cancelTestPayment(String impUid) {
        log.info("테스트 결제 환불 요청 시작: impUid={}", impUid);
        return refundPayment(impUid); // 환불 메서드 재사용
    }

    /**
     * 결제 취소 (환불과 동일)
     * @param impUid 취소할 결제 고유 ID
     * @return 취소 성공 여부
     */
    public boolean cancelPayment(String impUid) {
        log.info("결제 취소 요청 시작: impUid={}", impUid);
        return refundPayment(impUid); // 환불 메서드 재사용
    }

    /**
     * 카드번호 마스킹 메서드
     * @param cardNumber 원본 카드번호
     * @return 마스킹된 카드번호
     */
    private String maskCardNumber(String cardNumber) {
        return PaymentCardUtils.maskCardNumber(cardNumber);
    }

    /**
     * 결제 정보 검증 메서드
     * @param paymentInfo 검증할 결제 정보
     * @param amount 결제 금액
     * @throws PaymentException 검증 실패 시
     */
    private void validatePaymentInfo(PaymentInfo paymentInfo, double amount) {
        if (paymentInfo == null) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "결제 수단 정보가 없습니다.");
        }

        if (paymentInfo.getCardNumber() == null || paymentInfo.getCardNumber().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 번호는 필수 입력 값입니다.");
        }

        if (paymentInfo.getExpirationDate() == null || paymentInfo.getExpirationDate().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 유효기간은 필수 입력 값입니다.");
        }

        if (paymentInfo.getBirthDate() == null || paymentInfo.getBirthDate().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "생년월일은 필수 입력 값입니다.");
        }

        if (paymentInfo.getCardPassword() == null || paymentInfo.getCardPassword().isBlank()) {
            throw new PaymentException(PaymentErrorCode.INVALID_CARD_INFO, "카드 비밀번호 앞 2자리는 필수 입력 값입니다.");
        }

        if (amount <= 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT, "결제 금액은 0보다 커야 합니다.");
        }
    }
}