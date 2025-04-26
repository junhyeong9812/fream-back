package com.fream.back.domain.chatQuestion.service;

import com.fream.back.domain.chatQuestion.dto.gpt.GPTRequestDto;
import com.fream.back.domain.chatQuestion.dto.gpt.GPTResponseDto;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionErrorCode;
import com.fream.back.domain.chatQuestion.exception.GPTApiException;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.global.config.GPTConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * GPT API 통신 서비스
 * OpenAI GPT API와의 통신을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GPTService {

    private final RestTemplate restTemplate;
    private final GPTConfig gptConfig;

    /**
     * 기존 메서드 - 문자열만 반환
     * 간단한 사용 시나리오를 위한 간편 메서드
     *
     * @param question 사용자 질문
     * @param faqList FAQ 데이터 목록
     * @return GPT 응답 문자열
     */
    public String getGPTResponse(String question, List<FAQResponseDto> faqList) {
        try {
            validateQuestion(question);
            GPTResponseDto response = getGPTResponseWithUsage(question, faqList);
            return response.getAnswer();
        } catch (GPTApiException e) {
            log.error("GPT API 호출 중 에러: {}", e.getMessage());
            return "질문 처리 중 오류가 발생했습니다: " + e.getMessage();
        } catch (Exception e) {
            log.error("GPT API 호출 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            return "서비스 연결 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    /**
     * 토큰 사용량 포함한 전체 응답 반환
     * 토큰 사용량 추적이 필요한 상황을 위한 메서드
     * 네트워크 또는 서버 오류 시 자동 재시도 (최대 3회, 지수 백오프)
     *
     * @param question 사용자 질문
     * @param faqList FAQ 데이터 목록
     * @return GPT 응답 DTO
     */
    @Retryable(
            value = {ResourceAccessException.class, HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public GPTResponseDto getGPTResponseWithUsage(String question, List<FAQResponseDto> faqList) {
        validateQuestion(question);

        try {
            log.info("GPT API 호출 시작: 질문=\"{}\"", truncateQuestion(question));

            // FAQ 데이터를 문자열 리스트로 변환
            List<String> faqData = formatFAQData(faqList);
            log.debug("FAQ 데이터 변환 완료: {} 개 FAQ 항목", faqData.size());

            // API 요청 DTO 생성
            GPTRequestDto requestDto = GPTRequestDto.of(
                    gptConfig.getModel(),
                    question,
                    faqData,
                    true
            );

            // HTTP 헤더 및 엔티티 생성
            HttpEntity<GPTRequestDto> requestEntity = createHttpRequestEntity(requestDto);

            // API 호출 및 응답 받기
            log.debug("OpenAI API 호출 중: 모델={}", gptConfig.getModel());
            GPTResponseDto response = callGptApi(requestEntity);

            // 응답 로깅 및 반환
            logApiResponse(response);
            return response;
        } catch (GPTApiException e) {
            // 이미 처리된 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("GPT API 호출 중 예상치 못한 오류 발생: {} - {}", e.getClass().getName(), e.getMessage(), e);
            throw new GPTApiException(ChatQuestionErrorCode.GPT_API_ERROR,
                    "GPT API 호출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 질문 유효성 검사
     *
     * @param question 사용자 질문
     * @throws GPTApiException 질문이 유효하지 않은 경우
     */
    private void validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new GPTApiException(ChatQuestionErrorCode.INVALID_QUESTION_DATA, "질문 내용이 비어있습니다.");
        }

        // 질문 길이 제한 확인 (예: 2000자)
        if (question.length() > 2000) {
            throw new GPTApiException(ChatQuestionErrorCode.QUESTION_LENGTH_EXCEEDED,
                    "질문 길이가 제한을 초과했습니다. (최대 2000자)");
        }
    }

    /**
     * 로그 출력용 질문 내용 축약
     *
     * @param question 원본 질문
     * @return 축약된 질문 (50자 초과 시 ...으로 표시)
     */
    private String truncateQuestion(String question) {
        return question.length() > 50 ? question.substring(0, 47) + "..." : question;
    }

    /**
     * HTTP 요청 엔티티 생성
     *
     * @param requestDto GPT 요청 DTO
     * @return HTTP 요청 엔티티
     */
    private HttpEntity<GPTRequestDto> createHttpRequestEntity(GPTRequestDto requestDto) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(gptConfig.getApiKey());
        return new HttpEntity<>(requestDto, headers);
    }

    /**
     * GPT API 호출
     *
     * @param requestEntity HTTP 요청 엔티티
     * @return GPT 응답 DTO
     * @throws GPTApiException API 호출 실패 시
     */
    private GPTResponseDto callGptApi(HttpEntity<GPTRequestDto> requestEntity) {
        try {
            return restTemplate.postForObject(
                    gptConfig.getApiUrl(),
                    requestEntity,
                    GPTResponseDto.class
            );
        } catch (HttpClientErrorException e) {
            // 4xx 에러 (클라이언트 오류)
            log.error("GPT API 클라이언트 에러: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);

            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                // 429 - 사용량 제한 초과
                throw new GPTApiException(ChatQuestionErrorCode.GPT_USAGE_LIMIT_EXCEEDED,
                        "GPT API 사용량 제한이 초과되었습니다. 잠시 후 다시 시도해주세요.", e);
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                // 401 - 인증 오류
                throw new GPTApiException(ChatQuestionErrorCode.GPT_API_ERROR,
                        "GPT API 인증에 실패했습니다.", e);
            } else {
                throw new GPTApiException(ChatQuestionErrorCode.GPT_API_ERROR,
                        "GPT API 호출 중 클라이언트 오류가 발생했습니다: " + e.getStatusText(), e);
            }
        } catch (HttpServerErrorException e) {
            // 5xx 에러 (서버 오류)
            log.error("GPT API 서버 에러: {} - {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new GPTApiException(ChatQuestionErrorCode.GPT_API_ERROR,
                    "GPT API 서버에 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (ResourceAccessException e) {
            // 네트워크 오류
            log.error("GPT API 네트워크 오류: {}", e.getMessage(), e);
            throw new GPTApiException(ChatQuestionErrorCode.GPT_API_ERROR,
                    "GPT API 서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.", e);
        }
    }

    /**
     * API 응답 로깅
     *
     * @param response GPT 응답 DTO
     */
    private void logApiResponse(GPTResponseDto response) {
        if (response != null) {
            log.info("GPT API 응답 성공: 모델={}, 토큰 사용량={}",
                    response.getModel(),
                    response.getUsage() != null ? response.getUsage().getTotal_tokens() : "알 수 없음");
        } else {
            log.warn("GPT API 응답이 null입니다.");
            throw new GPTApiException(ChatQuestionErrorCode.GPT_RESPONSE_PROCESSING_ERROR,
                    "GPT API가 유효한 응답을 반환하지 않았습니다.");
        }
    }

    /**
     * FAQ 데이터를 GPT 프롬프트에 적합한 형식으로 변환
     *
     * @param faqList FAQ 데이터 목록
     * @return 포맷팅된 FAQ 데이터 목록
     */
    private List<String> formatFAQData(List<FAQResponseDto> faqList) {
        List<String> formatted = new ArrayList<>();

        if (faqList == null) {
            return formatted;
        }

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