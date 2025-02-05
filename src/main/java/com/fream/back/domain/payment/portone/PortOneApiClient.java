package com.fream.back.domain.payment.portone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.entity.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PortOneApiClient {

    private final RestTemplate restTemplate;

    private static final String BASE_URL = "https://api.iamport.kr";

    @Value("${imp.key}")
    private String impKey;

    @Value("${imp.secret}")
    private String impSecret;

    @Value("${imp.pg}")
    private String pg; // PG 설정 값

    @Value("${imp.storeId}")
    private String storeId;

    public String getAccessToken() {
        String url = BASE_URL + "/users/getToken";

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
                System.out.println("responseContent = " + (String) responseContent.get("access_token"));
                return (String) responseContent.get("access_token"); // 발급된 토큰 반환
            } else {
                throw new IllegalArgumentException("토큰 발급에 실패했습니다. 응답: " + response.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("토큰 발급 중 에러 발생", e);
        }
    }

    public String requestTestPayment(PaymentInfoCreateDto dto) {
        String url = BASE_URL + "/subscribe/payments/onetime";
        String accessToken = getAccessToken(); // 인증 토큰 발급

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
            System.out.println("Serialized JSON Body: " + jsonBody);
            System.out.println("Request URL: " + url);
            System.out.println("Request Body: " + jsonBody);
            System.out.println("Request Headers: " + headers);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            // 응답 상태와 내용 로그 출력
            System.out.println("Response Status: " + response.getStatusCode());
            System.out.println("Response Body: " + response.getBody());

            if (response.getStatusCode() != HttpStatus.OK || !response.getBody().get("code").equals(0)) {
                throw new IllegalArgumentException("결제 요청에 실패했습니다. 응답: " + response.getBody());
            }

            Map<String, Object> paymentResponse = (Map<String, Object>) response.getBody().get("response");
            return (String) paymentResponse.get("imp_uid");
        } catch (Exception e) {
            throw new RuntimeException("결제 요청 중 오류 발생", e);
        }
    }
//    private void validatePaymentInfo(PaymentInfoCreateDto dto) {
//        if (dto.getAmount() == null || dto.getAmount() <= 0) {
//            throw new IllegalArgumentException("결제 금액(amount)은 필수 입력 값입니다.");
//        }
//        if (dto.getCardNumber() == null || dto.getCardNumber().isEmpty()) {
//            throw new IllegalArgumentException("카드 번호(card_number)는 필수 입력 값입니다.");
//        }
//        if (dto.getExpirationDate() == null || dto.getExpirationDate().isEmpty()) {
//            throw new IllegalArgumentException("카드 유효기간(expiry)은 필수 입력 값입니다.");
//        }
//        if (dto.getBirthDate() == null || dto.getBirthDate().isEmpty()) {
//            throw new IllegalArgumentException("생년월일(birth)은 필수 입력 값입니다.");
//        }
//        if (dto.getCardPassword() == null || dto.getCardPassword().isEmpty()) {
//            throw new IllegalArgumentException("카드 비밀번호 앞 2자리(pwd_2digit)는 필수 입력 값입니다.");
//        }
//    }

    public Map<String, Object> processCardPayment(PaymentInfo paymentInfo, double amount) {
        String url = BASE_URL + "/subscribe/payments/onetime";
        String accessToken = getAccessToken();

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

            if (response.getStatusCode() != HttpStatus.OK || !response.getBody().get("code").equals(0)) {
                throw new IllegalArgumentException("결제 요청에 실패했습니다. 응답: " + response.getBody());
            }

            return (Map<String, Object>) response.getBody().get("response");

        } catch (Exception e) {
            throw new RuntimeException("결제 요청 중 오류 발생", e);
        }
    }

    public boolean refundPayment(String impUid) {
        String url = BASE_URL + "/payments/cancel";
        String accessToken = getAccessToken();

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

            if (response.getStatusCode() == HttpStatus.OK && response.getBody().get("code").equals(0)) {
                return true; // 환불 성공
            } else {
                return false; // 환불 실패
            }
        } catch (Exception e) {
            throw new RuntimeException("환불 요청 중 오류 발생", e);
        }
    }

    public boolean cancelTestPayment(String impUid) {
        String url = BASE_URL + "/payments/cancel";
        String accessToken = getAccessToken(); // 인증 토큰 발급

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_uid", impUid);
        requestBody.put("reason", "테스트 결제 환불");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer "+ accessToken); // 인증 토큰 포함
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // JSON 직렬화
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody().get("code").equals(0)) {
                return true; // 환불 성공
            } else {
                return false; // 환불 실패
            }
//            if (response.getStatusCode() != HttpStatus.OK || !response.getBody().get("code").equals(0)) {
//                throw new IllegalArgumentException("결제 환불에 실패했습니다. 응답: " + response.getBody());
//            }
        } catch (Exception e) {
            throw new RuntimeException("결제 환불 중 오류 발생", e);
        }
    }

    public boolean cancelPayment(String impUid) {
        String url = BASE_URL + "/payments/cancel";
        String accessToken = getAccessToken(); // 인증 토큰 발급

        // 요청 본문 생성
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_uid", impUid);
        requestBody.put("reason", "사용자 요청에 의한 환불");

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken); // 인증 토큰 포함
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && (int) response.getBody().get("code") == 0) {
                return true; // 환불 성공
            } else {
                return false; // 환불 실패
            }
        } catch (Exception e) {
            throw new RuntimeException("환불 요청 중 오류 발생: " + e.getMessage());
        }
    }

}
