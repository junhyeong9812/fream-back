package com.fream.back.domain.faq.service.query;

import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.entity.FAQImage;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQNotFoundException;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.repository.FAQImageRepository;
import com.fream.back.domain.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FAQQueryService {

    private final FAQRepository faqRepository;
    private final FAQImageRepository faqImageRepository;

    /**
     * FAQ 페이징 조회
     */
    @Cacheable(value = "faqList", key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<FAQResponseDto> getFAQs(Pageable pageable) {
        try {
            log.debug("전체 FAQ 목록 조회: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

            return faqRepository.findAll(pageable)
                    .map(faq -> {
                        List<FAQImage> images = faqImageRepository.findAllByFaqId(faq.getId());
                        return FAQResponseDto.from(faq, images);
                    });
        } catch (DataAccessException e) {
            log.error("FAQ 목록 조회 중 데이터베이스 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "FAQ 목록을 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 목록 조회 중 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "FAQ 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * FAQ 단일 조회
     */
    @Cacheable(value = "faqDetail", key = "#id")
    public FAQResponseDto getFAQ(Long id) {
        try {
            log.debug("FAQ 단일 조회: id={}", id);

            FAQ faq = faqRepository.findById(id)
                    .orElseThrow(() -> new FAQNotFoundException("ID가 " + id + "인 FAQ를 찾을 수 없습니다."));

            List<FAQImage> images = faqImageRepository.findAllByFaqId(id);
            return FAQResponseDto.from(faq, images);
        } catch (FAQNotFoundException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("FAQ 조회 중 데이터베이스 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "FAQ를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 조회 중 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "FAQ를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * FAQ 카테고리별 목록 조회
     */
    @Cacheable(value = "faqCategoryList", key = "#category.name() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<FAQResponseDto> getFAQsByCategory(FAQCategory category, Pageable pageable) {
        try {
            log.debug("카테고리별 FAQ 목록 조회: category={}, page={}, size={}",
                    category, pageable.getPageNumber(), pageable.getPageSize());

            return faqRepository.findByCategory(category, pageable)
                    .map(faq -> {
                        List<FAQImage> images = faqImageRepository.findAllByFaqId(faq.getId());
                        return FAQResponseDto.from(faq, images);
                    });
        } catch (DataAccessException e) {
            log.error("카테고리별 FAQ 조회 중 데이터베이스 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "카테고리별 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("카테고리별 FAQ 조회 중 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "카테고리별 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * FAQ 검색
     */
    @Cacheable(value = "faqSearchResults", key = "'search:' + (T(java.util.Objects).toString(#keyword)) + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<FAQResponseDto> searchFAQs(String keyword, Pageable pageable) {
        try {
            log.debug("FAQ 검색: keyword={}, page={}, size={}",
                    keyword, pageable.getPageNumber(), pageable.getPageSize());

            return faqRepository.searchFAQs(keyword, pageable)
                    .map(faq -> {
                        List<FAQImage> images = faqImageRepository.findAllByFaqId(faq.getId());
                        return FAQResponseDto.from(faq, images);
                    });
        } catch (DataAccessException e) {
            log.error("FAQ 검색 중 데이터베이스 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "FAQ 검색 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("FAQ 검색 중 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "FAQ 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 FAQ 조회 (페이징 없음)
     */
    @Cacheable(value = "faqList", key = "'all'")
    public List<FAQResponseDto> getAllFAQs() {
        try {
            log.debug("전체 FAQ 목록 조회 (페이징 없음)");

            return faqRepository.findAll().stream()
                    .map(faq -> {
                        List<FAQImage> images = faqImageRepository.findAllByFaqId(faq.getId());
                        return FAQResponseDto.from(faq, images);
                    })
                    .collect(Collectors.toList());
        } catch (DataAccessException e) {
            log.error("전체 FAQ 조회 중 데이터베이스 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "전체 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("전체 FAQ 조회 중 오류: {}", e.getMessage());
            throw new FAQException(FAQErrorCode.FAQ_QUERY_ERROR, "전체 FAQ를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}