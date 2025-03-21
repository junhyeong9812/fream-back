package com.fream.back.domain.accessLog.service.command;

import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.exception.AccessLogErrorCode;
import com.fream.back.domain.accessLog.exception.InvalidParameterException;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import com.fream.back.domain.accessLog.service.geo.GeoIPService;
import com.fream.back.domain.accessLog.service.kafka.UserAccessLogProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    /**
     * 접근 로그 생성
     * - 현재 로직은 Kafka 프로듀서를 통해 비동기 전송 (DB 저장은 Consumer 측)
     *
     * @param logDto 접근 로그 DTO
     * @throws InvalidParameterException 유효하지 않은 접근 로그 데이터인 경우
     */
    public void createAccessLog(UserAccessLogDto logDto) {
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

        // 기본 검증
        if (logDto.getPageUrl() == null || logDto.getPageUrl().isEmpty()) {
            // 페이지 URL이 없으면 "Unknown"으로 설정
            logDto.setPageUrl("Unknown");
        }

        // 현재는 카프카를 통해 전달
        userAccessLogProducer.sendAccessLog(logDto);
        log.debug("접근 로그 이벤트 전송 완료: {}", logDto.getIpAddress());
    }
}