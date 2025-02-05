package com.fream.back.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipmentBatchScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("updateShipmentStatusesJob")
    @Autowired
    private Job updateShipmentStatusesJob;

//    private final Job updateShipmentStatusesJob;
//
//    // Write the constructor manually:
//    public ShipmentBatchScheduler(
//            JobLauncher jobLauncher,
//            @Qualifier("updateShipmentStatusesJob") Job updateShipmentStatusesJob
//    ) {
//        this.jobLauncher = jobLauncher;
//        this.updateShipmentStatusesJob = updateShipmentStatusesJob;
//    }

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

