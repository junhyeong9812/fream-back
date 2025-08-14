package com.fream.back.domain.order.service.kafka;

import com.fream.back.domain.order.dto.PayAndShipmentRequestDto;
import com.fream.back.domain.order.dto.kafka.OrderProcessingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 주문 처리 이벤트 발행 서비스
 * 결제 + 배송 + 창고보관을 포함한 전체 주문 처리를 비동기로 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderProcessingEvent> kafkaTemplate;

    private static final String ORDER_PROCESSING_TOPIC = "order-processing-topic";
    private static final String ORDER_RETRY_TOPIC = "order-retry-topic";

    /**
     * 주문 처리 이벤트 발행
     *
     * @param orderId 주문 ID
     * @param userEmail 사용자 이메일
     * @param requestDto 결제 및 배송 요청 정보
     */
    public void sendOrderProcessingEvent(Long orderId, String userEmail, PayAndShipmentRequestDto requestDto) {
        try {
            OrderProcessingEvent event = OrderProcessingEvent.create(orderId, userEmail, requestDto);

            log.info("주문 처리 이벤트 발행 시작: 주문ID={}, 사용자={}, 이벤트ID={}",
                    orderId, userEmail, event.getEventId());

            // 주문 ID를 키로 사용하여 순서 보장
            CompletableFuture<SendResult<String, OrderProcessingEvent>> future =
                    kafkaTemplate.send(ORDER_PROCESSING_TOPIC, orderId.toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("주문 처리 이벤트 발행 성공: 주문ID={}, 파티션={}, 오프셋={}",
                            orderId,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    log.error("주문 처리 이벤트 발행 실패: 주문ID={}, 오류={}", orderId, ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("주문 처리 이벤트 발행 중 오류 발생: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
            throw new RuntimeException("주문 처리 이벤트 발행 실패", e);
        }
    }

    /**
     * 주문 처리 재시도 이벤트 발행
     */
    public void sendOrderRetryEvent(OrderProcessingEvent originalEvent) {
        try {
            if (originalEvent.isMaxRetryExceeded()) {
                log.warn("최대 재시도 횟수 초과로 재시도 중단: 주문ID={}, 재시도횟수={}",
                        originalEvent.getOrderId(), originalEvent.getRetryCount());
                return;
            }

            OrderProcessingEvent retryEvent = originalEvent.withRetry();

            log.info("주문 처리 재시도 이벤트 발행: 주문ID={}, 재시도횟수={}",
                    retryEvent.getOrderId(), retryEvent.getRetryCount());

            CompletableFuture<SendResult<String, OrderProcessingEvent>> future =
                    kafkaTemplate.send(ORDER_RETRY_TOPIC, retryEvent.getOrderId().toString(), retryEvent);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("주문 처리 재시도 이벤트 발행 성공: 주문ID={}", retryEvent.getOrderId());
                } else {
                    log.error("주문 처리 재시도 이벤트 발행 실패: 주문ID={}, 오류={}",
                            retryEvent.getOrderId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("주문 처리 재시도 이벤트 발행 중 오류 발생: 주문ID={}, 오류={}",
                    originalEvent.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 동기식 주문 처리 이벤트 발행 (테스트용)
     */
    public void sendOrderProcessingEventSync(Long orderId, String userEmail, PayAndShipmentRequestDto requestDto) {
        try {
            OrderProcessingEvent event = OrderProcessingEvent.create(orderId, userEmail, requestDto);

            log.info("동기식 주문 처리 이벤트 발행: 주문ID={}", orderId);

            SendResult<String, OrderProcessingEvent> result =
                    kafkaTemplate.send(ORDER_PROCESSING_TOPIC, orderId.toString(), event).get();

            log.info("동기식 주문 처리 이벤트 발행 완료: 주문ID={}, 파티션={}, 오프셋={}",
                    orderId,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("동기식 주문 처리 이벤트 발행 실패: 주문ID={}, 오류={}", orderId, e.getMessage(), e);
            throw new RuntimeException("동기식 주문 처리 이벤트 발행 실패", e);
        }
    }
}