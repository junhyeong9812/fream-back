package com.fream.back.domain.order.service.kafka;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.dto.kafka.OrderProcessingEvent;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderStatus;
import com.fream.back.domain.order.repository.OrderRepository;
import com.fream.back.domain.order.service.command.OrderCommandService;
import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.payment.service.command.PaymentCommandService;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 주문 처리 이벤트 컨슈머
 * 결제 + 배송 + 창고보관을 포함한 전체 주문 처리를 트랜잭션으로 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final PaymentCommandService paymentCommandService;
    private final OrderShipmentCommandService orderShipmentCommandService;
    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final UserQueryService userQueryService;
    private final OrderEventProducer orderEventProducer;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메시지 전송
    private final KafkaTemplate<String, Map<String, Object>> notificationKafkaTemplate;

    /**
     * 주문 처리 이벤트 리스너
     * 전체 주문 처리를 하나의 트랜잭션으로 수행
     */
    @KafkaListener(
            topics = "order-processing-topic",
            groupId = "order-processing-group",
            containerFactory = "orderProcessingKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderProcessingEvent(
            @Payload OrderProcessingEvent event,
            Acknowledgment acknowledgment) {

        Instant start = Instant.now();

        try {
            log.info("주문 처리 이벤트 수신: 주문ID={}, 이벤트ID={}",
                    event.getOrderId(), event.getEventId());

            // 1. 멱등성 검사 - 이미 처리된 주문인지 확인
            if (isOrderAlreadyProcessed(event.getOrderId())) {
                log.warn("이미 처리된 주문 요청 - 중복 처리 방지: 주문ID={}", event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // 2. 주문 및 사용자 정보 조회
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "주문을 찾을 수 없습니다. 주문ID: " + event.getOrderId()));

            User user = userQueryService.findByEmail(event.getUserEmail());

            // 3. 권한 확인
            if (!order.getUser().getId().equals(user.getId())) {
                log.error("주문과 사용자 정보 불일치: 주문ID={}, 주문사용자ID={}, 요청사용자ID={}",
                        event.getOrderId(), order.getUser().getId(), user.getId());
                throw new IllegalArgumentException("주문과 사용자 정보가 일치하지 않습니다.");
            }

            // 4. 전체 주문 처리 수행 (하나의 트랜잭션)
            processCompleteOrder(order, user, event.getRequestDto());

            // 5. 성공 알림 전송
            sendSuccessNotification(event.getOrderId(), event.getUserEmail());

            // 6. 처리 완료 ACK
            acknowledgment.acknowledge();

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.info("주문 처리 완료 - 소요시간: {}ms, 주문ID={}", duration.toMillis(), event.getOrderId());

        } catch (Exception e) {
            log.error("주문 처리 실패: 주문ID={}, 오류={}", event.getOrderId(), e.getMessage(), e);
            handleOrderProcessingFailure(event, e, acknowledgment);
        }
    }

    /**
     * 주문 재시도 이벤트 리스너
     */
    @KafkaListener(
            topics = "order-retry-topic",
            groupId = "order-retry-group",
            containerFactory = "orderProcessingKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderRetryEvent(
            @Payload OrderProcessingEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("주문 재시도 처리: 주문ID={}, 재시도횟수={}",
                    event.getOrderId(), event.getRetryCount());

            if (event.isMaxRetryExceeded()) {
                log.error("최대 재시도 횟수 초과: 주문ID={}, 재시도횟수={}",
                        event.getOrderId(), event.getRetryCount());

                // 최종 실패 알림
                sendFailureNotification(event.getOrderId(), event.getUserEmail(), "최대 재시도 횟수 초과");
                acknowledgment.acknowledge();
                return;
            }

            // 멱등성 재확인
            if (isOrderAlreadyProcessed(event.getOrderId())) {
                log.info("재시도 중 주문 완료 확인: 주문ID={}", event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // 재시도 지연 (백오프 전략)
            Thread.sleep(Math.min(1000 * event.getRetryCount(), 5000));

            // 주문 재처리
            handleOrderProcessingEvent(event, acknowledgment);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("주문 재시도 중 인터럽트 발생: 주문ID={}", event.getOrderId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("주문 재시도 처리 중 오류: 주문ID={}, 오류={}",
                    event.getOrderId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * 전체 주문 처리 수행 (하나의 트랜잭션)
     */
    private void processCompleteOrder(Order order, User user, PayAndShipmentRequestDto requestDto) {
        try {
            log.info("전체 주문 처리 시작: 주문ID={}", order.getId());

            // 1. 결제 처리
            Payment payment = paymentCommandService.processPayment(order, user, requestDto.getPaymentRequest());
            order.assignPayment(payment);

            if (!payment.isSuccess()) {
                throw new RuntimeException("결제 실패: " + payment.getStatus());
            }

            log.info("결제 처리 완료: 주문ID={}, 결제ID={}", order.getId(), payment.getId());

            // 2. 배송 정보 생성
            OrderShipment shipment = orderShipmentCommandService.createOrderShipment(
                    order,
                    requestDto.getReceiverName(),
                    requestDto.getReceiverPhone(),
                    requestDto.getPostalCode(),
                    requestDto.getAddress()
            );
            order.assignOrderShipment(shipment);
            log.info("배송 정보 생성 완료: 주문ID={}", order.getId());

            // 3. 상태 업데이트 (OrderStatus enum 사용)
            if (!order.getStatus().canTransitionTo(OrderStatus.PAYMENT_COMPLETED)) {
                throw new RuntimeException("결제 완료 상태로 전환할 수 없습니다. 현재 상태: " + order.getStatus());
            }
            order.updateStatus(OrderStatus.PAYMENT_COMPLETED);

            if (requestDto.isWarehouseStorage()) {
                // 창고 보관일 경우
                WarehouseStorage warehouseStorage = warehouseStorageCommandService.createOrderStorage(order, user);
                order.assignWarehouseStorage(warehouseStorage);

                // 상태 전환 검증
                if (order.getStatus().canTransitionTo(OrderStatus.PREPARING)) {
                    order.updateStatus(OrderStatus.PREPARING);
                }
                if (order.getStatus().canTransitionTo(OrderStatus.IN_WAREHOUSE)) {
                    order.updateStatus(OrderStatus.IN_WAREHOUSE);
                }
                if (order.getStatus().canTransitionTo(OrderStatus.COMPLETED)) {
                    order.updateStatus(OrderStatus.COMPLETED);
                }

                log.info("창고 보관 처리 완료: 주문ID={}", order.getId());
            } else {
                // 실제 배송일 경우
                if (order.getStatus().canTransitionTo(OrderStatus.PREPARING)) {
                    order.updateStatus(OrderStatus.PREPARING);
                }
                log.info("배송 준비 완료: 주문ID={}", order.getId());
            }

            // 4. 주문 저장
            orderRepository.save(order);
            log.info("전체 주문 처리 완료: 주문ID={}, 최종상태={}", order.getId(), order.getStatus());

        } catch (Exception e) {
            log.error("전체 주문 처리 실패: 주문ID={}, 오류={}", order.getId(), e.getMessage(), e);
            throw e; // 트랜잭션 롤백을 위해 예외 재발생
        }
    }

    /**
     * 주문이 이미 처리되었는지 확인 (멱등성 보장)
     */
    private boolean isOrderAlreadyProcessed(Long orderId) {
        try {
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return false;
            }

            // PENDING_PAYMENT 상태가 아니면 이미 처리된 것으로 간주
            OrderStatus currentStatus = order.getStatus();
            return currentStatus != OrderStatus.PENDING_PAYMENT;

        } catch (Exception e) {
            log.error("주문 중복 검사 중 오류 발생: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 주문 처리 실패 처리
     */
    private void handleOrderProcessingFailure(OrderProcessingEvent event, Exception error, Acknowledgment acknowledgment) {
        try {
            // 재시도 가능한 오류인지 판단
            if (isRetryableError(error)) {
                log.info("재시도 가능한 오류로 재시도 이벤트 발행: 주문ID={}", event.getOrderId());
                orderEventProducer.sendOrderRetryEvent(event);
            } else {
                log.error("재시도 불가능한 오류로 주문 처리 실패: 주문ID={}, 오류={}",
                        event.getOrderId(), error.getMessage());
                sendFailureNotification(event.getOrderId(), event.getUserEmail(), error.getMessage());
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("주문 처리 실패 처리 중 오류: 주문ID={}, 오류={}", event.getOrderId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * 재시도 가능한 오류인지 판단
     */
    private boolean isRetryableError(Exception error) {
        // 네트워크 오류, 일시적인 시스템 오류 등은 재시도 가능
        return !(error instanceof IllegalArgumentException) &&
                !(error.getMessage() != null && error.getMessage().contains("찾을 수 없습니다"));
    }

    /**
     * 성공 알림 전송 (WebSocket + 선택적으로 Kafka)
     */
    private void sendSuccessNotification(Long orderId, String userEmail) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("orderId", orderId);
            notification.put("userEmail", userEmail);
            notification.put("status", "SUCCESS");
            notification.put("type", "ORDER_COMPLETED");
            notification.put("title", "주문 완료");
            notification.put("message", "주문 및 결제가 성공적으로 완료되었습니다!");
            notification.put("timestamp", LocalDateTime.now());

            // 1. 개별 사용자에게 WebSocket 알림 (추천 방식)
            String userDestination = "/user/" + userEmail + "/queue/notifications";
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification);

            // 2. 주문별 토픽으로 WebSocket 알림 (대안 방식)
            String orderDestination = "/topic/order/" + orderId;
            messagingTemplate.convertAndSend(orderDestination, notification);

            log.info("주문 성공 WebSocket 알림 전송 완료: 주문ID={}, 사용자={}, 대상={}",
                    orderId, userEmail, userDestination);

            // 3. 선택사항: 추가 알림 서비스로 Kafka 이벤트 발행
            try {
                Map<String, Object> kafkaNotification = new HashMap<>();
                kafkaNotification.put("type", "ORDER_COMPLETION");
                kafkaNotification.put("orderId", orderId);
                kafkaNotification.put("userEmail", userEmail);
                kafkaNotification.put("message", "주문이 완료되었습니다");
                kafkaNotification.put("timestamp", System.currentTimeMillis());

                notificationKafkaTemplate.send("notification-topic", userEmail, kafkaNotification);
                log.debug("주문 완료 Kafka 알림 이벤트 발행: 주문ID={}", orderId);

            } catch (Exception kafkaError) {
                log.warn("Kafka 알림 이벤트 발행 실패 (WebSocket 알림은 성공): 주문ID={}, 오류={}",
                        orderId, kafkaError.getMessage());
            }

        } catch (Exception e) {
            log.error("성공 알림 전송 실패: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 실패 알림 전송
     */
    private void sendFailureNotification(Long orderId, String userEmail, String errorMessage) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("orderId", orderId);
            notification.put("userEmail", userEmail);
            notification.put("status", "FAILED");
            notification.put("type", "ORDER_FAILED");
            notification.put("title", "주문 실패");
            notification.put("message", "주문 처리에 실패했습니다: " + errorMessage);
            notification.put("timestamp", LocalDateTime.now());
            notification.put("retryable", isRetryableError(new RuntimeException(errorMessage)));

            // 1. 개별 사용자에게 WebSocket 알림
            messagingTemplate.convertAndSendToUser(userEmail, "/queue/notifications", notification);

            // 2. 주문별 토픽으로 WebSocket 알림
            String orderDestination = "/topic/order/" + orderId;
            messagingTemplate.convertAndSend(orderDestination, notification);

            log.info("주문 실패 WebSocket 알림 전송 완료: 주문ID={}, 사용자={}", orderId, userEmail);

            // 3. 실패 알림도 Kafka로 추가 처리 (이메일, SMS 등)
            try {
                Map<String, Object> kafkaNotification = new HashMap<>();
                kafkaNotification.put("type", "ORDER_FAILURE");
                kafkaNotification.put("orderId", orderId);
                kafkaNotification.put("userEmail", userEmail);
                kafkaNotification.put("errorMessage", errorMessage);
                kafkaNotification.put("timestamp", System.currentTimeMillis());

                notificationKafkaTemplate.send("notification-topic", userEmail, kafkaNotification);

            } catch (Exception kafkaError) {
                log.warn("Kafka 실패 알림 이벤트 발행 실패: 주문ID={}, 오류={}",
                        orderId, kafkaError.getMessage());
            }

        } catch (Exception e) {
            log.error("실패 알림 전송 실패: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
        }
    }
}