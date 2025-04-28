package com.fream.back.domain.product.service.brand;

import com.fream.back.domain.product.entity.Brand;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 브랜드 엔티티 서비스
 * 브랜드 엔티티를 조회하는 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BrandEntityService {

    private final BrandRepository brandRepository;

    /**
     * 브랜드명으로 브랜드 엔티티 조회
     *
     * @param name 브랜드명
     * @return 브랜드 엔티티
     * @throws ProductException 브랜드가 존재하지 않을 경우
     */
    public Brand findByName(String name) {
        log.debug("브랜드명으로 브랜드 엔티티 조회 - 브랜드명: {}", name);

        return brandRepository.findByName(name)
                .orElseThrow(() -> {
                    log.warn("브랜드 엔티티 조회 실패 - 존재하지 않는 브랜드명: {}", name);
                    return new ProductException(ProductErrorCode.BRAND_NOT_FOUND,
                            "브랜드가 존재하지 않습니다: " + name);
                });
    }
}