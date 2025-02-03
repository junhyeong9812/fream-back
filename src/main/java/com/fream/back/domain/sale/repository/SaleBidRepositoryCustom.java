package com.fream.back.domain.sale.repository;

import com.fream.back.domain.sale.dto.SaleBidResponseDto;
import com.fream.back.domain.sale.dto.SaleBidStatusCountDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SaleBidRepositoryCustom {
    Page<SaleBidResponseDto> findSaleBidsByFilters(String email, String saleBidStatus, String saleStatus, Pageable pageable);
    SaleBidStatusCountDto countSaleBidsByStatus(String email);
    SaleBidResponseDto findSaleBidById(Long saleBidId, String email);
}

