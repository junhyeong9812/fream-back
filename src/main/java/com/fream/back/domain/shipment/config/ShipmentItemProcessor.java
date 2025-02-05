package com.fream.back.domain.shipment.config;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.global.utils.PlaywrightBrowserManager;
import com.microsoft.playwright.Page;
import org.springframework.batch.item.ItemProcessor;


public class ShipmentItemProcessor implements ItemProcessor<OrderShipment, OrderShipment> {

    private final PlaywrightBrowserManager browserManager;
    private final NotificationCommandService notificationService;
    private final OrderShipmentCommandService orderService;

    public ShipmentItemProcessor(
            PlaywrightBrowserManager browserManager,
            NotificationCommandService notificationService,
            OrderShipmentCommandService orderService
    ) {
        this.browserManager = browserManager;
        this.notificationService = notificationService;
        this.orderService = orderService;
    }

    @Override
    public OrderShipment process(OrderShipment orderShipment) throws Exception {
        String trackingNumber = orderShipment.getTrackingNumber();

        // (1) 열린 브라우저에서 새 페이지 생성
        Page page = browserManager.newPage();

        // (2) URL 이동
        page.navigate("https://trace.cjlogistics.com/next/tracking.html?wblNo=" + trackingNumber);

        // (3) 셀렉터 대기
        page.waitForSelector("tbody#statusDetail tr");

        // (4) 최종 HTML
        String renderedHtml = page.content();
        page.close(); // 페이지만 닫고, 브라우저는 계속 열려 있음

        // (5) Jsoup 파싱
        // ... (기존 로직) ...
        ShipmentStatus newStatus = parseStatusFromHtml(renderedHtml);

        // (6) 상태 업데이트 + 알림
        if (newStatus == ShipmentStatus.DELIVERED) {
            orderShipment.updateStatus(ShipmentStatus.DELIVERED);
            orderService.completeOrder(orderShipment.getOrder().getId());
            notificationService.notifyShipmentCompleted(orderShipment.getOrder());
        } else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
            orderShipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);
        } else {
            orderShipment.updateStatus(ShipmentStatus.IN_TRANSIT);
        }

        return orderShipment;
    }

    private ShipmentStatus parseStatusFromHtml(String html) {
        // Jsoup 분석 후 "배송완료", "배송출발" 등 매핑
        // ...
        // (기존 mapToShipmentStatus 로직 그대로)
        return ShipmentStatus.IN_TRANSIT;
    }
}
