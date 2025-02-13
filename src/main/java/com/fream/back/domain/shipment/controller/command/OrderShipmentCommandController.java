package com.fream.back.domain.shipment.controller.command;

import com.fream.back.domain.shipment.dto.OrderShipmentRequestDto;
import com.fream.back.domain.shipment.dto.ShipmentStatusResponse;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shipments/order")
@RequiredArgsConstructor
public class OrderShipmentCommandController {

    private final OrderShipmentCommandService orderShipmentCommandService;

    // 배송 상태 업데이트 엔드포인트
    @PatchMapping("/{shipmentId}/status")
    public ResponseEntity<Void> updateTrackingInfo(
            @PathVariable Long shipmentId,
            @RequestBody OrderShipmentRequestDto requestDto
    ) {
        orderShipmentCommandService.updateTrackingInfo(
                shipmentId,
                requestDto.getCourier(),
                requestDto.getTrackingNumber()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * 단건 조회 & 상태 즉시 업데이트 후 결과 반환
     */
    @PostMapping("/{shipmentId}/check-status")
    public ResponseEntity<ShipmentStatusResponse> updateAndCheckStatus(
            @PathVariable("shipmentId") Long shipmentId,
            @RequestBody OrderShipmentRequestDto requestDto
    ) {
        System.out.println("requestDto = " + requestDto.getCourier());
        System.out.println("requestDto = " + requestDto.getTrackingNumber());
        try {
            // 1) Service에서 상태 업데이트 & 조회
            ShipmentStatus updatedStatus = orderShipmentCommandService.updateAndCheckShipmentStatus(
                    shipmentId,
                    requestDto.getCourier(),
                    requestDto.getTrackingNumber()
            );

            // 2) 응답 DTO로 감싸서 반환
            ShipmentStatusResponse response = new ShipmentStatusResponse(updatedStatus);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // 전체 스택트레이스 찍기
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/{shipmentId}/check-status-string")
    public ResponseEntity<String> updateAndCheckStatusAsString(
            @PathVariable("shipmentId") Long shipmentId,
            @RequestBody OrderShipmentRequestDto requestDto
    ) {
        try {
            ShipmentStatus updatedStatus
                    = orderShipmentCommandService.updateAndCheckShipmentStatus(
                    shipmentId,
                    requestDto.getCourier(),
                    requestDto.getTrackingNumber()
            );

            // 문자열로 반환
            return ResponseEntity.ok(updatedStatus.name());

        } catch (Exception e) {
            e.printStackTrace(); // 어떤 예외인지 확인
            // 예외 메시지까지 Body에 담아줄 수 있음
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

}
