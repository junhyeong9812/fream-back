package com.fream.back.domain.accessLog.service.command;

import com.fream.back.domain.accessLog.aop.annotation.AccessLogExceptionHandler;
import com.fream.back.domain.accessLog.aop.annotation.AccessLogMethodLogger;
import com.fream.back.domain.accessLog.aop.annotation.AccessLogPerformanceMonitor;
import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.entity.UserAccessLog;
import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.InvalidParameterException;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import com.fream.back.domain.accessLog.service.geo.GeoIPService;
import com.fream.back.domain.accessLog.service.kafka.UserAccessLogProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 접근 로그 쓰기(생성/수정/삭제) 로직 담당
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserAccessLogCommandService {

    private final UserAccessLogRepository userAccessLogRepository;
    private final GeoIPService geoIPService;
    private final UserAccessLogProducer userAccessLogProducer;

    @Value("${access-log.use-kafka:false}")
    private boolean useKafka;

    /**
     * 접근 로그 생성
     * - Kafka 사용 설정에 따라 직접 저장 또는 비동기 전송
     *
     * @param logDto 접근 로그 DTO
     * @throws InvalidParameterException 유효하지 않은 접근 로그 데이터인 경우
     */
    @AccessLogExceptionHandler(
            defaultType = AccessLogExceptionHandler.ExceptionType.SAVE,
            message = "접근 로그 생성 중 오류가 발생했습니다",
            retry = true,
            retryCount = 3,
            logLevel = AccessLogExceptionHandler.LogLevel.ERROR
    )
    @AccessLogMethodLogger(
            level = AccessLogMethodLogger.LogLevel.INFO,
            logParameters = true,
            logReturnValue = false,
            measureExecutionTime = true,
            customMessage = "사용자 접근 로그 생성 처리"
    )
    @AccessLogPerformanceMonitor(
            thresholdMs = 500L,
            measureMemory = true,
            collectMetrics = true,
            slowQueryThresholdMs = 2000L,
            enablePerformanceGrading = true
    )
    @Transactional
    public void createAccessLog(UserAccessLogDto logDto) {
        validateLogDto(logDto);

        if (useKafka) {
            // Kafka 이벤트로 전송 (비동기 처리)
            userAccessLogProducer.sendAccessLog(logDto);
            log.debug("접근 로그 이벤트 전송 완료: {}", logDto.getIpAddress());
        } else {
            // 직접 저장 (동기 처리)
            enrichWithGeoData(logDto);
            UserAccessLog accessLog = mapToEntity(logDto);
            userAccessLogRepository.save(accessLog);
            log.debug("접근 로그 저장 완료: {}", logDto.getIpAddress());
        }
    }

    /**
     * 접근 로그 데이터 유효성 검증
     */
    @AccessLogExceptionHandler(
            defaultType = AccessLogExceptionHandler.ExceptionType.VALIDATION,
            message = "접근 로그 검증 중 오류가 발생했습니다",
            retry = false,
            logLevel = AccessLogExceptionHandler.LogLevel.WARN
    )
    @AccessLogMethodLogger(
            level = AccessLogMethodLogger.LogLevel.DEBUG,
            logParameters = true,
            logReturnValue = false,
            measureExecutionTime = false,
            customMessage = "접근 로그 데이터 검증"
    )
    private void validateLogDto(UserAccessLogDto logDto) {
        if (logDto == null) {
            throw new InvalidParameterException(AccessLogErrorCode.INVALID_ACCESS_LOG_DATA,
                    "접근 로그 데이터가 null입니다.");
        }

        // IP 주소 검증
        if (logDto.getIpAddress() == null || logDto.getIpAddress().isEmpty()) {
            log.warn("IP 주소가 누락됨: {}", logDto);
            throw new InvalidParameterException(AccessLogErrorCode.INVALID_IP_ADDRESS,
                    "IP 주소는 필수 항목입니다.");
        }

        // 페이지 URL 검증 및 기본값 설정
        if (logDto.getPageUrl() == null || logDto.getPageUrl().isEmpty()) {
            logDto.setPageUrl("Unknown");
        }
    }

    /**
     * GeoIP 서비스를 이용하여 위치 정보 추가
     */
    @AccessLogExceptionHandler(
            defaultType = AccessLogExceptionHandler.ExceptionType.GEO_IP,
            message = "위치 정보 조회 중 오류가 발생했습니다",
            retry = true,
            retryCount = 2,
            logLevel = AccessLogExceptionHandler.LogLevel.WARN
    )
    @AccessLogMethodLogger(
            level = AccessLogMethodLogger.LogLevel.DEBUG,
            logParameters = true,
            logReturnValue = false,
            measureExecutionTime = true,
            customMessage = "IP 위치 정보 조회"
    )
    @AccessLogPerformanceMonitor(
            thresholdMs = 1000L,
            measureMemory = false,
            collectMetrics = true,
            slowQueryThresholdMs = 3000L,
            logPerformance = false
    )
    private void enrichWithGeoData(UserAccessLogDto logDto) {
        GeoIPService.Location location = geoIPService.getLocation(logDto.getIpAddress());
        logDto.setCountry(location.getCountry());
        logDto.setRegion(location.getRegion());
        logDto.setCity(location.getCity());
    }

    /**
     * DTO를 엔티티로 변환
     */
    @AccessLogExceptionHandler(
            defaultType = AccessLogExceptionHandler.ExceptionType.GENERAL,
            retry = false,
            logLevel = AccessLogExceptionHandler.LogLevel.ERROR
    )
    @AccessLogMethodLogger(
            level = AccessLogMethodLogger.LogLevel.TRACE,
            logParameters = false,
            logReturnValue = false,
            measureExecutionTime = false
    )
    private UserAccessLog mapToEntity(UserAccessLogDto dto) {
        return UserAccessLog.builder()
                .refererUrl(dto.getRefererUrl())
                .userAgent(dto.getUserAgent())
                .os(dto.getOs())
                .browser(dto.getBrowser())
                .deviceType(dto.getDeviceType())
                .ipAddress(dto.getIpAddress())
                .country(dto.getCountry())
                .region(dto.getRegion())
                .city(dto.getCity())
                .pageUrl(dto.getPageUrl())
                .email(dto.getEmail())
                .isAnonymous(dto.isAnonymous())
                .networkType(dto.getNetworkType())
                .browserLanguage(dto.getBrowserLanguage())
                .screenWidth(dto.getScreenWidth())
                .screenHeight(dto.getScreenHeight())
                .devicePixelRatio(dto.getDevicePixelRatio())
                .build();
    }
}