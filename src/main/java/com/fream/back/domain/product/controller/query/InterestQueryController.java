package com.fream.back.domain.product.controller.query;

import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.repository.SortOption;
import com.fream.back.domain.product.service.interest.InterestQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interests")
@RequiredArgsConstructor
public class InterestQueryController {

    private final InterestQueryService interestQueryService;

    @GetMapping("/{userId}")
    public ResponseEntity<Page<ProductSearchResponseDto>> getUserInterestProducts(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "field", required = false) String field,
            @RequestParam(name = "order", required = false) String order,
            Pageable pageable) {
        // SortOption 객체 생성
        SortOption sortOption = new SortOption(field, order);
        Page<ProductSearchResponseDto> response = interestQueryService.findUserInterestProducts(userId, sortOption, pageable);
        return ResponseEntity.ok(response);
    }
}
