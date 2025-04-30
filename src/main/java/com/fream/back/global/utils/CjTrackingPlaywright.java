package com.fream.back.global.utils;

import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Configuration;

/**
 * CJ대한통운 배송 조회를 위한 Playwright 유틸리티 클래스
 */
@Slf4j
@Configuration
public class CjTrackingPlaywright {

    private static final String TRACKING_URL_PREFIX = "https://trace.cjlogistics.com/next/tracking.html?wblNo=";
    private static final String STATUS_SELECTOR = "tbody#statusDetail tr";
    private static final int SELECTOR_TIMEOUT_MS = 30000; // 30초

    /**
     * 운송장 번호로 현재 배송 상태를 조회합니다.
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 상태 텍스트 (예: "배송완료", "배송출발")
     * @throws ShipmentException 배송 조회 실패 시 발생
     */
    public String getCurrentTrackingStatus(String trackingNumber) throws Exception {
        if (trackingNumber == null || trackingNumber.isBlank()) {
            log.warn("유효하지 않은 운송장 번호: {}", trackingNumber);
            throw new ShipmentException(ShipmentErrorCode.TRACKING_NUMBER_INVALID);
        }

        log.info("CJ대한통운 배송 상태 조회 시작: 운송장 번호 = {}", trackingNumber);

        Playwright playwright = null;
        Browser browser = null;
        Page page = null;

        try {
            // 1) Playwright 객체 생성
            log.debug("Playwright 인스턴스 생성");
            playwright = Playwright.create();

            // 2) Chromium 브라우저를 headless 모드로 실행
            log.debug("Chromium 브라우저 시작 (headless 모드)");
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(true)
                            .setSlowMo(100)
            );

            page = browser.newPage();

            // 3) 해당 URL로 이동
            String url = TRACKING_URL_PREFIX + trackingNumber;
            log.debug("배송 조회 페이지로 이동: {}", url);
            page.navigate(url);

            // 4) 자바스크립트로 tbody#statusDetail이 채워질 때까지 대기
            log.debug("배송 상태 테이블 로드 대기 (최대 {}ms)", SELECTOR_TIMEOUT_MS);
            page.setDefaultTimeout(SELECTOR_TIMEOUT_MS);
            page.waitForSelector(STATUS_SELECTOR);

            // 5) 최종 렌더링된 HTML 문자열 추출
            log.debug("페이지 HTML 로드");
            String renderedHtml = page.content();

            // 6) Jsoup으로 파싱
            log.debug("HTML 파싱 시작");
            String statusText = parseStatusFromHtml(renderedHtml, trackingNumber);
            log.info("배송 상태 파싱 완료: 운송장={}, 상태={}", trackingNumber, statusText);

            return statusText;

        } catch (Exception e) {
            log.error("배송 상태 조회 실패: 운송장={}, 오류={}", trackingNumber, e.getMessage(), e);

            if (e instanceof ShipmentException) {
                throw e;
            }

            throw new ShipmentException(
                    ShipmentErrorCode.EXTERNAL_TRACKING_SERVICE_ERROR,
                    "CJ대한통운 배송 조회 중 오류 발생: " + e.getMessage(),
                    e
            );
        } finally {
            // 리소스 해제
            log.debug("리소스 해제 시작");

            try {
                if (page != null) {
                    page.close();
                }

                if (browser != null) {
                    browser.close();
                }

                if (playwright != null) {
                    playwright.close();
                }

                log.debug("리소스 해제 완료");
            } catch (Exception e) {
                log.warn("리소스 해제 중 오류 발생: {}", e.getMessage());
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
}