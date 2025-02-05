package com.fream.back.domain.sale.service.query;

import com.fream.back.domain.sale.dto.SaleBidResponseDto;
import com.fream.back.domain.sale.dto.SaleBidStatusCountDto;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.sale.repository.SaleBidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class SaleBidQueryService {

    private final SaleBidRepository saleBidRepository;

    @Transactional(readOnly = true)
    public SaleBid findById(Long saleBidId) {
        return saleBidRepository.findById(saleBidId)
                .orElseThrow(() -> new IllegalArgumentException("해당 SaleBid를 찾을 수 없습니다: " + saleBidId));
    }
    @Transactional(readOnly = true)
    public SaleBid findByOrderId(Long orderId) {
        return saleBidRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Order에 연결된 SaleBid가 없습니다. Order ID: " + orderId));
    }
    @Transactional(readOnly = true)
    public SaleBid findBySaleId(Long saleId) {
        return saleBidRepository.findBySale_Id(saleId)
                .orElse(null); // 연결되지 않은 경우 null 반환
    }
    public Page<SaleBidResponseDto> getSaleBids(String email, String saleBidStatus, String saleStatus, Pageable pageable) {
        return saleBidRepository.findSaleBidsByFilters(email, saleBidStatus, saleStatus, pageable);
    }
    public SaleBidStatusCountDto getSaleBidStatusCounts(String email) {
        return saleBidRepository.countSaleBidsByStatus(email);
    }
    @Transactional(readOnly = true)
    public SaleBidResponseDto getSaleBidDetail(Long saleBidId, String email) {
        return saleBidRepository.findSaleBidById(saleBidId, email);
    }
}
