package com.fream.back.domain.shipment.controller.command;

import com.fream.back.domain.shipment.dto.OrderShipmentRequestDto;
import com.fream.back.domain.shipment.dto.ShipmentStatusResponse;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 주문 배송 정보 명령 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/shipments/order")
@RequiredArgsConstructor
public class OrderShipmentCommandController {

    private final OrderShipmentCommandService orderShipmentCommandService;

    /**
     * 배송 운송장 정보를 업데이트합니다.
     *
     * @param shipmentId 배송 ID
     * @param requestDto 운송장 정보 DTO
     * @return 성공 응답
     */
    @PatchMapping("/{shipmentId}/status")
    public ResponseEntity<Void> updateTrackingInfo(
            @PathVariable Long shipmentId,
            @Validated @RequestBody OrderShipmentRequestDto requestDto
    ) {
        log.info("배송 운송장 정보 업데이트 요청: shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, requestDto.getCourier(), requestDto.getTrackingNumber());

        orderShipmentCommandService.updateTrackingInfo(
                shipmentId,
                requestDto.getCourier(),
                requestDto.getTrackingNumber()
        );

        log.info("배송 운송장 정보 업데이트 완료: shipmentId={}", shipmentId);
        return ResponseEntity.ok().build();
    }

    /**
     * 단일 배송 건에 대해 상태를 즉시 조회하고 업데이트합니다.
     *
     * @param shipmentId 배송 ID
     * @param requestDto 운송장 정보 DTO
     * @return 배송 상태 응답
     */
    @PostMapping("/{shipmentId}/check-status")
    public ResponseEntity<ShipmentStatusResponse> updateAndCheckStatus(
            @PathVariable("shipmentId") Long shipmentId,
            @Validated @RequestBody OrderShipmentRequestDto requestDto
    ) {
        log.info("배송 상태 즉시 조회 요청: shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, requestDto.getCourier(), requestDto.getTrackingNumber());

        try {
            // 서비스에서 상태 업데이트 & 조회
            ShipmentStatus updatedStatus = orderShipmentCommandService.updateAndCheckShipmentStatus(
                    shipmentId,
                    requestDto.getCourier(),
                    requestDto.getTrackingNumber()
            );

            // 응답 DTO로 감싸서 반환
            ShipmentStatusResponse response = new ShipmentStatusResponse(updatedStatus);
            log.info("배송 상태 조회 완료: shipmentId={}, status={}", shipmentId, updatedStatus);

            return ResponseEntity.ok(response);

        } catch (ShipmentException e) {
            log.error("배송 상태 조회 실패 (ShipmentException): shipmentId={}, code={}, message={}",
                    shipmentId, e.getErrorCode().getCode(), e.getMessage());

            return ResponseEntity
                    .status(e.getErrorCode().getStatus())
                    .build();

        } catch (Exception e) {
            log.error("배송 상태 조회 실패 (일반 예외): shipmentId={}, error={}",
                    shipmentId, e.getMessage(), e);

            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 단일 배송 건에 대해 상태를 즉시 조회하고 문자열로 반환합니다.
     *
     * @param shipmentId 배송 ID
     * @param requestDto 운송장 정보 DTO
     * @return 배송 상태 문자열
     */
    @PostMapping("/{shipmentId}/check-status-string")
    public ResponseEntity<String> updateAndCheckStatusAsString(
            @PathVariable("shipmentId") Long shipmentId,
            @Validated @RequestBody OrderShipmentRequestDto requestDto
    ) {
        log.info("배송 상태 즉시 조회 요청 (문자열 응답): shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, requestDto.getCourier(), requestDto.getTrackingNumber());

        try {
            ShipmentStatus updatedStatus = orderShipmentCommandService.updateAndCheckShipmentStatus(
                    shipmentId,
                    requestDto.getCourier(),
                    requestDto.getTrackingNumber()
            );

            log.info("배송 상태 조회 완료 (문자열 응답): shipmentId={}, status={}",
                    shipmentId, updatedStatus);

            // 문자열로 반환
            return ResponseEntity.ok(updatedStatus.name());

        } catch (ShipmentException e) {
            log.error("배송 상태 문자열 조회 실패 (ShipmentException): shipmentId={}, code={}, message={}",
                    shipmentId, e.getErrorCode().getCode(), e.getMessage());

            return ResponseEntity
                    .status(e.getErrorCode().getStatus())
                    .body("Error: " + e.getMessage());

        } catch (Exception e) {
            log.error("배송 상태 문자열 조회 실패 (일반 예외): shipmentId={}, error={}",
                    shipmentId, e.getMessage(), e);

            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}