package com.fream.back.domain.chatQuestion.service;

import com.fream.back.domain.chatQuestion.dto.gpt.GPTRequestDto;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.global.config.GPTConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService {

    private final RestTemplate restTemplate;
    private final GPTConfig gptConfig;

    // 기존 메서드 - 문자열만 반환
    public String getGPTResponse(String question, List<FAQResponseDto> faqList) {
        try {
            GPTResponseDto response = getGPTResponseWithUsage(question, faqList);
            return response.getAnswer();
        } catch (Exception e) {
            log.error("GPT API 호출 중 오류 발생: {}", e.getMessage(), e);
            return "서비스 연결 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    // 토큰 사용량 포함한 전체 응답 반환
    public GPTResponseDto getGPTResponseWithUsage(String question, List<FAQResponseDto> faqList) {
        try {
            log.info("GPT API 호출 시작: 질문=\"{}\"", question.length() > 50 ? question.substring(0, 47) + "..." : question);

            // FAQ 데이터를 문자열 리스트로 변환
            List<String> faqData = formatFAQData(faqList);
            log.debug("FAQ 데이터 변환 완료: {} 개 FAQ 항목", faqData.size());

            // API 요청 DTO 생성
            GPTRequestDto requestDto = GPTRequestDto.of(
                    gptConfig.getModel(),
                    question,
                    faqData
            );

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(gptConfig.getApiKey());

            // HTTP 엔티티 생성
            HttpEntity<GPTRequestDto> requestEntity = new HttpEntity<>(requestDto, headers);

            // API 호출 및 응답 받기
            log.debug("OpenAI API 호출 중: 모델={}", gptConfig.getModel());
            GPTResponseDto response = restTemplate.postForObject(
                    gptConfig.getApiUrl(),
                    requestEntity,
                    GPTResponseDto.class
            );

            // 응답 반환
            if (response != null) {
                log.info("GPT API 응답 성공: 모델={}, 토큰 사용량={}",
                        response.getModel(),
                        response.getUsage() != null ? response.getUsage().getTotal_tokens() : "알 수 없음");
                return response;
            } else {
                log.warn("GPT API 응답이 null입니다.");
                GPTResponseDto errorResponse = new GPTResponseDto();
                errorResponse.setModel(gptConfig.getModel());
                return errorResponse;
            }

        } catch (Exception e) {
            log.error("GPT API 호출 중 오류 발생: {} - {}", e.getClass().getName(), e.getMessage(), e);
            throw e;
        }
    }

    // FAQ 데이터를 GPT 프롬프트에 적합한 형식으로 변환
    private List<String> formatFAQData(List<FAQResponseDto> faqList) {
        List<String> formatted = new ArrayList<>();

        for (FAQResponseDto faq : faqList) {
            StringBuilder sb = new StringBuilder();
            sb.append("카테고리: ").append(faq.getCategory()).append("\n");
            sb.append("질문: ").append(faq.getQuestion()).append("\n");
            sb.append("답변: ").append(faq.getAnswer()).append("\n");

            formatted.add(sb.toString());
        }

        return formatted;
    }
}