package com.fream.back.domain.shipment.config;

import com.fream.back.global.utils.PlaywrightBrowserManager;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;

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
