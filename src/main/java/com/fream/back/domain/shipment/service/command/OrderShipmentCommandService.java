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
import com.fream.back.domain.shipment.repository.OrderShipmentRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import com.fream.back.global.utils.CjTrackingPlaywright;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderShipmentCommandService {

    private final OrderShipmentRepository orderShipmentRepository;
    private final NotificationCommandService notificationCommandService;
    private final SaleBidQueryService saleBidQueryService;
    private final OrderBidQueryService orderBidQueryService;
    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final CjTrackingPlaywright cjTrackingPlaywright;
    @Transactional
    public OrderShipment createOrderShipment(Order order, String receiverName, String receiverPhone,
                                             String postalCode, String address) {
        OrderShipment shipment = OrderShipment.builder()
                .order(order)
                .receiverName(receiverName)
                .receiverPhone(receiverPhone)
                .postalCode(postalCode)
                .address(address)
                .status(ShipmentStatus.PENDING) // 초기 상태 설정
                .build();

        return orderShipmentRepository.save(shipment);
    }
    @Transactional
    public void updateShipmentStatus(OrderShipment shipment, ShipmentStatus newStatus) {
        shipment.updateStatus(newStatus);
        orderShipmentRepository.save(shipment);
    }

    @Transactional
    public void updateSaleStatusToSold(Long orderId) {
        // SaleBid 조회
        SaleBid saleBid = saleBidQueryService.findByOrderId(orderId);
        if (saleBid != null) {
            Sale sale = saleBid.getSale();
            if (sale != null && sale.getStatus().canTransitionTo(SaleStatus.SOLD)) {
                sale.updateStatus(SaleStatus.SOLD); // 더티 체크에 의해 자동으로 업데이트됨

                // 창고 상태 업데이트
                if (sale.isWarehouseStorage()) {
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
            }
        }

        // OrderBid 조회
        OrderBid orderBid = orderBidQueryService.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 OrderBid를 찾을 수 없습니다: " + orderId));
        if (orderBid != null) {
            Sale sale = orderBid.getSale();
            if (sale != null && sale.getStatus().canTransitionTo(SaleStatus.SOLD)) {
                sale.updateStatus(SaleStatus.SOLD); // 더티 체크에 의해 자동으로 업데이트됨
                // 판매자에게 알림 생성
                User seller = sale.getSeller();
                notificationCommandService.createNotification(
                        seller.getId(),
                        NotificationCategory.SHOPPING,
                        NotificationType.BID,
                        "상품이 성공적으로 판매 완료되었습니다. 판매 ID: " + sale.getId()
                );
            }
        }
    }
    @Transactional
    public void completeOrder(Long orderId) {
        // 1) OrderBid or Order 엔티티 조회 (프로젝트 구조에 따라 다름)
        //    예: "주문" 테이블이 있다면...
        OrderBid orderBid = orderBidQueryService.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("OrderBid가 존재하지 않습니다. orderId=" + orderId));
        Order order = orderBid.getOrder();

        // 2) 주문 상태를 COMPLETED로 변경
        order.updateStatus(OrderStatus.COMPLETED);

        // 3) 필요하다면 판매 상태 변경 로직 (ex: sale.setStatus(SOLD)) 이나 창고 갱신 호출
        updateSaleStatusToSold(orderId);

        // 4) 알림 또는 기타 후속 로직
        //    예: "주문이 완료되었습니다." 알림
        User buyer = order.getUser();
        notificationCommandService.createNotification(
                buyer.getId(),
                NotificationCategory.SHOPPING,
                NotificationType.BID,
                "[completeOrder] 주문이 완료 처리되었습니다. 주문 ID: " + order.getId()
        );
    }

    @Transactional
    public void updateTrackingInfo(Long shipmentId, String courier, String trackingNumber) {
        OrderShipment shipment = orderShipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment 정보를 찾을 수 없습니다: " + shipmentId));

        shipment.updateTrackingInfo(courier, trackingNumber);
        shipment.updateStatus(ShipmentStatus.IN_TRANSIT);

        // 배송 상태가 변경되었으므로 주문 상태도 업데이트
        Order order = shipment.getOrder();
        order.updateStatus(OrderStatus.IN_TRANSIT);

        // 주문한 사용자에게 알림 생성
        User buyer = order.getUser();
        notificationCommandService.createNotification(
                buyer.getId(),
                NotificationCategory.SHOPPING,
                NotificationType.BID,
                "상품이 출발하였습니다. 주문 ID: " + order.getId()
        );
        // 판매 상태 업데이트 및 알림 전송
        updateSaleStatusToSold(order.getId());

        // SaleBid에서 Sale 조회 및 창고 상태 업데이트
        SaleBid saleBid = saleBidQueryService.findByOrderId(order.getId());
        if (saleBid != null) {
            Sale sale = saleBid.getSale();
            if (sale != null && sale.isWarehouseStorage()) {
                warehouseStorageCommandService.updateWarehouseStatusToSold(sale);
            }
        }


        // OrderBid 상태를 COMPLETED로 변경
        updateOrderAndSaleBidStatusToCompleted(order.getId());

    }

    //Cj대한통운 송장조회
    public void updateShipmentStatuses() {
        // 1. IN_TRANSIT 또는 OUT_FOR_DELIVERY 상태의 OrderShipment 조회
        List<OrderShipment> shipments = orderShipmentRepository.findByStatusIn(
                List.of(ShipmentStatus.IN_TRANSIT, ShipmentStatus.OUT_FOR_DELIVERY)
        );

        for (OrderShipment shipment : shipments) {
            try {
                // 2. CJ대한통운에서 현재 배송 상태 추출
                String currentStatus = getCurrentTrackingStatus(shipment.getTrackingNumber());
                ShipmentStatus newStatus = mapToShipmentStatus(currentStatus);

                // 3. 배송 상태가 DELIVERED인 경우 Order와 Shipment 상태 업데이트
                if (newStatus == ShipmentStatus.DELIVERED) {
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

                }

                orderShipmentRepository.save(shipment);
            } catch (Exception e) {
                // 에러 처리 (예: 로그 남기기)
                System.err.println("배송 상태 업데이트 실패: " + e.getMessage());
            }
        }
    }

    private String getCurrentTrackingStatus(String trackingNumber) throws Exception {
//        CjTrackingPlaywright playwrightUtil = new CjTrackingPlaywright();
//        return playwrightUtil.getCurrentTrackingStatus(trackingNumber);
        return cjTrackingPlaywright.getCurrentTrackingStatus(trackingNumber);
    }

//    private String getCurrentTrackingStatus(String trackingNumber) throws Exception {
//        String url = "https://trace.cjlogistics.com/next/tracking.html?wblNo=" + trackingNumber;
//        Document doc = Jsoup.connect(url).get();
//        System.out.println("doc = " + doc);
//        // HTML에서 tbody[id=statusDetail]의 마지막 <tr>의 5번째 <td> 추출
//        Elements rows = doc.select("tbody#statusDetail tr");
//        if (rows.isEmpty()) {
//            throw new IllegalStateException("배송 정보가 없습니다.");
//        }
//        Element lastRow = rows.last();
//        Elements cells = lastRow.select("td");
//        if (cells.size() < 5) {
//            throw new IllegalStateException("배송 상태 정보를 찾을 수 없습니다.");
//        }
//        return cells.get(4).text();
//    }
    private ShipmentStatus mapToShipmentStatus(String statusText) {
        return switch (statusText) {
            case "배송완료" -> ShipmentStatus.DELIVERED;
            case "배송출발" -> ShipmentStatus.OUT_FOR_DELIVERY;
            default -> ShipmentStatus.IN_TRANSIT;
        };
    }

    @Transactional
    private void updateOrderAndSaleBidStatusToCompleted(Long orderId) {
        // OrderBid 조회
        OrderBid orderBid = orderBidQueryService.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 OrderBid를 찾을 수 없습니다: " + orderId));

        // 상태를 COMPLETED로 업데이트
        if (orderBid.getStatus() == com.fream.back.domain.order.entity.BidStatus.MATCHED) {
            orderBid.updateStatus(com.fream.back.domain.order.entity.BidStatus.COMPLETED);
        }
        // SaleBid 상태 업데이트
        SaleBid saleBid = saleBidQueryService.findByOrderId(orderId);
        if (saleBid != null && saleBid.getStatus() == BidStatus.MATCHED) {
            saleBid.updateStatus(BidStatus.COMPLETED);
        }
    }

    /**
     * 단일 송장에 대해 즉시 상태를 갱신하고, 최종 상태를 반환
     */
    @Transactional
    public ShipmentStatus updateAndCheckShipmentStatus(Long shipmentId, String courier, String trackingNumber) throws Exception {

        // 1) 기존 Shipment 로드
        OrderShipment shipment = orderShipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new IllegalArgumentException("Shipment 정보를 찾을 수 없습니다: " + shipmentId));
        System.out.println("shipment = " + shipment);
        // 2) 송장 정보 업데이트 (택배사, 송장번호)
        shipment.updateTrackingInfo(courier, trackingNumber);
        System.out.println(" 송장 정보 업데이트 ");
        // 배송 상태도 IN_TRANSIT으로 바꿔둠 (필요하다면)
//        shipment.updateStatus(ShipmentStatus.IN_TRANSIT);
        System.out.println("1234");
        // 3) CJ대한통운 HTML 스크래핑으로 현재 상태 조회
        String currentStatus = getCurrentTrackingStatus(trackingNumber);
        System.out.println("currentStatus = " + currentStatus);
        ShipmentStatus newStatus = mapToShipmentStatus(currentStatus);
        System.out.println("newStatus = " + newStatus);
        // 4) 상태가 DELIVERED인 경우 등등 로직
        if (newStatus == ShipmentStatus.DELIVERED) {
            shipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);
            shipment.updateStatus(ShipmentStatus.DELIVERED);
            // 주문 완료 로직 (completeOrder)
            completeOrder(shipment.getOrder().getId());
        }
        else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
            shipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        }
        else {
            shipment.updateStatus(ShipmentStatus.IN_TRANSIT);
        }

        // 5) 더티체킹으로 DB 반영
        // 6) 최종 상태 반환
        return shipment.getStatus();
    }

}

