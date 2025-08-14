package com.fream.back.domain.order.controller.query;

import com.fream.back.domain.order.dto.OrderStatusDto;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderStatus;
import com.fream.back.domain.order.repository.OrderRepository;
import com.fream.back.domain.order.service.query.OrderQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 주문 조회 컨트롤러
 * 주문 상태 조회, 주문 내역 조회 등 조회 관련 API 제공
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@Validated
public class OrderQueryController {

    private final OrderRepository orderRepository;
    // private final OrderQueryService orderQueryService; // TODO: 추후 구현

    /**
     * 주문 처리 상태 조회 API
     * 클라이언트가 폴링으로 상태를 확인할 수 있도록 제공
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getOrderStatus(@PathVariable Long orderId) {
        String email = SecurityUtils.extractAndValidateEmailForOrder("주문 상태 조회");

        log.info("주문 상태 조회 요청: 주문ID={}, 사용자={}", orderId, email);

        try {
            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            // 권한 확인
            if (!order.getUser().getEmail().equals(email)) {
                log.warn("주문 상태 조회 권한 없음: 주문ID={}, 요청자={}, 주문자={}",
                        orderId, email, order.getUser().getEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseDto.error("ACCESS_DENIED", "해당 주문에 대한 접근 권한이 없습니다."));
            }

            // 상태 정보 구성
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("orderId", orderId);
            statusData.put("status", order.getStatus().name());
            statusData.put("statusDescription", getStatusDescription(order.getStatus()));
            statusData.put("canCancel", canCancelOrder(order.getStatus()));
            statusData.put("lastUpdated", order.getUpdatedAt());
            statusData.put("totalAmount", order.getTotalAmount());

            // 결제 정보 추가
            if (order.getPayment() != null) {
                Map<String, Object> paymentInfo = new HashMap<>();
                paymentInfo.put("paymentId", order.getPayment().getId());
                paymentInfo.put("paymentStatus", order.getPayment().getStatus().name());
                paymentInfo.put("isSuccess", order.getPayment().isSuccess());
                paymentInfo.put("paidAmount", order.getPayment().getPaidAmount());
                statusData.put("payment", paymentInfo);
            }

            // 배송 정보 추가
            if (order.getOrderShipment() != null) {
                Map<String, Object> shipmentInfo = new HashMap<>();
                shipmentInfo.put("receiverName", order.getOrderShipment().getReceiverName());
                shipmentInfo.put("receiverPhone", order.getOrderShipment().getReceiverPhone());
                shipmentInfo.put("address", order.getOrderShipment().getAddress());
                statusData.put("shipment", shipmentInfo);
            }

            // 창고 보관 정보 추가
            if (order.getWarehouseStorage() != null) {
                Map<String, Object> warehouseInfo = new HashMap<>();
                warehouseInfo.put("warehouseId", order.getWarehouseStorage().getId());
                warehouseInfo.put("warehouseStatus", order.getWarehouseStorage().getStatus().name());
                statusData.put("warehouse", warehouseInfo);
            }

            return ResponseEntity.ok(ResponseDto.success(statusData, "주문 상태 조회 성공"));

        } catch (IllegalArgumentException e) {
            log.error("주문 상태 조회 실패 - 주문 없음: 주문ID={}, 사용자={}, 오류={}", orderId, email, e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.error("ORDER_NOT_FOUND",
                            "주문을 찾을 수 없습니다: " + e.getMessage()));

        } catch (Exception e) {
            log.error("주문 상태 조회 실패: 주문ID={}, 사용자={}, 오류={}", orderId, email, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("ORDER_STATUS_QUERY_FAILED",
                            "주문 상태 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 상세 정보 조회 API
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ResponseDto<Map<String, Object>>> getOrderDetails(@PathVariable Long orderId) {
        String email = SecurityUtils.extractAndValidateEmailForOrder("주문 상세 조회");

        log.info("주문 상세 조회 요청: 주문ID={}, 사용자={}", orderId, email);

        try {
            // 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            // 권한 확인
            if (!order.getUser().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseDto.error("ACCESS_DENIED", "해당 주문에 대한 접근 권한이 없습니다."));
            }

            // 상세 정보 구성
            Map<String, Object> orderDetails = new HashMap<>();
            orderDetails.put("orderId", orderId);
            orderDetails.put("status", order.getStatus().name());
            orderDetails.put("statusDescription", getStatusDescription(order.getStatus()));
            orderDetails.put("totalAmount", order.getTotalAmount());
            orderDetails.put("discountAmount", order.getDiscountAmount());
            orderDetails.put("usedPoints", order.getUsedPoints());
            orderDetails.put("createdAt", order.getCreatedDate());
            orderDetails.put("updatedAt", order.getModifiedDate());

            // TODO: OrderItem 정보 추가
            // TODO: 결제 상세 정보 추가
            // TODO: 배송 상세 정보 추가

            return ResponseEntity.ok(ResponseDto.success(orderDetails, "주문 상세 조회 성공"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDto.error("ORDER_NOT_FOUND",
                            "주문을 찾을 수 없습니다: " + e.getMessage()));

        } catch (Exception e) {
            log.error("주문 상세 조회 실패: 주문ID={}, 사용자={}, 오류={}", orderId, email, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("ORDER_DETAIL_QUERY_FAILED",
                            "주문 상세 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자의 주문 목록 조회 API
     */
    @GetMapping
    public ResponseEntity<ResponseDto<Map<String, Object>>> getUserOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {

        String email = SecurityUtils.extractAndValidateEmailForOrder("주문 목록 조회");

        log.info("사용자 주문 목록 조회 요청: 사용자={}, 페이지={}, 크기={}, 상태필터={}",
                email, page, size, status);

        try {
            // TODO: OrderQueryService에서 페이징된 주문 목록 조회 구현
            // Page<Order> orders = orderQueryService.getUserOrders(email, page, size, status);

            // 임시 응답
            Map<String, Object> orderList = new HashMap<>();
            orderList.put("orders", new Object[]{});
            orderList.put("totalElements", 0);
            orderList.put("totalPages", 0);
            orderList.put("currentPage", page);
            orderList.put("size", size);

            return ResponseEntity.ok(ResponseDto.success(orderList, "주문 목록 조회 성공"));

        } catch (Exception e) {
            log.error("주문 목록 조회 실패: 사용자={}, 오류={}", email, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDto.error("ORDER_LIST_QUERY_FAILED",
                            "주문 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 주문 상태에 대한 설명 반환
     */
    private String getStatusDescription(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "결제 대기 중";
            case PAYMENT_COMPLETED -> "결제 완료";
            case PREPARING -> "상품 준비 중";
            case IN_WAREHOUSE -> "창고 보관 중";
            case SHIPMENT_STARTED -> "배송 시작";
            case IN_TRANSIT -> "배송 중";
            case COMPLETED -> "배송 완료";
            case REFUND_REQUESTED -> "환불 요청";
            case REFUNDED -> "환불 완료";
        };
    }

    /**
     * 주문 취소 가능 여부 확인
     */
    private boolean canCancelOrder(OrderStatus status) {
        return status == OrderStatus.PENDING_PAYMENT ||
                status == OrderStatus.PAYMENT_COMPLETED ||
                status == OrderStatus.PREPARING;
    }
}