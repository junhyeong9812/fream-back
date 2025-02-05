package com.fream.back.domain.sale.controller.command;

import com.fream.back.domain.sale.dto.ShipmentRequestDto;
import com.fream.back.domain.sale.service.command.SaleCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleCommandController {

    private final SaleCommandService saleCommandService;


    // 판매 상태 업데이트 (배송 상태)
    @PostMapping("/{saleId}/shipment")
    public ResponseEntity<Void> createSellerShipment(
            @PathVariable("saleId") Long saleId,
            @RequestBody ShipmentRequestDto requestDto
    ) {
        saleCommandService.createSellerShipment(
                saleId,
                requestDto.getCourier(),
                requestDto.getTrackingNumber()
        );
        return ResponseEntity.ok().build();
    }
}
