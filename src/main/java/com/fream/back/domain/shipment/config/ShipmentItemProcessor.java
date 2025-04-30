package com.fream.back.domain.shipment.config;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.global.utils.PlaywrightBrowserManager;
import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.batch.item.ItemProcessor;

/**
 * 배송 상태 배치 처리를 위한 ItemProcessor
 * 주문 배송 상태를 조회하고 처리합니다.
 */
@Slf4j
public class ShipmentItemProcessor implements ItemProcessor<OrderShipment, OrderShipment> {

    private final PlaywrightBrowserManager browserManager;
    private final NotificationCommandService notificationService;
    private final OrderShipmentCommandService orderService;

    private static final String TRACKING_URL_PREFIX = "https://trace.cjlogistics.com/next/tracking.html?wblNo=";
    private static final String STATUS_SELECTOR = "tbody#statusDetail tr";
    private static final int TIMEOUT_MS = 30000; // 30초

    /**
     * 배송 상태 처리기 생성자
     *
     * @param browserManager Playwright 브라우저 관리자
     * @param notificationService 알림 서비스
     * @param orderService 주문 서비스
     */
    public ShipmentItemProcessor(
            PlaywrightBrowserManager browserManager,
            NotificationCommandService notificationService,
            OrderShipmentCommandService orderService
    ) {
        this.browserManager = browserManager;
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

        Page page = null;
        try {
            // browserManager를 통해 페이지 생성
            page = browserManager.newPage();
            if (page == null) {
                throw new ShipmentException(ShipmentErrorCode.BROWSER_INITIALIZATION_ERROR);
            }

            // 타임아웃 설정
            page.setDefaultTimeout(TIMEOUT_MS);

            // 배송 추적 URL로 이동
            String url = TRACKING_URL_PREFIX + trackingNumber;
            log.debug("배송 조회 페이지로 이동: {}", url);
            page.navigate(url);

            // 배송 상태 테이블 로드 대기
            log.debug("배송 상태 테이블 로드 대기");
            page.waitForSelector(STATUS_SELECTOR);

            // 최종 HTML 가져오기
            String renderedHtml = page.content();
            log.debug("페이지 HTML 로드 완료 (길이: {})", renderedHtml.length());

            // HTML 파싱하여 배송 상태 추출
            String statusText = parseStatusFromHtml(renderedHtml, trackingNumber);
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
        } finally {
            // 페이지 리소스 해제
            if (page != null) {
                try {
                    page.close();
                    log.debug("페이지 리소스 해제 완료");
                } catch (Exception e) {
                    log.warn("페이지 닫기 실패: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * HTML에서 배송 상태를 파싱합니다.
     *
     * @param html 파싱할 HTML 문자열
     * @param trackingNumber 로깅용 운송장 번호
     * @return 배송 상태 문자열
     * @throws ShipmentException 파싱 실패 시 발생
     */
    private String parseStatusFromHtml(String html, String trackingNumber) {
        try {
            Document doc = Jsoup.parse(html);
            Elements rows = doc.select(STATUS_SELECTOR);

            if (rows.isEmpty()) {
                log.warn("배송 상태 테이블이 비어 있음: 운송장={}", trackingNumber);
                throw new ShipmentException(
                        ShipmentErrorCode.TRACKING_HTML_PARSE_ERROR,
                        "배송 정보가 없습니다. 운송장 번호를 확인해주세요: " + trackingNumber
                );
            }

            // 마지막 행(최신 상태)의 5번째 컬럼 선택
            String statusText = rows.last().select("td").get(4).text().trim();

            if (statusText.isEmpty()) {
                log.warn("배송 상태 텍스트가 비어 있음: 운송장={}", trackingNumber);
                throw new ShipmentException(
                        ShipmentErrorCode.TRACKING_HTML_PARSE_ERROR,
                        "배송 상태 정보를 찾을 수 없습니다."
                );
            }

            return statusText;

        } catch (IndexOutOfBoundsException e) {
            log.error("HTML 파싱 중 인덱스 오류: 운송장={}, 오류={}", trackingNumber, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.TRACKING_HTML_PARSE_ERROR,
                    "배송 상태 정보 파싱 실패: 예상한 형식과 다릅니다.",
                    e
            );
        } catch (Exception e) {
            if (e instanceof ShipmentException) {
                throw (ShipmentException) e;
            }

            log.error("HTML 파싱 중 오류 발생: 운송장={}, 오류={}", trackingNumber, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.TRACKING_HTML_PARSE_ERROR,
                    "배송 상태 정보 파싱 실패: " + e.getMessage(),
                    e
            );
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