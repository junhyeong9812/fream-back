package com.fream.back.domain.accessLog.service.command;

import com.fream.back.domain.accessLog.dto.UserAccessLogDto;
import com.fream.back.domain.accessLog.repository.UserAccessLogRepository;
import com.fream.back.domain.accessLog.service.geo.GeoIPService;
import com.fream.back.domain.accessLog.service.kafka.UserAccessLogProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 접근 로그 쓰기(생성/수정/삭제) 로직 담당
 */
@Service
@RequiredArgsConstructor
public class UserAccessLogCommandService {

    private final UserAccessLogRepository userAccessLogRepository;
    private final GeoIPService geoIPService;
    private final UserAccessLogProducer userAccessLogProducer;

    /**
     * 접근 로그 생성
     * - 현재 로직은 Kafka 프로듀서를 통해 비동기 전송 (DB 저장은 Consumer 측)
     */
    public void createAccessLog(UserAccessLogDto logDto) {
        // 현재는 카프카를 통해 전달
        userAccessLogProducer.sendAccessLog(logDto);
    }
}
