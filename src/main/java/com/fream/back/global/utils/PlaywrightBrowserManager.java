package com.fream.back.global.utils;

import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Playwright 브라우저 생성 및 관리 유틸리티
 * 싱글톤으로 브라우저 인스턴스를 관리합니다.
 */
@Slf4j
@Component
public class PlaywrightBrowserManager {
    private Playwright playwright;
    private Browser browser;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static final BrowserType.LaunchOptions LAUNCH_OPTIONS = new BrowserType.LaunchOptions()
            .setHeadless(true)
            .setSlowMo(100);

    /**
     * 브라우저를 열고 초기화합니다.
     * 이미 열려있는 경우, 기존 브라우저를 재사용합니다.
     *
     * @throws ShipmentException 브라우저 초기화 실패 시 발생
     */
    public synchronized void openBrowser() {
        if (initialized.get() && browser != null && playwright != null) {
            log.debug("브라우저가 이미 열려 있습니다. 재사용합니다.");
            return;
        }

        try {
            log.info("Playwright 브라우저 초기화 시작");

            if (playwright == null) {
                log.debug("Playwright 엔진 생성");
                playwright = Playwright.create();
            }

            if (browser == null) {
                log.debug("Chromium 브라우저 시작 (headless={}, slowMo={}ms)",
                        LAUNCH_OPTIONS.headless, LAUNCH_OPTIONS.slowMo);
                browser = playwright.chromium().launch(LAUNCH_OPTIONS);
            }

            initialized.set(true);
            log.info("Playwright 브라우저 초기화 완료");

        } catch (Exception e) {
            log.error("Playwright 브라우저 초기화 실패: {}", e.getMessage(), e);
            closeBrowser(); // 실패한 경우 리소스 정리

            throw new ShipmentException(
                    ShipmentErrorCode.BROWSER_INITIALIZATION_ERROR,
                    "브라우저 초기화에 실패했습니다: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 브라우저를 닫고 리소스를 해제합니다.
     */
    public synchronized void closeBrowser() {
        log.info("Playwright 브라우저 리소스 해제 시작");

        if (browser != null) {
            try {
                log.debug("브라우저 닫기 시작");
                browser.close(); // 모든 Page도 함께 닫힘
                log.debug("브라우저 닫기 완료");
            } catch (Exception e) {
                log.warn("브라우저 닫기 실패: {}", e.getMessage());
            } finally {
                browser = null;
            }
        }

        if (playwright != null) {
            try {
                log.debug("Playwright 엔진 닫기 시작");
                playwright.close();
                log.debug("Playwright 엔진 닫기 완료");
            } catch (Exception e) {
                log.warn("Playwright 엔진 닫기 실패: {}", e.getMessage());
            } finally {
                playwright = null;
            }
        }

        initialized.set(false);
        log.info("Playwright 브라우저 리소스 해제 완료");
    }

    /**
     * 새 페이지를 생성합니다.
     * 브라우저가 초기화되지 않은 경우 자동으로 초기화합니다.
     *
     * @return 새로 생성된 페이지
     * @throws ShipmentException 페이지 생성 실패 시 발생
     */
    public Page newPage() {
        if (!initialized.get() || browser == null) {
            log.info("브라우저가 초기화되지 않았습니다. 자동으로 초기화합니다.");
            openBrowser();
        }

        try {
            log.debug("새 페이지 생성");
            return browser.newPage();
        } catch (Exception e) {
            log.error("새 페이지 생성 실패: {}", e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.BROWSER_INITIALIZATION_ERROR,
                    "새 페이지를 생성할 수 없습니다: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 현재 브라우저 인스턴스가 유효한지 확인합니다.
     *
     * @return 브라우저가 초기화되었고 사용 가능한 경우 true
     */
    public boolean isInitialized() {
        return initialized.get() && browser != null && playwright != null;
    }
}