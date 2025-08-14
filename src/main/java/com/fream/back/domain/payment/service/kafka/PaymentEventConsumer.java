package com.fream.back.domain.payment.service.kafka;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.repository.OrderRepository;
import com.fream.back.domain.payment.dto.kafka.PaymentEvent;
import com.fream.back.domain.payment.entity.Payment;
import com.fream.back.domain.payment.exception.PaymentException;
import com.fream.back.domain.payment.repository.PaymentRepository;
import com.fream.back.domain.payment.service.command.PaymentCommandService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 결제 이벤트 컨슈머
 * Kafka로부터 결제 처리 이벤트를 수신하여 실제 결제 처리 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentCommandService paymentCommandService;
    private final PaymentEventProducer paymentEventProducer;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserQueryService userQueryService;

    /**
     * 결제 처리 이벤트 리스너
     * 멱등성을 보장하여 중복 결제를 방지
     */
    @KafkaListener(
            topics = "payment-processing-topic",
            groupId = "payment-processing-group",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentEvent(
            @Payload PaymentEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        Instant start = Instant.now();

        try {
            log.info("결제 처리 이벤트 수신: 주문ID={}, 이벤트ID={}, 파티션={}, 오프셋={}",
                    event.getOrderId(), event.getEventId(), partition, offset);

            // 1. 멱등성 검사 - 이미 처리된 주문인지 확인
            if (isPaymentAlreadyProcessed(event.getOrderId())) {
                log.warn("이미 처리된 결제 요청 - 중복 처리 방지: 주문ID={}", event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // 2. 주문 정보 조회
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "주문을 찾을 수 없습니다. 주문ID: " + event.getOrderId()));

            // 3. 사용자 정보 조회
            User user = userQueryService.findByEmail(event.getUserEmail());

            // 4. 주문과 사용자 일치 여부 확인
            if (!order.getUser().getId().equals(user.getId())) {
                log.error("주문과 사용자 정보 불일치: 주문ID={}, 주문사용자ID={}, 요청사용자ID={}",
                        event.getOrderId(), order.getUser().getId(), user.getId());
                throw new PaymentException(null, "주문과 사용자 정보가 일치하지 않습니다.");
            }

            // 5. 결제 처리 수행
            Payment payment = paymentCommandService.processPayment(order, user, event.getPaymentRequest());

            log.info("결제 처리 완료: 주문ID={}, 결제ID={}, 결제상태={}",
                    event.getOrderId(), payment.getId(), payment.getStatus());

            // 6. 처리 완료 ACK
            acknowledgment.acknowledge();

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.info("결제 처리 완료 - 소요시간: {}ms, 주문ID={}", duration.toMillis(), event.getOrderId());

        } catch (PaymentException e) {
            log.error("결제 처리 실패 (PaymentException): 주문ID={}, 오류={}",
                    event.getOrderId(), e.getMessage(), e);
            handlePaymentFailure(event, e, acknowledgment);

        } catch (Exception e) {
            log.error("결제 처리 중 예상치 못한 오류: 주문ID={}, 오류={}",
                    event.getOrderId(), e.getMessage(), e);
            handlePaymentFailure(event, e, acknowledgment);
        }
    }

    /**
     * 결제 재시도 이벤트 리스너
     */
    @KafkaListener(
            topics = "payment-retry-topic",
            groupId = "payment-retry-group",
            containerFactory = "paymentEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void handlePaymentRetryEvent(
            @Payload PaymentEvent event,
            Acknowledgment acknowledgment) {

        try {
            log.info("결제 재시도 처리: 주문ID={}, 재시도횟수={}",
                    event.getOrderId(), event.getRetryCount());

            // 재시도 횟수 확인
            if (event.isMaxRetryExceeded()) {
                log.error("최대 재시도 횟수 초과: 주문ID={}, 재시도횟수={}",
                        event.getOrderId(), event.getRetryCount());
                acknowledgment.acknowledge();
                return;
            }

            // 멱등성 재확인
            if (isPaymentAlreadyProcessed(event.getOrderId())) {
                log.info("재시도 중 결제 완료 확인: 주문ID={}", event.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // 재시도 지연 (백오프 전략)
            Thread.sleep(Math.min(1000 * event.getRetryCount(), 5000));

            // 결제 재처리
            handlePaymentEvent(event, 0, 0, acknowledgment);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("결제 재시도 중 인터럽트 발생: 주문ID={}", event.getOrderId());
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("결제 재시도 처리 중 오류: 주문ID={}, 오류={}",
                    event.getOrderId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * 결제가 이미 처리되었는지 확인 (멱등성 보장)
     */
    private boolean isPaymentAlreadyProcessed(Long orderId) {
        try {
            Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);

            if (existingPayment.isPresent()) {
                Payment payment = existingPayment.get();
                log.info("기존 결제 발견: 주문ID={}, 결제ID={}, 결제상태={}",
                        orderId, payment.getId(), payment.getStatus());

                // 성공한 결제가 있으면 중복 처리로 간주
                return payment.isSuccess();
            }

            return false;
        } catch (Exception e) {
            log.error("결제 중복 검사 중 오류 발생: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 결제 실패 처리
     */
    private void handlePaymentFailure(PaymentEvent event, Exception error, Acknowledgment acknowledgment) {
        try {
            // 재시도 가능한 오류인지 판단
            if (isRetryableError(error)) {
                log.info("재시도 가능한 오류로 재시도 이벤트 발행: 주문ID={}", event.getOrderId());
                paymentEventProducer.sendPaymentRetryEvent(event);
            } else {
                log.error("재시도 불가능한 오류로 결제 실패 처리: 주문ID={}, 오류={}",
                        event.getOrderId(), error.getMessage());
                // TODO: 결제 실패 알림 등 추가 처리
            }

            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: 주문ID={}, 오류={}", event.getOrderId(), e.getMessage(), e);
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
}