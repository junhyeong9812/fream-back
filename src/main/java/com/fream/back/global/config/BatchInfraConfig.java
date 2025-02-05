package com.fream.back.global.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


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

        // ★ 현재 H2 인메모리로 사용
//        factory.setDatabaseType(DatabaseType.H2.name());
         factory.setDatabaseType(DatabaseType.MYSQL.name()); // <-- 추후 MySQL로 전환 시 활성화

        // factory.setTablePrefix("BATCH_"); // 테이블 접두어 설정 가능
        factory.afterPropertiesSet();
        return factory.getObject();
    }

}
