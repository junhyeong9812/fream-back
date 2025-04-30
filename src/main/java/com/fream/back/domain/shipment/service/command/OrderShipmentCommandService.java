package com.fream.back.domain.shipment.service.command;

import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderBid;
import com.fream.back.domain.order.entity.OrderStatus;
import com.fream.back.domain.order.service.query.OrderBidQueryService;
import com.fream.back.domain.sale.entity.BidStatus;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.sale.entity.SaleStatus;
import com.fream.back.domain.sale.service.query.SaleBidQueryService;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.repository.OrderShipmentRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import com.fream.back.global.utils.CjTrackingPlaywright;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 주문 배송 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderShipmentCommandService {

    private final OrderShipmentRepository orderShipmentRepository;
    private final NotificationCommandService notificationCommandService;
    private final SaleBidQueryService saleBidQueryService;
    private final OrderBidQueryService orderBidQueryService;
    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final CjTrackingPlaywright cjTrackingPlaywright;

    /**
     * 주문에 대한 배송 정보를 생성합니다.
     *
     * @param order 주문 엔티티
     * @param receiverName 수령인 이름
     * @param receiverPhone 수령인 전화번호
     * @param postalCode 우편번호
     * @param address 배송지 주소
     * @return 생성된 배송 정보
     */
    @Transactional
    public OrderShipment createOrderShipment(Order order, String receiverName, String receiverPhone,
                                             String postalCode, String address) {
        log.info("주문 배송 정보 생성 시작: orderId={}, receiverName={}",
                order.getId(), receiverName);

        OrderShipment shipment = OrderShipment.builder()
                .order(order)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .postalCode(postalCode)
                .address(address)
                .status(ShipmentStatus.PENDING) // 초기 상태 설정
                .build();

        OrderShipment savedShipment = orderShipmentRepository.save(shipment);
        log.info("주문 배송 정보 생성 완료: shipmentId={}, orderId={}",
                savedShipment.getId(), order.getId());

        return savedShipment;
    }

    /**
     * 배송 상태를 업데이트합니다.
     *
     * @param shipment 업데이트할 배송 엔티티
     * @param newStatus 새로운 배송 상태
     */
    @Transactional
    public void updateShipmentStatus(OrderShipment shipment, ShipmentStatus newStatus) {
        log.info("배송 상태 업데이트: shipmentId={}, currentStatus={}, newStatus={}",
                shipment.getId(), shipment.getStatus(), newStatus);

        try {
            shipment.updateStatus(newStatus);
            orderShipmentRepository.save(shipment);

            log.info("배송 상태 업데이트 완료: shipmentId={}, status={}",
                    shipment.getId(), newStatus);
        } catch (IllegalStateException e) {
            log.error("배송 상태 업데이트 실패: shipmentId={}, error={}",
                    shipment.getId(), e.getMessage(), e);

            throw new ShipmentException(
                    ShipmentErrorCode.INVALID_SHIPMENT_STATUS_TRANSITION,
                    "유효하지 않은 배송 상태 전환입니다: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 주문 완료에 따라 판매 상태를 'SOLD'로 변경합니다.
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void updateSaleStatusToSold(Long orderId) {
        log.info("판매 상태를 'SOLD'로 변경 시작: orderId={}", orderId);

        // SaleBid 조회
        SaleBid saleBid = saleBidQueryService.findByOrderId(orderId);
        if (saleBid != null) {
            Sale sale = saleBid.getSale();
            if (sale != null && sale.getStatus().canTransitionTo(SaleStatus.SOLD)) {
                log.info("Sale 상태 'SOLD'로 변경: saleId={}, sellerId={}",
                        sale.getId(), sale.getSeller().getId());

                sale.updateStatus(SaleStatus.SOLD); // 더티 체크에 의해 자동으로 업데이트됨

                // 창고 상태 업데이트
                if (sale.isWarehouseStorage()) {
                    log.info("창고 보관 상품의 창고 상태 업데이트: saleId={}", sale.getId());
                    warehouseStorageCommandService.updateWarehouseStatusToSold(sale);
                }

                // 판매자에게 알림 생성
                User seller = sale.getSeller();
                notificationCommandService.createNotification(
                        seller.getId(),
                        NotificationCategory.SHOPPING,
                        NotificationType.BID,
                        "상품이 성공적으로 판매 완료되었습니다. 판매 ID: " + sale.getId()
                );

                log.info("판매자 알림 생성 완료: sellerId={}, saleId={}",
                        seller.getId(), sale.getId());
            } else {
                log.debug("Sale 상태 변경 불가 또는 Sale이 null: saleBidId={}", saleBid.getId());
            }
        } else {
            log.debug("해당 주문과 연결된 SaleBid가 없음: orderId={}", orderId);
        }

        // OrderBid 조회
        try {
            OrderBid orderBid = orderBidQueryService.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 OrderBid를 찾을 수 없습니다: " + orderId));

            if (orderBid != null) {
                Sale sale = orderBid.getSale();
                if (sale != null && sale.getStatus().canTransitionTo(SaleStatus.SOLD)) {
                    log.info("OrderBid 기반 Sale 상태 'SOLD'로 변경: orderBidId={}, saleId={}",
                            orderBid.getId(), sale.getId());

                    sale.updateStatus(SaleStatus.SOLD); // 더티 체크에 의해 자동으로 업데이트됨

                    // 판매자에게 알림 생성
                    User seller = sale.getSeller();
                    notificationCommandService.createNotification(
                            seller.getId(),
                            NotificationCategory.SHOPPING,
                            NotificationType.BID,
                            "상품이 성공적으로 판매 완료되었습니다. 판매 ID: " + sale.getId()
                    );

                    log.info("판매자 알림 생성 완료: sellerId={}, saleId={}",
                            seller.getId(), sale.getId());
                } else {
                    log.debug("Sale 상태 변경 불가 또는 Sale이 null: orderBidId={}", orderBid.getId());
                }
            }
        } catch (IllegalArgumentException e) {
            log.warn("OrderBid 조회 실패: orderId={}, error={}", orderId, e.getMessage());
            // OrderBid가 없는 경우 예외로 중단하지 않고 진행
        }

        log.info("판매 상태 'SOLD' 변경 작업 완료: orderId={}", orderId);
    }

    /**
     * 주문을 완료 상태로 변경하고 관련된 후속 작업을 처리합니다.
     *
     * @param orderId 완료할 주문 ID
     */
    @Transactional
    public void completeOrder(Long orderId) {
        log.info("주문 완료 처리 시작: orderId={}", orderId);

        try {
            // OrderBid 조회
            OrderBid orderBid = orderBidQueryService.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("OrderBid가 존재하지 않습니다. orderId=" + orderId));

            Order order = orderBid.getOrder();
            log.debug("주문 정보 조회: orderId={}, userId={}, currentStatus={}",
                    order.getId(), order.getUser().getId(), order.getStatus());

            // 주문 상태를 COMPLETED로 변경
            order.updateStatus(OrderStatus.COMPLETED);
            log.info("주문 상태 COMPLETED로 변경: orderId={}", orderId);

            // 판매 상태 변경 로직
            updateSaleStatusToSold(orderId);

            // 주문 완료 알림
            User buyer = order.getUser();
            notificationCommandService.createNotification(
                    buyer.getId(),
                    NotificationCategory.SHOPPING,
                    NotificationType.BID,
                    "[completeOrder] 주문이 완료 처리되었습니다. 주문 ID: " + order.getId()
            );

            log.info("구매자 알림 생성 완료: userId={}, orderId={}", buyer.getId(), order.getId());

        } catch (Exception e) {
            log.error("주문 완료 처리 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.ORDER_NOT_FOUND_FOR_SHIPMENT,
                    "주문 완료 처리 중 오류 발생: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 배송 정보에 운송장 정보를 업데이트하고 관련 상태를 변경합니다.
     *
     * @param shipmentId 배송 ID
     * @param courier 택배사 이름
     * @param trackingNumber 운송장 번호
     */
    @Transactional
    public void updateTrackingInfo(Long shipmentId, String courier, String trackingNumber) {
        log.info("배송 운송장 정보 업데이트: shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, courier, trackingNumber);

        if (courier == null || courier.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            log.warn("유효하지 않은 배송사 또는 운송장 번호: shipmentId={}", shipmentId);
            throw new ShipmentException(ShipmentErrorCode.TRACKING_INFO_REQUIRED);
        }

        try {
            OrderShipment shipment = orderShipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ShipmentException(
                            ShipmentErrorCode.SHIPMENT_NOT_FOUND,
                            "Shipment 정보를 찾을 수 없습니다: " + shipmentId
                    ));

            // 운송장 정보 업데이트
            shipment.updateTrackingInfo(courier, trackingNumber);

            // 배송 상태가 변경되었으므로 주문 상태도 업데이트
            Order order = shipment.getOrder();
            order.updateStatus(OrderStatus.IN_TRANSIT);

            log.info("주문 상태 IN_TRANSIT으로 업데이트: orderId={}", order.getId());

            // 주문한 사용자에게 알림 생성
            User buyer = order.getUser();
            notificationCommandService.createNotification(
                    buyer.getId(),
                    NotificationCategory.SHOPPING,
                    NotificationType.BID,
                    "상품이 출발하였습니다. 주문 ID: " + order.getId()
            );

            log.info("구매자 출발 알림 생성: userId={}, orderId={}", buyer.getId(), order.getId());

            // 판매 상태 업데이트 및 알림 전송
            updateSaleStatusToSold(order.getId());

            // SaleBid에서 Sale 조회 및 창고 상태 업데이트
            SaleBid saleBid = saleBidQueryService.findByOrderId(order.getId());
            if (saleBid != null) {
                Sale sale = saleBid.getSale();
                if (sale != null && sale.isWarehouseStorage()) {
                    log.info("창고 보관 상품의 창고 상태 업데이트: saleId={}", sale.getId());
                    warehouseStorageCommandService.updateWarehouseStatusToSold(sale);
                }
            }

            // OrderBid 상태를 COMPLETED로 변경
            updateOrderAndSaleBidStatusToCompleted(order.getId());

            log.info("운송장 정보 업데이트 완료: shipmentId={}", shipmentId);

        } catch (ShipmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("운송장 정보 업데이트 실패: shipmentId={}, error={}", shipmentId, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.SHIPMENT_NOT_FOUND,
                    "운송장 정보 업데이트 중 오류 발생: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 전체 배송 상태를 주기적으로 업데이트합니다.
     */
    @Transactional
    public void updateShipmentStatuses() {
        log.info("전체 배송 상태 업데이트 시작");

        // 1. IN_TRANSIT 또는 OUT_FOR_DELIVERY 상태의 OrderShipment 조회
        List<OrderShipment> shipments = orderShipmentRepository.findByStatusIn(
                List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY)
        );

        log.debug("업데이트 대상 배송 조회: 총 {}건", shipments.size());

        int successCount = 0;
        int errorCount = 0;

        for (OrderShipment shipment : shipments) {
            try {
                String trackingNumber = shipment.getTrackingNumber();
                if (trackingNumber == null || trackingNumber.isBlank()) {
                    log.warn("운송장 번호가 없는 배송건 스킵: shipmentId={}", shipment.getId());
                    errorCount++;
                    continue;
                }

                log.debug("배송 상태 조회 시작: shipmentId={}, trackingNumber={}",
                        shipment.getId(), trackingNumber);

                // 2. CJ대한통운에서 현재 배송 상태 추출
                String currentStatus = cjTrackingPlaywright.getCurrentTrackingStatus(trackingNumber);
                ShipmentStatus newStatus = mapToShipmentStatus(currentStatus);

                log.debug("배송 상태 조회 결과: shipmentId={}, currentStatus={}, newStatus={}",
                        shipment.getId(), currentStatus, newStatus);

                // 현재 상태와 동일한 경우 중복 처리 방지
                if (shipment.getStatus() == newStatus) {
                    log.debug("상태 변경 없음 (중복 처리 방지): shipmentId={}, status={}",
                            shipment.getId(), newStatus);
                    successCount++;
                    continue;
                }

                // 3. 배송 상태가 DELIVERED인 경우 Order와 Shipment 상태 업데이트
                if (newStatus == ShipmentStatus.DELIVERED) {
                    log.info("배송 완료 처리: shipmentId={}", shipment.getId());

                    shipment.updateStatus(newStatus);
                    Order order = shipment.getOrder();
                    order.updateStatus(OrderStatus.COMPLETED);

                    // 배송 완료 알림 전송
                    User buyer = order.getUser();
                    notificationCommandService.createNotification(
                            buyer.getId(),
                            NotificationCategory.SHOPPING,
                            NotificationType.BID,
                            "상품이 배송 완료되었습니다. 주문 ID: " + order.getId()
                    );

                    log.info("배송 완료 알림 전송: userId={}, orderId={}",
                            buyer.getId(), order.getId());

                } else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY &&
                        shipment.getStatus() != ShipmentStatus.OUT_FOR_DELIVERY) {
                    log.info("배송 출발 처리: shipmentId={}", shipment.getId());
                    shipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);

                } else {
                    log.debug("기타 상태 업데이트: shipmentId={}, newStatus={}",
                            shipment.getId(), newStatus);
                    shipment.updateStatus(newStatus);
                }

                orderShipmentRepository.save(shipment);
                successCount++;

            } catch (Exception e) {
                log.error("배송 상태 업데이트 실패: shipmentId={}, error={}",
                        shipment.getId(), e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("전체 배송 상태 업데이트 완료: 성공={}건, 실패={}건", successCount, errorCount);
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

    /**
     * OrderBid와 SaleBid의 상태를 COMPLETED로 업데이트합니다.
     *
     * @param orderId 주문 ID
     */
    @Transactional
    private void updateOrderAndSaleBidStatusToCompleted(Long orderId) {
        log.info("입찰 상태 COMPLETED로 변경: orderId={}", orderId);

        try {
            // OrderBid 조회
            OrderBid orderBid = orderBidQueryService.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 OrderBid를 찾을 수 없습니다: " + orderId));

            // 상태를 COMPLETED로 업데이트
            if (orderBid.getStatus() == com.fream.back.domain.order.entity.BidStatus.MATCHED) {
                log.debug("OrderBid 상태 COMPLETED로 변경: orderBidId={}", orderBid.getId());
                orderBid.updateStatus(com.fream.back.domain.order.entity.BidStatus.COMPLETED);
            }

            // SaleBid 상태 업데이트
            SaleBid saleBid = saleBidQueryService.findByOrderId(orderId);
            if (saleBid != null && saleBid.getStatus() == BidStatus.MATCHED) {
                log.debug("SaleBid 상태 COMPLETED로 변경: saleBidId={}", saleBid.getId());
                saleBid.updateStatus(BidStatus.COMPLETED);
            }

            log.info("입찰 상태 COMPLETED 변경 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("입찰 상태 변경 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            // 상태 변경 실패는 로그만 남기고 진행 (주요 로직에 영향 주지 않음)
        }
    }

    /**
     * 단일 송장에 대해 즉시 상태를 갱신하고, 최종 상태를 반환합니다.
     *
     * @param shipmentId 배송 ID
     * @param courier 택배사
     * @param trackingNumber 운송장 번호
     * @return 갱신된 배송 상태
     * @throws ShipmentException 배송 상태 조회 실패 시 발생
     */
    @Transactional
    public ShipmentStatus updateAndCheckShipmentStatus(Long shipmentId, String courier, String trackingNumber) throws Exception {
        log.info("단일 송장 상태 즉시 조회 및 업데이트: shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, courier, trackingNumber);

        if (courier == null || courier.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            log.warn("유효하지 않은 배송사 또는 운송장 번호: shipmentId={}", shipmentId);
            throw new ShipmentException(ShipmentErrorCode.TRACKING_INFO_REQUIRED);
        }

        try {
            // 1) 기존 Shipment 로드
            OrderShipment shipment = orderShipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ShipmentException(
                            ShipmentErrorCode.SHIPMENT_NOT_FOUND,
                            "Shipment 정보를 찾을 수 없습니다: " + shipmentId
                    ));

            log.debug("배송 정보 조회 완료: shipmentId={}, 현재상태={}",
                    shipment.getId(), shipment.getStatus());

            // 2) 송장 정보 업데이트 (택배사, 송장번호)
            shipment.updateTrackingInfo(courier, trackingNumber);
            log.debug("송장 정보 업데이트 완료: courier={}, trackingNumber={}", courier, trackingNumber);

            // 3) CJ대한통운 조회로 현재 상태 확인
            String currentStatus = cjTrackingPlaywright.getCurrentTrackingStatus(trackingNumber);
            ShipmentStatus newStatus = mapToShipmentStatus(currentStatus);

            log.info("배송 상태 조회 결과: currentStatus={}, mappedStatus={}", currentStatus, newStatus);

            // 4) 상태에 따른 처리
            if (newStatus == ShipmentStatus.DELIVERED) {
                log.info("배송 완료 처리: shipmentId={}", shipmentId);

                shipment.updateStatus(ShipmentStatus.DELIVERED);

                // 주문 완료 로직
                completeOrder(shipment.getOrder().getId());

            } else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
                log.info("배송 출발 처리: shipmentId={}", shipmentId);
                shipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);

            } else {
                log.info("배송 중 상태 업데이트: shipmentId={}", shipmentId);
                shipment.updateStatus(ShipmentStatus.IN_TRANSIT);
            }

            // 5) 상태 반환
            log.info("송장 상태 확인 완료: shipmentId={}, finalStatus={}",
                    shipmentId, shipment.getStatus());
            return shipment.getStatus();

        } catch (ShipmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("송장 상태 확인 중 오류 발생: shipmentId={}, error={}",
                    shipmentId, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.EXTERNAL_TRACKING_SERVICE_ERROR,
                    "배송 상태 조회 중 오류 발생: " + e.getMessage(),
                    e
            );
        }
    }
}