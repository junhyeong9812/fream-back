package com.fream.back.domain.inquiry.service.query;

import com.fream.back.domain.inquiry.dto.InquiryResponseDto;
import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryImage;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.exception.InquiryErrorCode;
import com.fream.back.domain.inquiry.exception.InquiryException;
import com.fream.back.domain.inquiry.exception.InquiryNotFoundException;
import com.fream.back.domain.inquiry.repository.InquiryImageRepository;
import com.fream.back.domain.inquiry.repository.InquiryRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 1대1 문의 쿼리 서비스 구현체
 * 문의 조회 기능 구현
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InquiryQueryServiceImpl implements InquiryQueryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final UserRepository userRepository;

    @Override
    public InquiryResponseDto getInquiry(Long inquiryId, Long userId, boolean isAdmin) {
        try {
            // 1. 문의 조회
            Inquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new InquiryNotFoundException("ID가 " + inquiryId + "인 문의를 찾을 수 없습니다."));

            // 2. 권한 확인 (본인 문의 또는 관리자만 조회 가능)
            if (inquiry.isPrivate() && !isAdmin && !inquiry.getUser().getId().equals(userId)) {
                throw new InquiryException(InquiryErrorCode.INQUIRY_PRIVATE, "비공개 문의는 작성자와 관리자만 조회할 수 있습니다.");
            }

            // 3. 이미지 조회
            List<InquiryImage> images = inquiryImageRepository.findAllByInquiryId(inquiryId);

            // 4. 응답 DTO 생성
            return InquiryResponseDto.from(inquiry, images);
        } catch (InquiryException e) {
            // InquiryNotFoundException도 InquiryException의 하위 클래스이므로 이 catch에서 처리됩니다.
            throw e;
        } catch (Exception e) {
            log.error("문의 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "문의 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Page<InquirySearchResultDto> getInquiries(InquirySearchCondition condition, Pageable pageable) {
        try {
            return inquiryRepository.searchInquiries(condition, pageable);
        } catch (Exception e) {
            log.error("문의 목록 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "문의 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Page<InquirySearchResultDto> getUserInquiries(Long userId, Pageable pageable) {
        try {
            // 사용자 존재 확인
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("ID가 " + userId + "인 사용자를 찾을 수 없습니다."));

            // 검색 조건 생성
            InquirySearchCondition condition = InquirySearchCondition.forUser(userId);

            return inquiryRepository.searchInquiries(condition, pageable);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 문의 목록 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "사용자 문의 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Page<InquirySearchResultDto> getInquiriesByStatus(InquiryStatus status, Pageable pageable) {
        try {
            // 검색 조건 생성
            InquirySearchCondition condition = InquirySearchCondition.builder()
                    .status(status)
                    .isAdmin(true) // 관리자 권한으로 조회 (비공개 문의 포함)
                    .build();

            return inquiryRepository.searchInquiries(condition, pageable);
        } catch (Exception e) {
            log.error("상태별 문의 목록 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "상태별 문의 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Page<InquirySearchResultDto> getInquiriesByCategory(InquiryCategory category, Pageable pageable) {
        try {
            // 검색 조건 생성
            InquirySearchCondition condition = InquirySearchCondition.builder()
                    .category(category)
                    .isAdmin(false) // 일반 사용자 권한으로 조회 (공개 문의만)
                    .build();

            return inquiryRepository.searchInquiries(condition, pageable);
        } catch (Exception e) {
            log.error("카테고리별 문의 목록 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "카테고리별 문의 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Page<InquirySearchResultDto> searchInquiries(String keyword, Pageable pageable) {
        try {
            return inquiryRepository.findByTitleOrContentContaining(keyword, pageable);
        } catch (Exception e) {
            log.error("문의 검색 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "문의 검색 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Page<InquirySearchResultDto> getPendingInquiries(Pageable pageable) {
        try {
            return inquiryRepository.findUnansweredInquiriesOrderByOldest(pageable);
        } catch (Exception e) {
            log.error("답변 대기 문의 목록 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "답변 대기 문의 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public Object getInquiryStatistics() {
        try {
            return inquiryRepository.getInquiryStatistics();
        } catch (Exception e) {
            log.error("문의 통계 조회 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND, "문의 통계 조회 중 오류가 발생했습니다.", e);
        }
    }
}