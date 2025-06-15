package com.fream.back.domain.user.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.user.dto.VerifiedCustomerDto;
import com.fream.back.domain.user.exception.IdentityVerificationFailedException;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${imp.secret}")
    private String portOneApiSecret;

    /**
     * PortOne API를 통해 본인인증 정보를 검증하고 가져옴
     * @param identityVerificationId 본인인증 ID
     * @return 검증된 고객 정보
     */
    public VerifiedCustomerDto verifyIdentity(String identityVerificationId) {
        log.info("본인인증 검증 시작: verificationId={}", identityVerificationId);

        try {
            // API 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "PortOne " + portOneApiSecret);
            headers.set("Content-Type", "application/json");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            // PortOne API 호출
            String url = "https://api.portone.io/identity-verifications/" + identityVerificationId;
            log.debug("PortOne API 호출: url={}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.debug("PortOne API 응답 수신: status={}", response.getStatusCode());

            // 응답 파싱
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            // 인증 상태 확인
            String status = rootNode.path("status").asText();
            if (!"VERIFIED".equals(status)) {
                log.warn("본인인증 미완료: verificationId={}, status={}", identityVerificationId, status);
                throw new IdentityVerificationFailedException(identityVerificationId, "본인인증이 완료되지 않았습니다. 상태: " + status);
            }

            // 인증된 고객 정보 추출
            JsonNode customerNode = rootNode.path("verifiedCustomer");

            VerifiedCustomerDto customerDto = VerifiedCustomerDto.builder()
                    .name(customerNode.path("name").asText())
                    .phoneNumber(customerNode.path("phoneNumber").asText())
                    .gender(customerNode.path("gender").asText())
                    .birthDate(customerNode.path("birthDate").asText())
                    .isForeigner(customerNode.path("isForeigner").asBoolean())
                    .ci(customerNode.path("ci").asText(""))
                    .di(customerNode.path("di").asText(""))
                    .build();

            log.info("본인인증 검증 완료: verificationId={}, name={}, phoneNumber={}****",
                    identityVerificationId,
                    customerDto.getName(),
                    customerDto.getPhoneNumber() != null && customerDto.getPhoneNumber().length() > 4
                            ? customerDto.getPhoneNumber().substring(0, 4) : "****");

            return customerDto;

        } catch (IdentityVerificationFailedException e) {
            throw e;
        } catch (Exception e) {
            log.error("본인인증 검증 중 시스템 오류: verificationId={}", identityVerificationId, e);
            throw new IdentityVerificationFailedException(identityVerificationId, "본인인증 정보를 검증할 수 없습니다.");
        }
    }
}