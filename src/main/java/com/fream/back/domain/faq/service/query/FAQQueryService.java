package com.fream.back.domain.faq.service.query;

import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.repository.FAQImageRepository;
import com.fream.back.domain.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FAQQueryService {

    private final FAQRepository faqRepository;
    private final FAQImageRepository faqImageRepository;

    // FAQ 페이징 조회
    public Page<FAQResponseDto> getFAQs(Pageable pageable) {
        return faqRepository.findAll(pageable).map(this::toResponseDto);
    }

    // FAQ 단일 조회
    public FAQResponseDto getFAQ(Long id) {
        FAQ faq = faqRepository.findWithImagesById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다."));
        return toResponseDto(faq);
    }

    // FAQ 카테고리별 목록 조회
    public Page<FAQResponseDto> getFAQsByCategory(FAQCategory category, Pageable pageable) {
        return faqRepository.findByCategory(category, pageable).map(this::toResponseDto);
    }

    // FAQ 검색
    public Page<FAQResponseDto> searchFAQs(String keyword, Pageable pageable) {
        return faqRepository.searchFAQs(keyword, pageable).map(this::toResponseDto);
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
