# 배치 처리 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 배치 처리 관련 설정을 포함합니다.

## BatchInfraConfig

Spring Batch 인프라 설정을 담당하는 클래스입니다.

```java
@Configuration
@EnableBatchProcessing
public class BatchInfraConfig {
    @Bean
    public JobRepository jobRepository(
            DataSource dataSource,
            PlatformTransactionManager transactionManager
    ) throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.setDatabaseType(DatabaseType.MYSQL.name());
        factory.afterPropertiesSet();
        return factory.getObject();
    }
}
```

### 주요 기능

- **배치 작업 저장소**: 배치 작업 메타데이터를 저장할 리포지토리를 설정합니다.
- **데이터베이스 설정**: 배치 작업에 사용할 데이터베이스 타입을 설정합니다 (MYSQL).
- **@EnableBatchProcessing**: Spring Batch 기능을 활성화합니다.

## ShipmentBatchScheduler

배송 상태 업데이트 배치 작업을 스케줄링하는 클래스입니다.

```java
@Component
@RequiredArgsConstructor
public class ShipmentBatchScheduler {
    private final JobLauncher jobLauncher;

    @Qualifier("updateShipmentStatusesJob")
    @Autowired
    private Job updateShipmentStatusesJob;

    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduleShipmentStatusJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            jobLauncher.run(updateShipmentStatusesJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 주요 기능

- **정기적인 작업 실행**: cron 표현식을 사용하여 6시간마다 배치 작업을 실행합니다.
- **작업 파라미터**: 배치 작업 실행 시 필요한 파라미터(timestamp)를 설정합니다.

## ApplicationShutdownConfig

애플리케이션 종료 시 실행될 작업을 설정하는 클래스입니다.

```java
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApplicationShutdownConfig {
    private final StyleViewLogBufferManager bufferManager;

    @Bean
    public ApplicationListener<ContextClosedEvent> shutdownHook() {
        return event -> {
            log.info("Application shutdown hook triggered");
            bufferManager.flushBufferOnShutdown();
            log.info("Application shutdown tasks completed");
        };
    }
}
```

### 주요 기능

- **종료 이벤트 감지**: 애플리케이션 컨텍스트가 종료될 때 이벤트를 감지합니다.
- **버퍼 데이터 저장**: 메모리에 있는 로그 데이터를 디스크에 저장합니다.
- **안전한 종료**: 애플리케이션이 안전하게 종료되도록 리소스를 정리합니다.

## 배치 작업 구현 예시

### 배송 상태 업데이트 배치 작업

```java
@Configuration
@RequiredArgsConstructor
public class ShipmentBatchConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final ShipmentStatusService shipmentStatusService;

    @Bean
    public Job updateShipmentStatusesJob() {
        return jobBuilderFactory.get("updateShipmentStatusesJob")
                .incrementer(new RunIdIncrementer())
                .start(updateShipmentStatusesStep())
                .build();
    }

    @Bean
    public Step updateShipmentStatusesStep() {
        return stepBuilderFactory.get("updateShipmentStatusesStep")
                .<Shipment, Shipment>chunk(10)
                .reader(shipmentReader())
                .processor(shipmentProcessor())
                .writer(shipmentWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Shipment> shipmentReader() {
        return new JdbcCursorItemReaderBuilder<Shipment>()
                .name("shipmentReader")
                .dataSource(dataSource)
                .sql("SELECT * FROM shipments WHERE status IN ('READY', 'SHIPPING')")
                .rowMapper(new BeanPropertyRowMapper<>(Shipment.class))
                .build();
    }

    @Bean
    public ItemProcessor<Shipment, Shipment> shipmentProcessor() {
        return shipment -> {
            // 배송 상태 업데이트 로직
            String currentStatus = shipmentStatusService.checkCurrentStatus(shipment.getTrackingNumber());
            shipment.setStatus(currentStatus);
            return shipment;
        };
    }

    @Bean
    public JdbcBatchItemWriter<Shipment> shipmentWriter() {
        return new JdbcBatchItemWriterBuilder<Shipment>()
                .dataSource(dataSource)
                .sql("UPDATE shipments SET status = :status, updated_at = :updatedAt WHERE id = :id")
                .beanMapped()
                .build();
    }
}
```

### 일일 정산 배치 작업

```java
@Configuration
@RequiredArgsConstructor
public class DailySettlementBatchConfig {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;
    private final SellerSettlementService settlementService;

    @Bean
    public Job dailySettlementJob() {
        return jobBuilderFactory.get("dailySettlementJob")
                .incrementer(new RunIdIncrementer())
                .start(processCompletedOrdersStep())
                .next(generateSettlementReportsStep())
                .build();
    }

    @Bean
    public Step processCompletedOrdersStep() {
        return stepBuilderFactory.get("processCompletedOrdersStep")
                .<Order, Settlement>chunk(100)
                .reader(completedOrderReader())
                .processor(orderToSettlementProcessor())
                .writer(settlementWriter())
                .build();
    }

    @Bean
    public Step generateSettlementReportsStep() {
        return stepBuilderFactory.get("generateSettlementReportsStep")
                .tasklet(settlementReportTasklet())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Order> completedOrderReader() {
        return new JpaPagingItemReaderBuilder<Order>()
                .name("completedOrderReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT o FROM Order o WHERE o.status = 'DELIVERED' " +
                             "AND o.isSettled = false")
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<Order, Settlement> orderToSettlementProcessor() {
        return order -> {
            // 주문을 정산 데이터로 변환
            return settlementService.createSettlementFromOrder(order);
        };
    }

    @Bean
    public JpaItemWriter<Settlement> settlementWriter() {
        JpaItemWriter<Settlement> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }

    @Bean
    public Tasklet settlementReportTasklet() {
        return (contribution, chunkContext) -> {
            // 정산 보고서 생성 로직
            settlementService.generateDailyReports();
            return RepeatStatus.FINISHED;
        };
    }
}
```

## 배치 작업 스케줄링 예시

다양한 배치 작업의 스케줄링 설정 예시입니다.

```java
@Configuration
@EnableScheduling
public class BatchSchedulerConfig {
    private final JobLauncher jobLauncher;
    private final Job dailySettlementJob;
    private final Job productInventoryUpdateJob;
    private final Job userPointExpirationJob;
    
    public BatchSchedulerConfig(
            JobLauncher jobLauncher,
            @Qualifier("dailySettlementJob") Job dailySettlementJob,
            @Qualifier("productInventoryUpdateJob") Job productInventoryUpdateJob,
            @Qualifier("userPointExpirationJob") Job userPointExpirationJob) {
        this.jobLauncher = jobLauncher;
        this.dailySettlementJob = dailySettlementJob;
        this.productInventoryUpdateJob = productInventoryUpdateJob;
        this.userPointExpirationJob = userPointExpirationJob;
    }
    
    /**
     * 매일 자정에 실행되는 정산 작업
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailySettlement() {
        runJob(dailySettlementJob, "dailySettlement");
    }
    
    /**
     * 1시간마다 실행되는 상품 재고 업데이트
     */
    @Scheduled(fixedRate = 3600000)
    public void runInventoryUpdate() {
        runJob(productInventoryUpdateJob, "inventoryUpdate");
    }
    
    /**
     * 매월 1일 오전 3시에 실행되는 포인트 만료 처리
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void runPointExpiration() {
        runJob(userPointExpirationJob, "pointExpiration");
    }
    
    /**
     * 배치 작업 실행 헬퍼 메서드
     */
    private void runJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("JobID", UUID.randomUUID().toString())
                    .addDate("currentTime", new Date())
                    .toJobParameters();
            
            JobExecution execution = jobLauncher.run(job, params);
            log.info("{} Job 실행 결과: {}", jobName, execution.getStatus());
        } catch (Exception e) {
            log.error("{} Job 실행 중 오류 발생: {}", jobName, e.getMessage(), e);
        }
    }
}
```

## JobExecutionListener 구현 예시

배치 작업 실행 전후 처리를 위한 리스너 구현 예시입니다.

```java
@Component
@Slf4j
public class ShipmentJobListener implements JobExecutionListener {
    private final EmailService emailService;
    private final UserRepository userRepository;
    
    public ShipmentJobListener(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("배송 상태 업데이트 배치 작업 시작: {}", jobExecution.getJobInstance().getJobName());
        log.info("매개변수: {}", jobExecution.getJobParameters());
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("배송 상태 업데이트 배치 작업 완료: {}", jobExecution.getStatus());
        
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            // 작업 성공 처리
            sendCompletionNotification(jobExecution);
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            // 작업 실패 처리
            sendFailureNotification(jobExecution);
        }
    }
    
    private void sendCompletionNotification(JobExecution jobExecution) {
        // 업데이트된 배송 정보 추출
        int updatedCount = getUpdatedCount(jobExecution);
        
        // 관리자에게 완료 이메일 전송
        String adminEmail = userRepository.findAdminEmail();
        
        if (adminEmail != null) {
            emailService.sendEmail(
                adminEmail,
                "배송 상태 업데이트 완료",
                String.format("배송 상태 업데이트가 완료되었습니다. 총 %d건 처리되었습니다.", updatedCount)
            );
        }
    }
    
    private void sendFailureNotification(JobExecution jobExecution) {
        // 실패 원인 추출
        String failureMessage = getFailureMessage(jobExecution);
        
        // 관리자에게 실패 알림 전송
        String adminEmail = userRepository.findAdminEmail();
        
        if (adminEmail != null) {
            emailService.sendEmail(
                adminEmail,
                "배송 상태 업데이트 실패",
                String.format("배송 상태 업데이트 중 오류가 발생했습니다: %s", failureMessage)
            );
        }
    }
    
    private int getUpdatedCount(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .mapToInt(StepExecution::getWriteCount)
                .sum();
    }
    
    private String getFailureMessage(JobExecution jobExecution) {
        List<Throwable> exceptions = jobExecution.getAllFailureExceptions();
        if (exceptions.isEmpty()) {
            return "알 수 없는 오류";
        }
        
        return exceptions.get(0).getMessage();
    }
}
```
                .