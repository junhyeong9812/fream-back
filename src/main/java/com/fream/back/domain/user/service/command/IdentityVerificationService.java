package com.fream.back.domain.user.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fream.back.domain.user.dto.VerifiedCustomerDto;
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
        try {
            // API 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "PortOne " + portOneApiSecret);
            headers.set("Content-Type", "application/json");

            HttpEntity<?> entity = new HttpEntity<>(headers);

            // PortOne API 호출
            String url = "https://api.portone.io/identity-verifications/" + identityVerificationId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // 응답 파싱
            JsonNode rootNode = objectMapper.readTree(response.getBody());

            // 인증 상태 확인
            String status = rootNode.path("status").asText();
            if (!"VERIFIED".equals(status)) {
                throw new IllegalStateException("본인인증이 완료되지 않았습니다. 상태: " + status);
            }

            // 인증된 고객 정보 추출
            JsonNode customerNode = rootNode.path("verifiedCustomer");

            return VerifiedCustomerDto.builder()
                    .name(customerNode.path("name").asText())
                    .phoneNumber(customerNode.path("phoneNumber").asText())
                    .gender(customerNode.path("gender").asText())
                    .birthDate(customerNode.path("birthDate").asText())
                    .isForeigner(customerNode.path("isForeigner").asBoolean())
                    .ci(customerNode.path("ci").asText(""))
                    .di(customerNode.path("di").asText(""))
                    .build();

        } catch (Exception e) {
            log.error("본인인증 검증 중 오류 발생", e);
            throw new RuntimeException("본인인증 정보를 검증할 수 없습니다.", e);
        }
    }
}