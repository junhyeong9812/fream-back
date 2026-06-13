package com.fream.back.global.security.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * IP 차단 및 레이트 리미팅 전용 Redis 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlockingRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    // 설정값들 (필요시 @Value로 외부화 가능)
    private static final int DEFAULT_MAX_REQUESTS = 100; // 10초 동안 최대 100회 요청
    private static final int DEFAULT_WINDOW_SECONDS = 10; // 시간 윈도우 10초
    private static final int DEFAULT_BLOCK_MINUTES = 30; // 차단 시간 30분

    /**
     * IP 요청 허용 여부 체크 (레이트 리미팅)
     * @param ip IP 주소
     * @return true: 허용, false: 차단
     */
    public boolean isIpAllowed(String ip) {
        return isIpAllowed(ip, DEFAULT_MAX_REQUESTS, DEFAULT_WINDOW_SECONDS);
    }

    /**
     * IP 요청 허용 여부 체크 (레이트 리미팅) - 커스텀 설정
     * @param ip IP 주소
     * @param maxRequests 허용 최대 요청 수
     * @param windowSeconds 시간 윈도우 (초)
     * @return true: 허용, false: 차단
     */
    public boolean isIpAllowed(String ip, int maxRequests, int windowSeconds) {
        // 1. 블랙리스트 체크 (이미 차단된 IP인지)
        if (isIpBlocked(ip)) {
            log.warn("Blocked IP attempted access: {}", ip);
            return false;
        }

        String rateLimitKey = "rate_limit:ip:" + ip;

        try {
            // 2. 현재 요청 횟수 조회
            String currentCountStr = redisTemplate.opsForValue().get(rateLimitKey);
            int currentCount = (currentCountStr != null) ? Integer.parseInt(currentCountStr) : 0;

            // 3. 제한 초과 시 IP 차단
            if (currentCount >= maxRequests) {
                log.warn("IP {} exceeded rate limit ({} requests in {} seconds). Blocking IP.",
                        ip, maxRequests, windowSeconds);
                blockIp(ip, DEFAULT_BLOCK_MINUTES);
                return false;
            }

            // 4. 요청 횟수 증가
            if (currentCount == 0) {
                // 첫 요청인 경우 TTL 설정
                redisTemplate.opsForValue().set(rateLimitKey, "1", Duration.ofSeconds(windowSeconds));
            } else {
                // 기존 TTL 유지하면서 카운트 증가
                redisTemplate.opsForValue().increment(rateLimitKey);
            }

            log.debug("IP {} request count: {}/{} in {} seconds window",
                    ip, currentCount + 1, maxRequests, windowSeconds);
            return true;

        } catch (Exception e) {
            log.error("Error checking rate limit for IP {}: {}", ip, e.getMessage());
            // 에러 발생 시 안전하게 허용 (또는 차단하도록 변경 가능)
            return true;
        }
    }

    /**
     * IP 블랙리스트 체크
     * @param ip IP 주소
     * @return true: 차단된 IP, false: 허용된 IP
     */
    public boolean isIpBlocked(String ip) {
        String blockKey = "blocked_ip:" + ip;
        return redisTemplate.hasKey(blockKey);
    }

    /**
     * IP 차단 (블랙리스트에 추가)
     * @param ip IP 주소
     * @param blockDurationMinutes 차단 시간 (분)
     */
    public void blockIp(String ip, int blockDurationMinutes) {
        String blockKey = "blocked_ip:" + ip;
        String rateLimitKey = "rate_limit:ip:" + ip;

        try {
            // 블랙리스트에 추가
            redisTemplate.opsForValue().set(blockKey, "blocked", Duration.ofMinutes(blockDurationMinutes));

            // 레이트 리미트 카운터 삭제
            redisTemplate.delete(rateLimitKey);

            log.warn("IP {} blocked for {} minutes due to rate limit violation", ip, blockDurationMinutes);

        } catch (Exception e) {
            log.error("Error blocking IP {}: {}", ip, e.getMessage());
        }
    }

    /**
     * IP 차단 해제
     * @param ip IP 주소
     */
    public void unblockIp(String ip) {
        String blockKey = "blocked_ip:" + ip;
        String rateLimitKey = "rate_limit:ip:" + ip;

        try {
            redisTemplate.delete(blockKey);
            redisTemplate.delete(rateLimitKey);
            log.info("IP {} unblocked", ip);

        } catch (Exception e) {
            log.error("Error unblocking IP {}: {}", ip, e.getMessage());
        }
    }

    /**
     * IP의 현재 요청 횟수 조회 (모니터링용)
     * @param ip IP 주소
     * @return 현재 요청 횟수
     */
    public int getCurrentRequestCount(String ip) {
        String rateLimitKey = "rate_limit:ip:" + ip;
        String countStr = redisTemplate.opsForValue().get(rateLimitKey);
        return (countStr != null) ? Integer.parseInt(countStr) : 0;
    }

    /**
     * IP 차단 남은 시간 조회 (초 단위)
     * @param ip IP 주소
     * @return 남은 차단 시간 (초), 차단되지 않은 경우 -1
     */
    public long getBlockRemainingTime(String ip) {
        String blockKey = "blocked_ip:" + ip;
        return redisTemplate.getExpire(blockKey);
    }
}