package com.fream.back.domain.faq.service.query;

import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQNotFoundException;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.repository.FAQImageRepository;
import com.fream.back.domain.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FAQQueryService {

    private final FAQRepository faqRepository;
    private final FAQImageRepository faqImageRepository;

    // FAQ 페이징 조회
    public Page<FAQResponseDto> getFAQs(Pageable pageable) {
        try {
            return faqRepository.findAll(pageable).map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("FAQ 목록 조회 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "FAQ 목록을 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 목록 조회 중 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "FAQ 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    // FAQ 단일 조회
    public FAQResponseDto getFAQ(Long id) {
        try {
            FAQ faq = faqRepository.findWithImagesById(id)
                    .orElseThrow(() -> new FAQNotFoundException("ID가 " + id + "인 FAQ를 찾을 수 없습니다."));
            return toResponseDto(faq);
        } catch (FAQNotFoundException e) {
            // 이미 생성된 FAQ 예외는 그대로 던짐
            throw e;
        } catch (DataAccessException e) {
            log.error("FAQ 조회 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "FAQ를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 조회 중 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "FAQ를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    // FAQ 카테고리별 목록 조회
    public Page<FAQResponseDto> getFAQsByCategory(FAQCategory category, Pageable pageable) {
        try {
            return faqRepository.findByCategory(category, pageable).map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("카테고리별 FAQ 조회 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "카테고리별 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("카테고리별 FAQ 조회 중 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "카테고리별 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    // FAQ 검색
    public Page<FAQResponseDto> searchFAQs(String keyword, Pageable pageable) {
        try {
            return faqRepository.searchFAQs(keyword, pageable).map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("FAQ 검색 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "FAQ 검색 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 검색 중 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "FAQ 검색 중 오류가 발생했습니다.", e);
        }
    }

    // 모든 FAQ 조회
    public List<FAQResponseDto> getAllFAQs() {
        try {
            return faqRepository.findAll().stream()
                    .map(this::toResponseDto)
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("전체 FAQ 조회 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "전체 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("전체 FAQ 조회 중 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR,
                    "전체 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    private FAQResponseDto toResponseDto(FAQ faq) {
        return FAQResponseDto.builder()
                .id(faq.getId())
                .category(faq.getCategory().name())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .imageUrls(faqImageRepository.findAllByFaqId(faq.getId())
                        .stream()
                        .map(image -> image.getImageUrl())
                        .collect(Collectors.toList()))
                .build();
    }
}