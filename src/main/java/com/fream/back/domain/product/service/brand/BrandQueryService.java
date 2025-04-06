package com.fream.back.domain.product.service.brand;

import com.fream.back.domain.product.dto.BrandResponseDto;
import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.repository.BrandRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BrandQueryService {

    private final BrandRepository brandRepository;

    public BrandQueryService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BrandResponseDto findByName(String name) {
        Brand brand = brandRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("해당 브랜드가 존재하지 않습니다."));
        return BrandResponseDto.fromEntity(brand); // 엔티티 -> DTO 변환
    }

    public List<BrandResponseDto> findAllBrands() {
        return brandRepository.findAllByOrderByNameDesc()
                .stream()
                .map(BrandResponseDto::fromEntity) // 엔티티 -> DTO 변환
                .collect(Collectors.toList());
    }
    public Brand findById(Long brandId){
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new IllegalArgumentException("해당 브랜드가 존재하지 않습니다."));
    }

    // 브랜드 ID로 DTO 조회
    public BrandResponseDto findBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 브랜드가 존재하지 않습니다."));
        return BrandResponseDto.fromEntity(brand);
    }

    // 페이징으로 브랜드 조회
    public Page<BrandResponseDto> findBrandsPaging(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(BrandResponseDto::fromEntity);
    }

    // 브랜드명으로 검색 (페이징)
    public Page<BrandResponseDto> searchBrandsByName(String keyword, Pageable pageable) {
        return brandRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(BrandResponseDto::fromEntity);
    }

}
