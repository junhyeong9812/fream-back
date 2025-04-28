package com.fream.back.domain.inspection.service.query;

import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.repository.InspectionStandardImageRepository;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 검수 기준 조회 서비스
 * - 캐싱 적용으로 성능 향상
 * - BaseTimeEntity 필드명 일치 (createdDate, modifiedDate)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InspectionStandardQueryService {

    private final InspectionStandardRepository inspectionStandardRepository;
    private final InspectionStandardImageRepository inspectionStandardImageRepository;

    /**
     * 모든 검수 기준 페이징 조회
     * - 캐싱 적용
     */
    @Cacheable(value = "inspectionStandards", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<InspectionStandardResponseDto> getStandards(Pageable pageable) {
        try {
            log.debug("모든 검수 기준 조회: page={}, size={}",
                    pageable.getPageNumber(), pageable.getPageSize());

            // 기존의 findAll 대신 findAllWithPaging 사용하여 N+1 문제 해결
            return inspectionStandardRepository.findAllWithPaging(pageable)
                    .map(this::toResponseDto);
        } catch (Exception e) {
            log.error("검수 기준 목록 조회 중 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리별 검수 기준 조회
     * - 캐싱 적용
     */
    @Cacheable(value = "inspectionStandardsByCategory", key = "#category.name() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<InspectionStandardResponseDto> getStandardsByCategory(InspectionCategory category, Pageable pageable) {
        if (category == null) {
            throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_CATEGORY,
                    "검수 기준 카테고리는 필수입니다.");
        }

        try {
            log.debug("카테고리별 검수 기준 조회: category={}, page={}, size={}",
                    category, pageable.getPageNumber(), pageable.getPageSize());

            return inspectionStandardRepository.findByCategory(category, pageable)
                    .map(this::toResponseDto);
        } catch (Exception e) {
            log.error("카테고리별 검수 기준 조회 중 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "카테고리별 검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 검색
     * - 캐싱 적용
     */
    @Cacheable(value = "inspectionStandardSearchResults", key = "#keyword + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<InspectionStandardResponseDto> searchStandards(String keyword, Pageable pageable) {
        try {
            log.debug("검수 기준 검색: keyword={}, page={}, size={}",
                    keyword, pageable.getPageNumber(), pageable.getPageSize());

            return inspectionStandardRepository.searchStandards(keyword, pageable)
                    .map(this::toResponseDto);
        } catch (Exception e) {
            log.error("검수 기준 검색 중 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 단일 검수 기준 조회
     * - 캐싱 적용
     */
    @Cacheable(value = "inspectionStandardDetail", key = "#id")
    public InspectionStandardResponseDto getStandard(Long id) {
        if (id == null) {
            throw new InspectionNotFoundException("조회할 검수 기준 ID가 필요합니다.");
        }

        try {
            log.debug("단일 검수 기준 조회: ID={}", id);

            // N+1 문제 해결을 위해 조인 쿼리 사용
            InspectionStandard standard = inspectionStandardRepository.findWithImagesById(id)
                    .orElseThrow(() -> new InspectionNotFoundException("ID가 " + id + "인 검수 기준을 찾을 수 없습니다."));

            return toResponseDto(standard);
        } catch (InspectionNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("검수 기준 조회 중 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 엔티티를 DTO로 변환
     * - BaseTimeEntity 필드명 일치 (createdDate, modifiedDate)
     */
    private InspectionStandardResponseDto toResponseDto(InspectionStandard standard) {
        try {
            List<String> imageUrls = inspectionStandardImageRepository
                    .findAllByInspectionStandardId(standard.getId())
                    .stream()
                    .map(image -> image.getImageUrl())
                    .collect(Collectors.toList());

            return InspectionStandardResponseDto.builder()
                    .id(standard.getId())
                    .category(standard.getCategory().name())
                    .content(standard.getContent())
                    .imageUrls(imageUrls)
                    .createdDate(standard.getCreatedDate()) // BaseTimeEntity 필드명에 맞게 수정
                    .modifiedDate(standard.getModifiedDate()) // BaseTimeEntity 필드명에 맞게 수정
                    .build();
        } catch (Exception e) {
            log.error("검수 기준 DTO 변환 중 오류: standard_id={}", standard.getId(), e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 정보를 변환하는 중 오류가 발생했습니다.", e);
        }
    }
}