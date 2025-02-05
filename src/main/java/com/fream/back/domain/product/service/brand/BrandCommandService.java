package com.fream.back.domain.product.service.brand;

import com.fream.back.domain.product.dto.BrandRequestDto;
import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.repository.BrandRepository;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class BrandCommandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandResponseDto createBrand(BrandRequestDto request) {
        Brand brand = Brand.builder()
                .name(request.getName())
                .build();
        brandRepository.save(brand);
        return BrandResponseDto.fromEntity(brand);
    }

    public BrandResponseDto updateBrand(Long id, BrandRequestDto request) {
        // 기존 브랜드 조회
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));
        // 필요한 필드만 업데이트 (더티체크 적용)
        brand.updateName(request.getName());
        return BrandResponseDto.fromEntity(brand);
    }

    public void deleteBrand(String name) {
        // 1. 브랜드 존재 여부 확인
        Brand brand = brandRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 브랜드입니다."));

        // 2. 연관된 상품(Product) 확인
        boolean hasAssociatedProducts = productRepository.existsByBrand(brand);
        if (hasAssociatedProducts) {
            throw new IllegalStateException("해당 브랜드와 연관된 상품이 존재합니다. 연관된 상품 삭제 후 브랜드를 삭제해주세요.");
        }

        // 3. 브랜드 삭제
        brandRepository.delete(brand);
    }
}
