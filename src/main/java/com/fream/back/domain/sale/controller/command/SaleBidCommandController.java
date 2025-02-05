package com.fream.back.domain.sale.controller.command;

import com.fream.back.domain.sale.dto.InstantSaleRequestDto;
import com.fream.back.domain.sale.dto.SaleBidRequestDto;
import com.fream.back.domain.sale.service.command.SaleBidCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sale-bids")
@RequiredArgsConstructor
public class SaleBidCommandController {

    private final SaleBidCommandService saleBidCommandService;

    // 판매 입찰 생성
    @PostMapping
    public ResponseEntity<Long> createSaleBid(
            @RequestBody SaleBidRequestDto requestDto) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 이메일 추출
        Long saleBidId = saleBidCommandService.createSaleBid(
                email,
                requestDto.getProductSizeId(),
                requestDto.getBidPrice(),
                requestDto.getReturnAddress(),
                requestDto.getPostalCode(),
                requestDto.getReceiverPhone(),
                requestDto.isWarehouseStorage()
        ).getId();
        return ResponseEntity.ok(saleBidId);
    }

    // 판매 입찰 삭제
    @DeleteMapping("/{saleBidId}")
    public ResponseEntity<Void> deleteSaleBid(
            @PathVariable("saleBidId") Long saleBidId) {
        saleBidCommandService.deleteSaleBid(saleBidId);
        return ResponseEntity.ok().build();
    }
    // 즉시 판매 생성
    @PostMapping("/instant")
    public ResponseEntity<Long> createInstantSale(
            @RequestBody InstantSaleRequestDto requestDto) {
        String sellerEmail = SecurityUtils.extractEmailFromSecurityContext(); // 이메일 추출
        Long saleId = saleBidCommandService.createInstantSaleBid(
                requestDto.getOrderBidId(),
                sellerEmail,
                requestDto.getReturnAddress(),
                requestDto.getPostalCode(),
                requestDto.getReceiverPhone()
        );
        return ResponseEntity.ok(saleId);
    }
}

