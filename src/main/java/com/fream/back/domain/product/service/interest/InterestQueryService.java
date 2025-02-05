package com.fream.back.domain.product.service.interest;

import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.repository.InterestQueryDslRepository;
import com.fream.back.domain.product.repository.SortOption;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestQueryService {

    private final InterestQueryDslRepository interestQueryDslRepository;

    public Page<ProductSearchResponseDto> findUserInterestProducts(
            Long userId,
            SortOption sortoption,
            Pageable pageable) {
        return interestQueryDslRepository.findUserInterestProducts(userId,sortoption, pageable);
    }
}