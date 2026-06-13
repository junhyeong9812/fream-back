package com.fream.back;

import com.fream.back.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 안전망 인프라 스모크 테스트.
 *
 * <p>전체 JPA 엔티티 모델이 H2(test 프로파일)에서 스키마를 생성하고
 * Spring Data 레포지토리(QueryDSL 커스텀 구현 포함)가 정상 구성되는지 검증한다.
 * 이게 통과해야 SCC 엔티티 영속성 기반 특성화 테스트를 쌓을 수 있다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class PersistenceSmokeTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void jpaSliceBootsAndSchemaBuilds() {
        assertNotNull(entityManager);
    }
}
