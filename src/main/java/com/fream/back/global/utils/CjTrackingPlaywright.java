package com.fream.back.global.utils;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CjTrackingPlaywright {
    public String getCurrentTrackingStatus(String trackingNumber) throws Exception {

        // 1) Playwright 객체 생성
        try (Playwright playwright = Playwright.create()) {
            // 2) Chromium 브라우저를 headless 모드로 실행
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );
            Page page = browser.newPage();

            // 3) 해당 URL로 이동
            String url = "https://trace.cjlogistics.com/next/tracking.html?wblNo=" + trackingNumber;
            page.navigate(url);

            // 4) 자바스크립트로 tbody#statusDetail이 채워질 때까지 대기
            page.waitForSelector("tbody#statusDetail tr");

            // 5) 최종 렌더링된 HTML 문자열 추출
            String renderedHtml = page.content();

            // 6) Jsoup으로 파싱
            Document doc = Jsoup.parse(renderedHtml);

            // 7) Jsoup select 로 배송 상태 td 찾기
            Elements rows = doc.select("tbody#statusDetail tr");
            if (rows.isEmpty()) {
                throw new IllegalStateException("배송 정보가 없습니다.");
            }
            // 마지막 row
            Elements cells = rows.last().select("td");
            if (cells.size() < 5) {
                throw new IllegalStateException("배송 상태 정보를 찾을 수 없습니다.");
            }
            return cells.get(4).text();
        }
    }
}