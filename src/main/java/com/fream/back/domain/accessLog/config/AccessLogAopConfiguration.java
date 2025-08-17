package com.fream.back.domain.accessLog.config;

import com.fream.back.domain.accessLog.aop.aspect.AccessLogExceptionAspect;
import com.fream.back.domain.accessLog.aop.aspect.AccessLogMethodLoggingAspect;
import com.fream.back.domain.accessLog.aop.aspect.AccessLogPerformanceAspect;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy; // Jakarta EE로 변경됨 (javax → jakarta)
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * AccessLog 도메인의 AOP 설정을 통합 관리하는 Configuration 클래스
 *
 * 주요 기능:
 * - AspectJ AOP 활성화
 * - Cache 설정 (성능 최적화용)
 * - AOP Aspect들의 통계 정보 주기적 출력
 * - AOP 관련 빈 설정
 *
 * 이 클래스는 AccessLog 도메인의 모든 AOP 기능을 활성화하고
 * 관련 설정을 중앙 집중화하여 관리합니다.
 *
 * 참고: Spring Boot 3.x부터 Jakarta EE 사용 (javax → jakarta)
 */
@Configuration // Spring Configuration 클래스 선언
@EnableAspectJAutoProxy( // AspectJ 기반 AOP 활성화
        proxyTargetClass = true, // CGLIB 프록시 사용 (인터페이스가 없어도 프록시 생성 가능)
        exposeProxy = true // AopContext.currentProxy()로 현재 프록시 접근 가능
)
@EnableScheduling // 스케줄링 기능 활성화 (주기적 통계 출력용)
@RequiredArgsConstructor // final 필드 생성자 자동 생성
@Slf4j // 로거 자동 생성
public class AccessLogAopConfiguration {

    // AOP Aspect들 주입 (통계 정보 수집용)
    private final AccessLogExceptionAspect exceptionAspect;
    private final AccessLogMethodLoggingAspect loggingAspect;
    private final AccessLogPerformanceAspect performanceAspect;

    /**
     * 설정 초기화 완료 후 실행
     * AOP 설정이 정상적으로 활성화되었는지 확인
     *
     * @PostConstruct: 빈 생성 및 의존성 주입 완료 후 실행되는 메서드
     * Jakarta EE 어노테이션 사용 (jakarta.annotation.PostConstruct)
     */
    @PostConstruct
    public void initializeAopConfiguration() {
        log.info("=== AccessLog AOP Configuration 초기화 완료 ===");
        log.info("✅ AspectJ AutoProxy: 활성화");
        log.info("✅ Exception Handling Aspect: 활성화 (Order 1)");
        log.info("✅ Method Logging Aspect: 활성화 (Order 2)");
        log.info("✅ Performance Monitoring Aspect: 활성화 (Order 3)");
        log.info("✅ Cache Manager: 활성화");
        log.info("✅ 통계 스케줄러: 활성화 (매 10분마다 실행)");
        log.info("✅ Jakarta EE 지원: jakarta.annotation 패키지 사용");
        log.info("================================================");
    }

    /**
     * Cache Manager 설정
     * AccessLog 도메인의 조회 성능 최적화를 위한 캐시 설정
     *
     * @return CacheManager 인스턴스
     */
    @Bean
    public CacheManager accessLogCacheManager() {
        // ConcurrentMapCacheManager: 메모리 기반 간단한 캐시 구현체
        // 운영 환경에서는 Redis나 Hazelcast 등의 분산 캐시 고려
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
                // GeoIP 관련 캐시
                "geoIpCache",           // IP 위치 정보 캐시

                // 통계 조회 캐시 (UserAccessLogQueryService용)
                "todayAccessCount",     // 오늘 접속자 수
                "todayUniqueVisitors",  // 오늘 고유 방문자 수
                "recent7DaysStats",     // 최근 7일 통계
                "countryStats",         // 국가별 통계
                "popularPages",         // 인기 페이지 통계
                "userTypeStats",        // 사용자 유형별 통계
                "hourlyStats",          // 시간대별 통계
                "deviceStats",          // 디바이스별 통계
                "browserStats",         // 브라우저별 통계

                // 성능 모니터링 캐시
                "performanceStats",     // 성능 통계 정보
                "methodStats"           // 메서드별 성능 통계
        );

        // 캐시 설정 커스터마이징
        cacheManager.setAllowNullValues(false); // null 값 캐싱 방지

        log.info("AccessLog Cache Manager 설정 완료: {} 개 캐시 정의",
                cacheManager.getCacheNames().size());

        return cacheManager;
    }

    /**
     * AOP 통계 정보 수집 및 출력
     * 매 10분마다 실행되어 AOP 동작 현황을 모니터링
     *
     * fixedRate: 지정된 간격으로 실행 (이전 실행 완료 여부와 무관)
     * initialDelay: 애플리케이션 시작 후 첫 실행까지의 지연 시간
     */
    @Scheduled(fixedRate = 600000, initialDelay = 60000) // 10분마다 실행, 1분 후 시작
    public void logAopStatistics() {
        try {
            log.info("📊 === AccessLog AOP 통계 정보 ===");

            // 1. 예외 처리 통계
            AccessLogExceptionAspect.ExceptionStats exceptionStats = exceptionAspect.getExceptionStats();
            log.info("🛡️ [예외 처리] {}", exceptionStats.toString());

            // 2. 메서드 로깅 통계
            AccessLogMethodLoggingAspect.LoggingStats loggingStats = loggingAspect.getLoggingStats();
            log.info("📝 [메서드 로깅] {}", loggingStats.toString());

            // 3. 성능 모니터링 통계
            AccessLogPerformanceAspect.PerformanceStats performanceStats = performanceAspect.getPerformanceStats();
            log.info("⚡ [성능 모니터링] {}", performanceStats.toString());

            // 4. 전반적인 건강 상태 평가
            evaluateAopHealth(exceptionStats, loggingStats, performanceStats);

            log.info("=====================================");

        } catch (Exception e) {
            log.error("AOP 통계 정보 수집 중 오류 발생", e);
        }
    }

    /**
     * AOP 시스템의 전반적인 건강 상태 평가
     *
     * @param exceptionStats 예외 처리 통계
     * @param loggingStats 로깅 통계
     * @param performanceStats 성능 통계
     */
    private void evaluateAopHealth(AccessLogExceptionAspect.ExceptionStats exceptionStats,
                                   AccessLogMethodLoggingAspect.LoggingStats loggingStats,
                                   AccessLogPerformanceAspect.PerformanceStats performanceStats) {

        StringBuilder healthReport = new StringBuilder();
        healthReport.append("🏥 [AOP 건강 상태] ");

        boolean hasIssues = false;

        // 1. 예외 발생률 체크 (30% 이상이면 경고)
        if (loggingStats.getExceptionRate() > 30.0) {
            healthReport.append("⚠️ 높은 예외 발생률 (").append(String.format("%.1f", loggingStats.getExceptionRate())).append("%) ");
            hasIssues = true;
        }

        // 2. 성능 임계값 초과율 체크 (20% 이상이면 경고)
        if (performanceStats.getThresholdExceededRate() > 20.0) {
            healthReport.append("⚠️ 높은 성능 임계값 초과율 (").append(String.format("%.1f", performanceStats.getThresholdExceededRate())).append("%) ");
            hasIssues = true;
        }

        // 3. 슬로우 쿼리 발생률 체크 (5% 이상이면 경고)
        if (performanceStats.getSlowQueryRate() > 5.0) {
            healthReport.append("⚠️ 높은 슬로우 쿼리 발생률 (").append(String.format("%.1f", performanceStats.getSlowQueryRate())).append("%) ");
            hasIssues = true;
        }

        // 4. 재시도율 체크 (10% 이상이면 경고)
        if (exceptionStats.getRetryRate() > 10.0) {
            healthReport.append("⚠️ 높은 재시도율 (").append(String.format("%.1f", exceptionStats.getRetryRate())).append("%) ");
            hasIssues = true;
        }

        // 건강 상태 결론
        if (!hasIssues) {
            healthReport.append("✅ 양호");
            log.info(healthReport.toString());
        } else {
            healthReport.append("| 조치 필요");
            log.warn(healthReport.toString());

            // 개선 제안 로깅
            logImprovementSuggestions(exceptionStats, loggingStats, performanceStats);
        }
    }

    /**
     * AOP 성능 개선 제안 로깅
     *
     * @param exceptionStats 예외 처리 통계
     * @param loggingStats 로깅 통계
     * @param performanceStats 성능 통계
     */
    private void logImprovementSuggestions(AccessLogExceptionAspect.ExceptionStats exceptionStats,
                                           AccessLogMethodLoggingAspect.LoggingStats loggingStats,
                                           AccessLogPerformanceAspect.PerformanceStats performanceStats) {

        log.warn("💡 === AOP 성능 개선 제안 ===");

        // 예외 발생률이 높은 경우
        if (loggingStats.getExceptionRate() > 30.0) {
            log.warn("1. 예외 발생률 개선 방안:");
            log.warn("   - 입력 데이터 검증 로직 강화");
            log.warn("   - 외부 시스템 연동 시 Circuit Breaker 패턴 적용");
            log.warn("   - 데이터베이스 연결 풀 설정 최적화");
        }

        // 성능 임계값 초과가 많은 경우
        if (performanceStats.getThresholdExceededRate() > 20.0) {
            log.warn("2. 성능 개선 방안:");
            log.warn("   - 데이터베이스 쿼리 최적화 (인덱스 추가, 쿼리 튜닝)");
            log.warn("   - 캐시 적용 범위 확대");
            log.warn("   - 비동기 처리 고려 (Kafka, 배치 처리)");
        }

        // 슬로우 쿼리가 많은 경우
        if (performanceStats.getSlowQueryRate() > 5.0) {
            log.warn("3. 슬로우 쿼리 개선 방안:");
            log.warn("   - 복잡한 조회 로직을 배치 작업으로 분리");
            log.warn("   - 읽기 전용 복제본 데이터베이스 활용");
            log.warn("   - 집계 데이터를 미리 계산하여 저장");
        }

        // 재시도가 많은 경우
        if (exceptionStats.getRetryRate() > 10.0) {
            log.warn("4. 재시도 최적화 방안:");
            log.warn("   - 외부 시스템의 안정성 점검");
            log.warn("   - 재시도 정책 조정 (백오프 시간, 최대 재시도 횟수)");
            log.warn("   - 대체 처리 로직 구현 (Fallback 메커니즘)");
        }

        log.warn("============================");
    }

    /**
     * 애플리케이션 종료 시 AOP 통계 최종 리포트 출력
     *
     * @PreDestroy: 빈 소멸 직전에 실행되는 메서드
     * Jakarta EE 어노테이션 사용 (jakarta.annotation.PreDestroy)
     * 정상 종료 시에만 실행되어 최종 통계를 기록
     */
    @PreDestroy // Jakarta EE로 변경됨 (javax.annotation.PreDestroy → jakarta.annotation.PreDestroy)
    public void generateFinalAopReport() {
        log.info("🏁 === AccessLog AOP 최종 리포트 ===");

        try {
            // 최종 통계 정보 출력
            AccessLogExceptionAspect.ExceptionStats exceptionStats = exceptionAspect.getExceptionStats();
            AccessLogMethodLoggingAspect.LoggingStats loggingStats = loggingAspect.getLoggingStats();
            AccessLogPerformanceAspect.PerformanceStats performanceStats = performanceAspect.getPerformanceStats();

            log.info("총 메서드 호출: {} 회", loggingStats.getTotalMethodCalls());
            log.info("총 예외 발생: {} 회 (발생률: {:.2f}%)",
                    loggingStats.getTotalExceptions(), loggingStats.getExceptionRate());
            log.info("총 성능 체크: {} 회", performanceStats.getTotalChecks());
            log.info("임계값 초과: {} 회 (초과율: {:.2f}%)",
                    performanceStats.getThresholdExceeded(), performanceStats.getThresholdExceededRate());
            log.info("슬로우 쿼리: {} 회 (발생률: {:.2f}%)",
                    performanceStats.getSlowQueries(), performanceStats.getSlowQueryRate());
            log.info("재시도 횟수: {} 회 (재시도율: {:.2f}%)",
                    exceptionStats.getRetriedExceptions(), exceptionStats.getRetryRate());

            log.info("================================");
            log.info("AccessLog AOP 시스템이 정상적으로 종료되었습니다.");

        } catch (Exception e) {
            log.error("최종 AOP 리포트 생성 중 오류 발생", e);
        }
    }

    /**
     * AOP 설정 유효성 검증
     * 개발 환경에서 AOP가 제대로 동작하는지 확인용
     *
     * @return 검증 결과
     */
    public boolean validateAopConfiguration() {
        try {
            // Aspect 빈들이 정상적으로 주입되었는지 확인
            boolean isValid = exceptionAspect != null &&
                    loggingAspect != null &&
                    performanceAspect != null;

            if (isValid) {
                log.debug("AOP 설정 검증 성공: 모든 Aspect가 정상적으로 등록됨");
            } else {
                log.error("AOP 설정 검증 실패: 일부 Aspect가 누락됨");
            }

            return isValid;

        } catch (Exception e) {
            log.error("AOP 설정 검증 중 오류 발생", e);
            return false;
        }
    }
}