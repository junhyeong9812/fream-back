package com.fream.back.domain.inspection.service.query;

import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionFileException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.repository.InspectionStandardImageRepository;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
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
public class InspectionStandardQueryService {

    private final InspectionStandardRepository inspectionStandardRepository;
    private final InspectionStandardImageRepository inspectionStandardImageRepository;

    /**
     * 모든 검수 기준 페이징 조회
     */
    public Page<InspectionStandardResponseDto> getStandards(Pageable pageable) {
        try {
            log.debug("모든 검수 기준 조회: page={}, size={}",
                    pageable.getPageNumber(), pageable.getPageSize());

            return inspectionStandardRepository.findAll(pageable)
                    .map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("검수 기준 목록 조회 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 목록을 조회하는 중 오류가 발생했습니다.", e);
        } catch (InspectionFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("검수 기준 목록 조회 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 관련 예외는 그대로 전파
            log.error("검수 기준 목록 조회 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("검수 기준 목록 조회 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리별 검수 기준 조회
     */
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
        } catch (DataAccessException e) {
            log.error("카테고리별 검수 기준 조회 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "카테고리별 검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        } catch (InspectionFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("카테고리별 검수 기준 조회 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 관련 예외는 그대로 전파
            log.error("카테고리별 검수 기준 조회 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("카테고리별 검수 기준 조회 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "카테고리별 검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 검색
     */
    public Page<InspectionStandardResponseDto> searchStandards(String keyword, Pageable pageable) {
        try {
            log.debug("검수 기준 검색: keyword={}, page={}, size={}",
                    keyword, pageable.getPageNumber(), pageable.getPageSize());

            return inspectionStandardRepository.searchStandards(keyword, pageable)
                    .map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("검수 기준 검색 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 검색 중 오류가 발생했습니다.", e);
        } catch (InspectionFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("검수 기준 검색 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 관련 예외는 그대로 전파
            log.error("검수 기준 검색 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("검수 기준 검색 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 단일 검수 기준 조회
     */
    public InspectionStandardResponseDto getStandard(Long id) {
        if (id == null) {
            throw new InspectionNotFoundException("조회할 검수 기준 ID가 필요합니다.");
        }

        try {
            log.debug("단일 검수 기준 조회: ID={}", id);

            InspectionStandard standard = inspectionStandardRepository.findById(id)
                    .orElseThrow(() -> new InspectionNotFoundException("ID가 " + id + "인 검수 기준을 찾을 수 없습니다."));

            return toResponseDto(standard);
        } catch (InspectionNotFoundException e) {
            log.warn("검수 기준 조회 중 기준을 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("검수 기준 조회 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        } catch (InspectionFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("검수 기준 조회 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 관련 예외는 그대로 전파
            log.error("검수 기준 조회 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("검수 기준 조회 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 엔티티를 DTO로 변환
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
                    .build();
        } catch (DataAccessException e) {
            log.error("검수 기준 DTO 변환 중 이미지 조회 오류: standard_id={}", standard.getId(), e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 이미지 정보를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("검수 기준 DTO 변환 중 예상치 못한 오류: standard_id={}", standard.getId(), e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 정보를 변환하는 중 오류가 발생했습니다.", e);
        }
    }
}