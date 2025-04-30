package com.fream.back.domain.shipment.config;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.global.utils.CjTrackingPlaywright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * 배송 상태 배치 처리를 위한 ItemProcessor
 * 주문 배송 상태를 조회하고 처리합니다.
 */
@Slf4j
public class ShipmentItemProcessor implements ItemProcessor<OrderShipment, OrderShipment> {

    private final CjTrackingPlaywright cjTrackingPlaywright;
    private final NotificationCommandService notificationService;
    private final OrderShipmentCommandService orderService;

    /**
     * 배송 상태 처리기 생성자
     *
     * @param cjTrackingPlaywright CJ대한통운 배송 조회 유틸리티
     * @param notificationService 알림 서비스
     * @param orderService 주문 서비스
     */
    public ShipmentItemProcessor(
            CjTrackingPlaywright cjTrackingPlaywright,
            NotificationCommandService notificationService,
            OrderShipmentCommandService orderService
    ) {
        this.cjTrackingPlaywright = cjTrackingPlaywright;
        this.notificationService = notificationService;
        this.orderService = orderService;
    }

    /**
     * 주문 배송의 상태를 외부 서비스를 통해 조회하고 업데이트합니다.
     *
     * @param orderShipment 처리할 배송 정보
     * @return 업데이트된 배송 정보
     * @throws Exception 처리 중 오류 발생 시
     */
    @Override
    public OrderShipment process(OrderShipment orderShipment) throws Exception {
        String trackingNumber = orderShipment.getTrackingNumber();
        Long shipmentId = orderShipment.getId();

        if (trackingNumber == null || trackingNumber.isBlank()) {
            log.warn("유효하지 않은 운송장 번호: shipmentId={}", shipmentId);
            throw new ShipmentException(ShipmentErrorCode.TRACKING_NUMBER_INVALID);
        }

        log.info("배송 상태 처리 시작: shipmentId={}, trackingNumber={}, currentStatus={}",
                shipmentId, trackingNumber, orderShipment.getStatus());

        try {
            // 외부 서비스를 통해 현재 배송 상태 조회
            String statusText = cjTrackingPlaywright.getCurrentTrackingStatus(trackingNumber);
            ShipmentStatus newStatus = mapToShipmentStatus(statusText);

            log.debug("배송 상태 조회 결과: shipmentId={}, statusText={}, mappedStatus={}",
                    shipmentId, statusText, newStatus);

            // 현재 상태와 동일한 경우 중복 처리 방지
            if (orderShipment.getStatus() == newStatus) {
                log.debug("상태 변경 없음 (중복 처리 방지): shipmentId={}, status={}",
                        shipmentId, newStatus);
                return orderShipment;
            }

            // 상태에 따른 처리
            if (newStatus == ShipmentStatus.DELIVERED) {
                log.info("배송 완료 처리: shipmentId={}, orderId={}",
                        shipmentId, orderShipment.getOrder().getId());

                // 배송 상태 업데이트
                orderShipment.updateStatus(ShipmentStatus.DELIVERED);

                // 주문 완료 처리 및 알림
                orderService.completeOrder(orderShipment.getOrder().getId());
                notificationService.notifyShipmentCompleted(orderShipment.getOrder());

            } else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
                log.info("배송 출발 처리: shipmentId={}", shipmentId);
                orderShipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);

            } else {
                log.info("배송 중 상태 업데이트: shipmentId={}", shipmentId);
                orderShipment.updateStatus(ShipmentStatus.IN_TRANSIT);
            }

            log.info("배송 상태 처리 완료: shipmentId={}, newStatus={}",
                    shipmentId, orderShipment.getStatus());

            return orderShipment;

        } catch (Exception e) {
            log.error("배송 상태 처리 중 오류 발생: shipmentId={}, trackingNumber={}, error={}",
                    shipmentId, trackingNumber, e.getMessage(), e);

            // ShipmentException 타입이 아닌 경우 래핑
            if (!(e instanceof ShipmentException)) {
                throw new ShipmentException(
                        ShipmentErrorCode.EXTERNAL_TRACKING_SERVICE_ERROR,
                        "배송 상태 조회 중 오류 발생: " + e.getMessage(),
                        e
                );
            }
            throw e;
        }
    }

    /**
     * 배송사 상태 텍스트를 내부 ShipmentStatus로 매핑합니다.
     *
     * @param statusText 배송사 상태 텍스트
     * @return 매핑된 ShipmentStatus
     */
    private ShipmentStatus mapToShipmentStatus(String statusText) {
        return switch (statusText) {
            case "배송완료" -> ShipmentStatus.DELIVERED;
            case "배송출발" -> ShipmentStatus.OUT_FOR_DELIVERY;
            default -> ShipmentStatus.IN_TRANSIT;
        };
    }
}