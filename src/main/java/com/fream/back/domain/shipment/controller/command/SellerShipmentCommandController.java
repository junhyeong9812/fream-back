package com.fream.back.domain.shipment.controller.command;

import com.fream.back.domain.shipment.dto.SellerShipmentRequestDto;
import com.fream.back.domain.shipment.entity.SellerShipment;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.service.command.SellerShipmentCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 판매자 배송 정보 명령 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/shipments/seller")
@RequiredArgsConstructor
public class SellerShipmentCommandController {

    private final SellerShipmentCommandService sellerShipmentCommandService;

    /**
     * 판매자 배송 정보를 생성합니다.
     *
     * @param requestDto 판매자 배송 정보 DTO
     * @return 생성된 배송 ID
     */
    @PostMapping
    public ResponseEntity<Long> createSellerShipment(
            @Validated @RequestBody SellerShipmentRequestDto requestDto
    ) {
        log.info("판매자 배송 정보 생성 요청: saleId={}, courier={}, trackingNumber={}",
                requestDto.getSaleId(), requestDto.getCourier(), requestDto.getTrackingNumber());

        try {
            SellerShipment shipment = sellerShipmentCommandService.createSellerShipment(
                    requestDto.getSaleId(),
                    requestDto.getCourier(),
                    requestDto.getTrackingNumber()
            );

            Long shipmentId = shipment.getId();
            log.info("판매자 배송 정보 생성 완료: shipmentId={}, saleId={}",
                    shipmentId, requestDto.getSaleId());

            return ResponseEntity.ok(shipmentId);

        } catch (ShipmentException e) {
            log.error("판매자 배송 정보 생성 실패 (ShipmentException): saleId={}, code={}, message={}",
                    requestDto.getSaleId(), e.getErrorCode().getCode(), e.getMessage());

            return ResponseEntity
                    .status(e.getErrorCode().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("판매자 배송 정보 생성 실패 (일반 예외): saleId={}, error={}",
                    requestDto.getSaleId(), e.getMessage(), e);

            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 판매자 배송 정보를 업데이트합니다.
     *
     * @param shipmentId 배송 ID
     * @param requestDto 판매자 배송 정보 DTO
     * @return 성공 응답
     */
    @PatchMapping("/{shipmentId}")
    public ResponseEntity<Void> updateShipment(
            @PathVariable Long shipmentId,
            @Validated @RequestBody SellerShipmentRequestDto requestDto
    ) {
        log.info("판매자 배송 정보 업데이트 요청: shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, requestDto.getCourier(), requestDto.getTrackingNumber());

        try {
            sellerShipmentCommandService.updateShipment(
                    shipmentId,
                    requestDto.getCourier(),
                    requestDto.getTrackingNumber()
            );

            log.info("판매자 배송 정보 업데이트 완료: shipmentId={}", shipmentId);
            return ResponseEntity.ok().build();

        } catch (ShipmentException e) {
            log.error("판매자 배송 정보 업데이트 실패 (ShipmentException): shipmentId={}, code={}, message={}",
                    shipmentId, e.getErrorCode().getCode(), e.getMessage());

            return ResponseEntity
                    .status(e.getErrorCode().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("판매자 배송 정보 업데이트 실패 (일반 예외): shipmentId={}, error={}",
                    shipmentId, e.getMessage(), e);

            return ResponseEntity.badRequest().build();
        }
    }
}