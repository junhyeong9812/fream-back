package com.fream.back.domain.product.service.brand;

import com.fream.back.domain.product.dto.BrandRequestDto;
import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.BrandRepository;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 브랜드 명령(Command) 서비스
 * 브랜드의 생성, 수정, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BrandCommandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    /**
     * 브랜드 생성
     *
     * @param request 브랜드 생성 요청 DTO
     * @return 생성된 브랜드 응답 DTO
     * @throws ProductException 브랜드 생성 실패 시
     */
    public BrandResponseDto createBrand(BrandRequestDto request) {
        log.info("브랜드 생성 요청 - 브랜드명: {}", request.getName());

        try {
            // 브랜드명 중복 확인
            if (brandRepository.existsByName(request.getName())) {
                log.warn("브랜드 생성 실패 - 이미 존재하는 브랜드명: {}", request.getName());
                throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS,
                        "이미 존재하는 브랜드명입니다: " + request.getName());
            }

            Brand brand = Brand.builder()
                    .name(request.getName())
                    .build();

            Brand savedBrand = brandRepository.save(brand);

            log.info("브랜드 생성 성공 - 브랜드ID: {}, 브랜드명: {}", savedBrand.getId(), savedBrand.getName());
            return BrandResponseDto.fromEntity(savedBrand);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("브랜드 생성 중 예상치 못한 오류 발생 - 브랜드명: {}", request.getName(), e);
            throw new ProductException(ProductErrorCode.BRAND_CREATION_FAILED, "브랜드 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드 수정
     *
     * @param id 브랜드 ID
     * @param request 브랜드 수정 요청 DTO
     * @return 수정된 브랜드 응답 DTO
     * @throws ProductException 브랜드 수정 실패 시
     */
    public BrandResponseDto updateBrand(Long id, BrandRequestDto request) {
        log.info("브랜드 수정 요청 - 브랜드ID: {}, 새 브랜드명: {}", id, request.getName());

        try {
            // 기존 브랜드 조회
            Brand brand = brandRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("브랜드 수정 실패 - 존재하지 않는 브랜드ID: {}", id);
                        return new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                                "존재하지 않는 브랜드입니다. ID: " + id);
                    });

            // 다른 브랜드와 이름 중복 확인
            if (!brand.getName().equals(request.getName()) &&
                    brandRepository.existsByName(request.getName())) {
                log.warn("브랜드 수정 실패 - 이미 존재하는 브랜드명: {}", request.getName());
                throw new ProductException(ProductErrorCode.BRAND_ALREADY_EXISTS,
                        "이미 존재하는 브랜드명입니다: " + request.getName());
            }

            // 필요한 필드만 업데이트 (더티체크 적용)
            brand.updateName(request.getName());

            log.info("브랜드 수정 성공 - 브랜드ID: {}, 새 브랜드명: {}", id, brand.getName());
            return BrandResponseDto.fromEntity(brand);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("브랜드 수정 중 예상치 못한 오류 발생 - 브랜드ID: {}", id, e);
            throw new ProductException(ProductErrorCode.BRAND_UPDATE_FAILED, "브랜드 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 브랜드 삭제
     *
     * @param name 브랜드명
     * @throws ProductException 브랜드 삭제 실패 시
     */
    public void deleteBrand(String name) {
        log.info("브랜드 삭제 요청 - 브랜드명: {}", name);

        try {
            // 1. 브랜드 존재 여부 확인
            Brand brand = brandRepository.findByName(name)
                    .orElseThrow(() -> {
                        log.warn("브랜드 삭제 실패 - 존재하지 않는 브랜드명: {}", name);
                        return new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                                "존재하지 않는 브랜드입니다: " + name);
                    });

            // 2. 연관된 상품(Product) 확인
            boolean hasAssociatedProducts = productRepository.existsByBrand(brand);
            if (hasAssociatedProducts) {
                log.warn("브랜드 삭제 실패 - 연관된 상품이 존재하는 브랜드: {}", name);
                throw new ProductException(ProductErrorCode.BRAND_IN_USE,
                        "해당 브랜드와 연관된 상품이 존재합니다. 연관된 상품 삭제 후 브랜드를 삭제해주세요.");
            }

            // 3. 브랜드 삭제
            brandRepository.delete(brand);
            log.info("브랜드 삭제 성공 - 브랜드ID: {}, 브랜드명: {}", brand.getId(), name);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("브랜드 삭제 중 예상치 못한 오류 발생 - 브랜드명: {}", name, e);
            throw new ProductException(ProductErrorCode.BRAND_DELETION_FAILED, "브랜드 삭제 중 오류가 발생했습니다.", e);
        }
    }
}