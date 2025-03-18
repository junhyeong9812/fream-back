# 택배 배송 상태 자동화 시스템

이 프로젝트는 택배사 웹 스크래핑을 통해 배송 상태를 자동으로 확인하고 업데이트하는 시스템을 구현한 것입니다. 특히 CJ대한통운의 배송 추적 페이지에서 JavaScript로 동적 렌더링되는 배송 상태 정보를 정확하게 추출하여 데이터베이스에 반영합니다.

## 목차

- [문제 상황](#문제-상황)
- [해결 방안](#해결-방안)
- [주요 구성 요소](#주요-구성-요소)
- [작동 방식](#작동-방식)
- [성능 최적화](#성능-최적화)
- [이슈 및 극복 과정](#이슈-및-극복-과정)
- [시스템 구성도](#시스템-구성도)

## 문제 상황

1. **송장 정보 자동 조회의 필요성**
    - 판매자가 등록한 상품의 배송 여부를 수동으로 확인하기에는 송장 번호가 너무 많음
    - 택배 사이트에 수동으로 접속하여 배송 상태를 확인하는 것은 비효율적
    - 자동화된 배치 프로세스가 필요

2. **JavaScript 렌더링 문제**
    - CJ대한통운 추적 페이지는 JavaScript를 통해 동적으로 렌더링되는 구조
    - 단순 HTML 파싱(`Jsoup`)만으로는 최종 데이터를 포함하지 않아 배송 상태 테이블을 제대로 가져올 수 없음
    - 정적 HTML 파싱으로는 "네트워크 탭에서 HTML을 봤을 땐 정보가 없고, 실제 브라우저 DOM엔 있는" 상황

3. **대규모 트래픽 및 성능 이슈**
    - 송장 조회 건수가 많을 경우, 항목별(Item별)로 브라우저를 열고 닫는 방식은 성능 저하 초래
    - 대량 처리 시 시간 지연 및 리소스 소모가 매우 커짐

4. **리눅스 환경에서의 호환성**
    - 서버(리눅스) 환경에서 헤드리스 브라우저를 안정적으로 구동해야 함
    - 다양한 웹 자동화 도구(Puppeteer, Selenium 등) 중에서 최적의 솔루션 선택 필요

## 해결 방안

### 1. Spring Batch + Cron 스케줄러 도입

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

- 6시간 간격으로 배송 상태를 자동 업데이트하도록 Cron 스케줄러 설정
- `updateShipmentStatusesJob`을 주기적으로 실행하여 배송 중인 송장만 필터링하여 처리
- `IN_TRANSIT`, `OUT_FOR_DELIVERY` 상태의 송장만 선택적으로 처리하여 효율성 증대

### 2. Playwright를 이용한 동적 렌더링 페이지 파싱

```java
@Configuration
public class CjTrackingPlaywright {
    public String getCurrentTrackingStatus(String trackingNumber) throws Exception {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();

            String url = "https://trace.cjlogistics.com/next/tracking.html?wblNo=" + trackingNumber;
            page.navigate(url);

            // JavaScript 렌더링이 끝날 때까지 대기
            page.waitForSelector("tbody#statusDetail tr");

            // 최종 렌더링된 HTML 추출
            String renderedHtml = page.content();

            // Jsoup으로 파싱
            Document doc = Jsoup.parse(renderedHtml);
            Elements rows = doc.select("tbody#statusDetail tr");
            if (rows.isEmpty()) {
                throw new IllegalStateException("배송 정보가 없습니다.");
            }
            
            Elements cells = rows.last().select("td");
            if (cells.size() < 5) {
                throw new IllegalStateException("배송 상태 정보를 찾을 수 없습니다.");
            }
            return cells.get(4).text();
        }
    }
}
```

- Playwright 헤드리스 모드로 브라우저를 실행하여 JavaScript 렌더링이 완료된 페이지 접근
- `page.waitForSelector("tbody#statusDetail tr")`로 동적 콘텐츠가 로드될 때까지 대기
- 최종 렌더링된 HTML을 추출하여 Jsoup으로 파싱, 배송 상태 정보를 정확히 추출

### 3. 브라우저 리소스 효율화를 위한 StepExecutionListener 구현

```java
@RequiredArgsConstructor
public class BrowserManageStepListener extends StepExecutionListenerSupport {

    private final PlaywrightBrowserManager browserManager;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        // Step 시작할 때 브라우저 실행
        browserManager.openBrowser();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        // Step 끝날 때 브라우저 종료
        browserManager.closeBrowser();
        return stepExecution.getExitStatus();
    }
}
```

- `beforeStep()`: 배치 Step 시작 시점에 Playwright 브라우저를 1번만 open
- `afterStep()`: 배치 Step이 완전히 끝난 후에 브라우저를 close
- 매번 브라우저를 새로 열고 닫는 대신 1회 오픈, N회 페이지 탐색 방식으로 리소스 효율 증대

### 4. 배송 상태 매핑 및 자동 알림 처리

```java
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

    // (5) 상태 파싱
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
```

- 파싱된 상태(`배송완료`, `배송출발` 등)를 `ShipmentStatus` 열거형으로 매핑
- 배송 완료일 경우 `OrderShipment`와 `Order` 상태를 업데이트하고 구매자에게 알림 자동 전송
- 단일 브라우저 인스턴스 내에서 탭만 열고 닫는 방식으로 리소스 효율화

## 주요 구성 요소

### PlaywrightBrowserManager

```java
public class PlaywrightBrowserManager {
    private Playwright playwright;
    private Browser browser;

    public void openBrowser() {
        if (playwright == null) {
            this.playwright = Playwright.create();
        }
        if (browser == null) {
            this.browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
        }
    }

    public Page newPage() {
        if (browser == null) {
            throw new IllegalStateException("Browser is not opened. Call openBrowser() first.");
        }
        return browser.newPage();
    }

    public void closeBrowser() {
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
    }
}
```

- 브라우저 리소스 관리를 위한 유틸리티 클래스
- 단일 브라우저 인스턴스를 유지하며 여러 페이지를 효율적으로 생성/파기
- 배치 작업 전체에서 하나의 브라우저 인스턴스만 사용하여 리소스 효율화

### ShipmentItemProcessor

```java
public class ShipmentItemProcessor implements ItemProcessor<OrderShipment, OrderShipment> {

    private final PlaywrightBrowserManager browserManager;
    private final NotificationCommandService notificationService;
    private final OrderShipmentCommandService orderService;

    // 생성자 생략...

    @Override
    public OrderShipment process(OrderShipment orderShipment) throws Exception {
        String trackingNumber = orderShipment.getTrackingNumber();

        // 열린 브라우저에서 새 페이지 생성
        Page page = browserManager.newPage();

        // ... 페이지 처리 로직 ...

        return orderShipment;
    }

    // 기타 메서드 생략...
}
```

- Spring Batch의 `ItemProcessor` 구현체로, 각 송장 항목 처리
- 주어진 브라우저 인스턴스에서 페이지만 새로 열어 리소스 효율화
- 상태 변경 시 주문 완료 처리 및 알림 등 후속 처리 연계

### UpdateShipmentStatusesJobConfig

```java
@Configuration
@RequiredArgsConstructor
public class UpdateShipmentStatusesJobConfig {

    // 의존성 주입 생략...

    @Bean
    public Job updateShipmentStatusesJob() {
        return new JobBuilder("updateShipmentStatusesJob", jobRepository)
                .start(updateShipmentStatusesStep())
                .build();
    }

    @Bean
    public Step updateShipmentStatusesStep() {
        return new StepBuilder("updateShipmentStatusesStep", jobRepository)
                .<OrderShipment, OrderShipment>chunk(50, transactionManager)
                .reader(shipmentItemReader())
                .processor(new ShipmentItemProcessor(
                        playwrightBrowserManager(), 
                        notificationService,
                        orderService
                ))
                .writer(shipmentJpaItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(50)
                .listener(shipmentSkipListener())
                .listener(new BrowserManageStepListener(playwrightBrowserManager()))
                .build();
    }

    // 기타 빈 메서드 생략...
}
```

- Spring Batch Job 설정 클래스
- 50개 단위로 청크 처리하며 예외 발생 시 건너뛰는 내결함성 적용
- `BrowserManageStepListener`를 통해 브라우저 리소스 관리

## 작동 방식

1. **스케줄러 동작**
    - 6시간마다 `updateShipmentStatusesJob` 실행
    - 현재 시간을 파라미터로 전달하여 중복 실행 방지

2. **배송 중 상태 송장만 조회**
    - `IN_TRANSIT`, `OUT_FOR_DELIVERY` 상태의 `OrderShipment` 엔티티만 조회
    - 50개 단위로 청크 처리하여 메모리 효율화

3. **브라우저 인스턴스 관리**
    - Step 시작 시 단일 브라우저 인스턴스 생성
    - 각 송장마다 페이지만 열고 닫음
    - Step 종료 시 브라우저 인스턴스 종료

4. **배송 상태 파싱 및 처리**
    - 각 송장별로 CJ대한통운 추적 페이지 접속
    - 동적 렌더링 대기 후 최종 HTML 추출
    - 배송 상태에 따라 DB 업데이트 및 알림 발송

5. **오류 처리**
    - 예외 발생 시 해당 항목만 스킵하고 계속 진행
    - 로그를 통한 오류 추적 및 모니터링

## 성능 최적화

1. **브라우저 인스턴스 재사용**
    - 각 송장마다 브라우저를 열고 닫는 대신, 하나의 브라우저에서 페이지만 열고 닫음
    - CPU 및 메모리 사용량 대폭 감소
    - 처리 시간 단축

2. **청크 단위 처리**
    - 50개 단위로 청크 처리하여 메모리 효율화
    - 트랜잭션 최적화

3. **필터링된 조회**
    - 이미 배송 완료된 송장은 조회하지 않음
    - 처리가 필요한 상태의 송장만 선택적으로 조회

4. **Playwright의 헤드리스 모드**
    - GUI 없이 백그라운드에서 동작하여 리소스 효율화
    - 리눅스 서버 환경에 최적화

## 이슈 및 극복 과정

1. **Jsoup 단독으로는 동적 콘텐츠 파싱 불가**
    - **문제**: JavaScript로 동적 생성되는 DOM 요소를 Jsoup만으로는 가져올 수 없음
    - **해결**: Playwright를 도입하여 실제 렌더링된 DOM을 파싱

2. **브라우저 리소스 관리 최적화**
    - **문제**: 각 송장마다 브라우저 생성/종료 시 성능 저하 및 리소스 소모 증가
    - **해결**: StepExecutionListener를 사용하여 브라우저 인스턴스 재사용 구조 적용

3. **리눅스 환경에서의 안정성**
    - **문제**: 서버(리눅스) 환경에서 헤드리스 브라우저의 안정적 구동 필요
    - **해결**: Playwright 선택으로 크로스 플랫폼 호환성 확보 및 설치/설정 간소화

4. **예외 처리 및 내결함성**
    - **문제**: 일부 송장 처리 중 에러 발생 시 전체 배치 실패 가능성
    - **해결**: `faultTolerant()` 및 `skip()` 메서드로 개별 에러 처리 및 배치 계속 진행

## 시스템 구성도

```
┌────────────────────┐      ┌─────────────────────────┐      ┌───────────────────┐
│  스케줄러 (Cron)   │─────▶│    Spring Batch Job     │─────▶│ 브라우저 매니저   │
└────────────────────┘      └─────────────────────────┘      └─────────┬─────────┘
                                       │                               │
                                       ▼                               ▼
┌────────────────────┐      ┌─────────────────────────┐      ┌───────────────────┐
│  알림 서비스       │◀─────│ ItemProcessor (50건)    │◀─────│ Playwright 브라우저│
└────────────────────┘      └─────────────────────────┘      └───────────────────┘
                                       │
                                       ▼
┌────────────────────┐      ┌─────────────────────────┐      ┌───────────────────┐
│  주문 완료 처리    │◀─────│       JPA Writer        │─────▶│      Database     │
└────────────────────┘      └─────────────────────────┘      └───────────────────┘
```

## 결과 및 효과

1. **자동화된 송장 추적 및 시간 절감**
    - 수동 확인이 불필요해져 운영 효율성 증대
    - 배송 상태를 실시간으로 파악 가능

2. **정확한 데이터 획득**
    - 동적 렌더링된 페이지에서 정확한 배송 정보 추출
    - 사용자에게 정확한 배송 상태 제공

3. **성능 및 리소스 효율 개선**
    - 브라우저 호출 횟수 최소화로 대량 처리 속도 향상
    - 메모리 및 CPU 사용량 최적화

4. **안정적인 서버 환경 운영**
    - 리눅스 서버에서 안정적으로 동작
    - Docker 컨테이너 환경 지원으로 CI/CD 파이프라인 연동 용이

---

## 사용된 기술 스택

- **Spring Boot**: 애플리케이션 프레임워크
- **Spring Batch**: 배치 작업 관리
- **Playwright**: 웹 브라우저 자동화
- **Jsoup**: HTML 파싱
- **Spring Data JPA**: 데이터베이스 연동
- **Cron Scheduler**: 주기적 작업 실행

## 개선 가능성

- 멀티스레드 처리를 통한 성능 향상
- 실패한 항목 재시도 메커니즘 강화
- 모니터링 및 알림 시스템 연동

---

*이 문서는 택배 배송 상태 자동화 시스템의 구현 및 운영에 관한 기술 문서입니다. 필요에 따라 업데이트될 수 있습니다.*