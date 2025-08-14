package com.fream.back.domain.payment.service.kafka;

import com.fream.back.domain.payment.dto.PaymentRequestDto;
import com.fream.back.domain.payment.dto.kafka.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 결제 이벤트 발행 서비스
 * 주문 생성 후 비동기 결제 처리를 위해 Kafka로 이벤트 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    private static final String PAYMENT_TOPIC = "payment-processing-topic";
    private static final String PAYMENT_RETRY_TOPIC = "payment-retry-topic";

    /**
     * 결제 처리 이벤트 발행
     *
     * @param orderId 주문 ID (멱등성 보장을 위한 키)
     * @param userEmail 사용자 이메일
     * @param paymentRequest 결제 요청 정보
     */
    public void sendPaymentEvent(Long orderId, String userEmail, PaymentRequestDto paymentRequest) {
        try {
            PaymentEvent event = PaymentEvent.create(orderId, userEmail, paymentRequest);

            log.info("결제 처리 이벤트 발행 시작: 주문ID={}, 사용자={}, 이벤트ID={}",
                    orderId, userEmail, event.getEventId());

            // 주문 ID를 키로 사용하여 같은 주문은 같은 파티션으로 전송 (순서 보장)
            CompletableFuture<SendResult<String, PaymentEvent>> future =
                    kafkaTemplate.send(PAYMENT_TOPIC, orderId.toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("결제 처리 이벤트 발행 성공: 주문ID={}, 파티션={}, 오프셋={}",
                            orderId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("결제 처리 이벤트 발행 실패: 주문ID={}, 오류={}", orderId, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("결제 처리 이벤트 발행 중 오류 발생: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
            throw new RuntimeException("결제 이벤트 발행 실패", e);
        }
    }

    /**
     * 결제 재시도 이벤트 발행
     * 결제 실패 시 재시도를 위한 이벤트 전송
     *
     * @param originalEvent 원본 결제 이벤트
     */
    public void sendPaymentRetryEvent(PaymentEvent originalEvent) {
        try {
            if (originalEvent.isMaxRetryExceeded()) {
                log.warn("최대 재시도 횟수 초과로 재시도 중단: 주문ID={}, 재시도횟수={}",
                        originalEvent.getOrderId(), originalEvent.getRetryCount());
                return;
            }

            PaymentEvent retryEvent = originalEvent.withRetry();

            log.info("결제 재시도 이벤트 발행: 주문ID={}, 재시도횟수={}",
                    retryEvent.getOrderId(), retryEvent.getRetryCount());

            CompletableFuture<SendResult<String, PaymentEvent>> future =
                    kafkaTemplate.send(PAYMENT_RETRY_TOPIC, retryEvent.getOrderId().toString(), retryEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("결제 재시도 이벤트 발행 성공: 주문ID={}", retryEvent.getOrderId());
                } else {
                    log.error("결제 재시도 이벤트 발행 실패: 주문ID={}, 오류={}",
                            retryEvent.getOrderId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("결제 재시도 이벤트 발행 중 오류 발생: 주문ID={}, 오류={}",
                    originalEvent.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 동기식 결제 이벤트 발행 (테스트용)
     * 실제 운영에서는 비동기 방식 사용 권장
     */
    public void sendPaymentEventSync(Long orderId, String userEmail, PaymentRequestDto paymentRequest) {
        try {
            PaymentEvent event = PaymentEvent.create(orderId, userEmail, paymentRequest);

            log.info("동기식 결제 처리 이벤트 발행: 주문ID={}", orderId);

            SendResult<String, PaymentEvent> result =
                    kafkaTemplate.send(PAYMENT_TOPIC, orderId.toString(), event).get();

            log.info("동기식 결제 처리 이벤트 발행 완료: 주문ID={}, 파티션={}, 오프셋={}",
                    orderId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("동기식 결제 처리 이벤트 발행 실패: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
            throw new RuntimeException("동기식 결제 이벤트 발행 실패", e);
        }
    }
}