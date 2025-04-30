package com.fream.back.domain.shipment.config;

import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.shipment.entity.OrderShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.repository.OrderShipmentRepository;
import com.fream.back.domain.shipment.service.command.OrderShipmentCommandService;
import com.fream.back.global.utils.CjTrackingPlaywright;
import com.fream.back.global.utils.PlaywrightBrowserManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * 배송 상태 자동 업데이트를 위한 배치 작업 구성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UpdateShipmentStatusesJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    private final OrderShipmentRepository orderShipmentRepository;
    private final NotificationCommandService notificationService;
    private final OrderShipmentCommandService orderService;
    private final CjTrackingPlaywright cjTrackingPlaywright;

    private static final int CHUNK_SIZE = 50;
    private static final int SKIP_LIMIT = 50;

    @Bean
    public Job updateShipmentStatusesJob() {
        log.info("배송 상태 업데이트 작업 구성");
        return new JobBuilder("updateShipmentStatusesJob", jobRepository)
                .start(updateShipmentStatusesStep())
                .build();
    }

    @Bean
    public Step updateShipmentStatusesStep() {
        log.info("배송 상태 업데이트 스텝 구성: 청크 크기={}, 스킵 제한={}", CHUNK_SIZE, SKIP_LIMIT);

        return new StepBuilder("updateShipmentStatusesStep", jobRepository)
                .<OrderShipment, OrderShipment>chunk(CHUNK_SIZE, transactionManager)
                .reader(shipmentItemReader())
                .processor(new ShipmentItemProcessor(
                        playwrightBrowserManager(), // Bean or new
                        notificationService,
                        orderService
                ))
                .writer(shipmentJpaItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(SKIP_LIMIT)
                .listener(shipmentSkipListener())
                // ★ Step Execution Listener로 Browser 관리 리스너 추가
                .listener(new BrowserManageStepListener(playwrightBrowserManager()))
                .build();
    }

    /**
     * 1) Reader: 상태가 IN_TRANSIT 또는 OUT_FOR_DELIVERY 인 OrderShipment 목록을 페이지 단위로 읽음
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<OrderShipment> shipmentItemReader() {
        log.debug("배송 상태 업데이트 ItemReader 구성");
        return new JpaPagingItemReaderBuilder<OrderShipment>()
                .name("shipmentItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT s FROM OrderShipment s WHERE s.status IN ('IN_TRANSIT','OUT_FOR_DELIVERY') AND s.trackingNumber IS NOT NULL")
                .build();
    }

    /**
     * 2) Processor: 각 OrderShipment에 대해 CJ대한통운 페이지를 조회하여 상태 갱신
     *    - parseCjLogisticsStatus(...) 를 통해 배송 상태 파싱
     *    - DELIVERY 완료 시 알림, Order 상태 완료 등도 여기서 처리 가능
     */
    @Bean
    @StepScope
    public ItemProcessor<OrderShipment, OrderShipment> shipmentItemProcessor() {
        log.debug("배송 상태 업데이트 ItemProcessor 구성");

        // 만약 Processor를 여러 단계로 나누고 싶다면 CompositeItemProcessorBuilder 사용도 가능
        return orderShipment -> {
            String trackingNumber = orderShipment.getTrackingNumber();
            Long shipmentId = orderShipment.getId();

            if (trackingNumber == null || trackingNumber.isBlank()) {
                log.warn("유효하지 않은 운송장 번호: shipmentId={}", shipmentId);
                throw new ShipmentException(ShipmentErrorCode.TRACKING_NUMBER_INVALID);
            }

            log.info("배송 상태 조회 시작: shipmentId={}, trackingNumber={}, currentStatus={}",
                    shipmentId, trackingNumber, orderShipment.getStatus());

            try {
                // 2-1) 현재 상태 가져오기
                String currentStatus = cjTrackingPlaywright.getCurrentTrackingStatus(trackingNumber);
                log.debug("배송 상태 조회 결과: shipmentId={}, currentStatus={}", shipmentId, currentStatus);

                // 2-2) 매핑
                ShipmentStatus newStatus = mapToShipmentStatus(currentStatus);
                log.debug("배송 상태 매핑 결과: currentStatus={}, mappedStatus={}", currentStatus, newStatus);

                // 현재 상태와 동일한 경우 중복 처리 방지
                if (orderShipment.getStatus() == newStatus) {
                    log.debug("상태 변경 없음 (중복 처리 방지): shipmentId={}, status={}",
                            shipmentId, newStatus);
                    return orderShipment;
                }

                // 2-3) 상태에 따른 처리
                if (newStatus == ShipmentStatus.DELIVERED) {
                    log.info("배송 완료 처리: shipmentId={}, orderId={}",
                            shipmentId, orderShipment.getOrder().getId());

                    orderShipment.updateStatus(ShipmentStatus.DELIVERED);
                    // Order 상태도 COMPLETED 로 변경
                    orderService.completeOrder(orderShipment.getOrder().getId());
                    // 알림 발송
                    notificationService.notifyShipmentCompleted(orderShipment.getOrder());
                }
                else if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
                    log.info("배송 출발 처리: shipmentId={}", shipmentId);
                    orderShipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY);
                    // 필요시 알림, 로직 추가
                }
                else {
                    log.info("배송 중 상태 업데이트: shipmentId={}", shipmentId);
                    orderShipment.updateStatus(ShipmentStatus.IN_TRANSIT);
                }

                log.info("배송 상태 처리 완료: shipmentId={}, newStatus={}", shipmentId, orderShipment.getStatus());
                return orderShipment; // Writer로 넘김

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
        };
    }

    /**
     * CJ대한통운 배송 상태 조회를 위한 메서드
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 상태 텍스트
     * @throws Exception 조회 실패 시 발생
     */
    private String parseCjLogisticsStatus(String trackingNumber) throws Exception {
        log.debug("운송장 조회 시작 (Jsoup): trackingNumber={}", trackingNumber);

        try {
            // 예시: 로직 단순화
            String url = "https://trace.cjlogistics.com/next/tracking.html?wblNo=" + trackingNumber;
            Document doc = Jsoup.connect(url).get();
            // tbody#statusDetail 의 마지막 tr => 5번째 td
            Elements rows = doc.select("tbody#statusDetail tr");
            if (rows.isEmpty()) {
                log.warn("배송 정보가 없음: trackingNumber={}", trackingNumber);
                throw new ShipmentException(
                        ShipmentErrorCode.TRACKING_HTML_PARSE_ERROR,
                        "배송 정보가 없습니다. trackingNo=" + trackingNumber
                );
            }

            String status = rows.last().select("td").get(4).text();
            log.debug("운송장 조회 완료: trackingNumber={}, status={}", trackingNumber, status);
            return status;

        } catch (Exception e) {
            log.error("운송장 조회 실패: trackingNumber={}, error={}", trackingNumber, e.getMessage(), e);

            if (e instanceof ShipmentException) {
                throw e;
            }

            throw new ShipmentException(
                    ShipmentErrorCode.TRACKING_HTML_PARSE_ERROR,
                    "배송 정보 조회 중 오류 발생: " + e.getMessage(),
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

    /**
     * 3) Writer: DB에 반영 (상태 갱신)
     */
    @Bean
    @StepScope
    public JpaItemWriter<OrderShipment> shipmentJpaItemWriter() {
        log.debug("배송 상태 업데이트 ItemWriter 구성");
        JpaItemWriter<OrderShipment> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    /**
     * SkipListener: 예외 발생 시 로그
     */
    @Bean
    public SkipListener<OrderShipment, OrderShipment> shipmentSkipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInProcess(OrderShipment item, Throwable t) {
                if (item != null) {
                    log.error("[Skip] 배송 상태 처리 스킵: shipmentId={}, trackingNumber={}, status={}, 사유={}",
                            item.getId(),
                            item.getTrackingNumber(),
                            item.getStatus(),
                            t.getMessage(),
                            t);
                } else {
                    log.error("[Skip] 배송 상태 처리 스킵: item=null, 사유={}", t.getMessage(), t);
                }
            }

            @Override
            public void onSkipInRead(Throwable t) {
                log.error("[SkipRead] 배송 정보 읽기 스킵: 사유={}", t.getMessage(), t);
            }

            @Override
            public void onSkipInWrite(OrderShipment item, Throwable t) {
                if (item != null) {
                    log.error("[SkipWrite] 배송 상태 저장 스킵: shipmentId={}, trackingNumber={}, status={}, 사유={}",
                            item.getId(),
                            item.getTrackingNumber(),
                            item.getStatus(),
                            t.getMessage(),
                            t);
                } else {
                    log.error("[SkipWrite] 배송 상태 저장 스킵: item=null, 사유={}", t.getMessage(), t);
                }
            }
        };
    }

    /**
     * PlaywrightBrowserManager 빈 정의
     */
    @Bean
    public PlaywrightBrowserManager playwrightBrowserManager() {
        log.debug("Playwright 브라우저 매니저 빈 생성");
        return new PlaywrightBrowserManager();
    }
}