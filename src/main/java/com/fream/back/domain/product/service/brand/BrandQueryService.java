package com.fream.back.domain.product.service.brand;

import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 브랜드 조회(Query) 서비스
 * 브랜드 조회 관련 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BrandQueryService {

    private final BrandRepository brandRepository;

    /**
     * 브랜드명으로 브랜드 조회
     *
     * @param name 브랜드명
     * @return 브랜드 응답 DTO
     * @throws ProductException 브랜드가 존재하지 않을 경우
     */
    public BrandResponseDto findByName(String name) {
        log.info("브랜드명으로 브랜드 조회 - 브랜드명: {}", name);

        try {
            Brand brand = brandRepository.findByName(name)
                    .orElseThrow(() -> {
                        log.warn("브랜드 조회 실패 - 존재하지 않는 브랜드명: {}", name);
                        return new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                                "해당 브랜드가 존재하지 않습니다: " + name);
                    });

            log.debug("브랜드 조회 성공 - 브랜드ID: {}, 브랜드명: {}", brand.getId(), brand.getName());
            return BrandResponseDto.fromEntity(brand); // 엔티티 -> DTO 변환
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("브랜드 조회 중 예상치 못한 오류 발생 - 브랜드명: {}", name, e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 브랜드 조회
     *
     * @return 브랜드 응답 DTO 목록
     */
    public List<BrandResponseDto> findAllBrands() {
        log.info("모든 브랜드 조회 요청");

        try {
            List<Brand> brands = brandRepository.findAllByOrderByNameDesc();

            List<BrandResponseDto> brandDtos = brands.stream()
                    .map(BrandResponseDto::fromEntity) // 엔티티 -> DTO 변환
                    .collect(Collectors.toList());

            log.info("모든 브랜드 조회 성공 - 브랜드 수: {}", brandDtos.size());
            return brandDtos;
        } catch (Exception e) {
            log.error("모든 브랜드 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ID로 브랜드 엔티티 조회
     *
     * @param brandId 브랜드 ID
     * @return 브랜드 엔티티
     * @throws ProductException 브랜드가 존재하지 않을 경우
     */
    public Brand findById(Long brandId) {
        log.debug("ID로 브랜드 엔티티 조회 - 브랜드ID: {}", brandId);

        return brandRepository.findById(brandId)
                .orElseThrow(() -> {
                    log.warn("브랜드 엔티티 조회 실패 - 존재하지 않는 브랜드ID: {}", brandId);
                    return new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                            "해당 브랜드가 존재하지 않습니다. ID: " + brandId);
                });
    }

    /**
     * ID로 브랜드 DTO 조회
     *
     * @param id 브랜드 ID
     * @return 브랜드 응답 DTO
     * @throws ProductException 브랜드가 존재하지 않을 경우
     */
    public BrandResponseDto findBrandById(Long id) {
        log.info("ID로 브랜드 조회 - 브랜드ID: {}", id);

        try {
            Brand brand = brandRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("브랜드 조회 실패 - 존재하지 않는 브랜드ID: {}", id);
                        return new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                                "해당 브랜드가 존재하지 않습니다. ID: " + id);
                    });

            log.debug("브랜드 조회 성공 - 브랜드ID: {}, 브랜드명: {}", id, brand.getName());
            return BrandResponseDto.fromEntity(brand);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("브랜드 조회 중 예상치 못한 오류 발생 - 브랜드ID: {}", id, e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 페이징으로 브랜드 조회
     *
     * @param pageable 페이징 정보
     * @return 페이징된 브랜드 응답 DTO
     */
    public Page<BrandResponseDto> findBrandsPaging(Pageable pageable) {
        log.info("페이징으로 브랜드 조회 - 페이지: {}, 사이즈: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<Brand> brandPage = brandRepository.findAll(pageable);
            Page<BrandResponseDto> dtoPage = brandPage.map(BrandResponseDto::fromEntity);

            log.info("페이징으로 브랜드 조회 성공 - 총 브랜드 수: {}, 총 페이지 수: {}",
                    dtoPage.getTotalElements(), dtoPage.getTotalPages());
            return dtoPage;
        } catch (Exception e) {
            log.error("페이징으로 브랜드 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드명으로 검색 (페이징)
     *
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 브랜드 응답 DTO
     */
    public Page<BrandResponseDto> searchBrandsByName(String keyword, Pageable pageable) {
        log.info("브랜드명으로 검색 - 키워드: {}, 페이지: {}, 사이즈: {}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<Brand> brandPage = brandRepository.findByNameContainingIgnoreCase(keyword, pageable);
            Page<BrandResponseDto> dtoPage = brandPage.map(BrandResponseDto::fromEntity);

            log.info("브랜드명으로 검색 성공 - 키워드: {}, 검색 결과 수: {}",
                    keyword, dtoPage.getTotalElements());
            return dtoPage;
        } catch (Exception e) {
            log.error("브랜드명으로 검색 중 예상치 못한 오류 발생 - 키워드: {}", keyword, e);
            throw new ProductException(ProductErrorCode.BRAND_NOT_FOUND, "브랜드 검색 중 오류가 발생했습니다.", e);
        }
    }
}