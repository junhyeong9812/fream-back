package com.fream.back.domain.user.redis;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 기존 API를 유지하면서 Primary-Replica 구조를 활용한 Redis 인증 서비스
 * - 기존 메서드 시그니처와 동작 100% 호환
 * - 내부적으로 쓰기는 Primary, 읽기는 Replica 사용 (fallback 포함)
 */
@Slf4j
@Service
public class AuthRedisService {

    // 기본 RedisTemplate (Primary-Replica 자동 분산)
    private final RedisTemplate<String, String> redisTemplate;

    // 쓰기 전용 Primary RedisTemplate
    private final RedisTemplate<String, String> writeRedisTemplate;

    // 읽기 전용 Replica RedisTemplate
    private final RedisTemplate<String, String> readRedisTemplate;

    public AuthRedisService(
            RedisTemplate<String, String> redisTemplate,
            @Qualifier("writeRedisTemplate") RedisTemplate<String, String> writeRedisTemplate,
            @Qualifier("readRedisTemplate") RedisTemplate<String, String> readRedisTemplate) {
        this.redisTemplate = redisTemplate;
        this.writeRedisTemplate = writeRedisTemplate;
        this.readRedisTemplate = readRedisTemplate;
    }

    /**
     * Access Token + (email, age, gender, ip) 등을 해시로 저장
     * 기존 API 유지 - Primary Redis에 쓰기 작업 수행
     */
    public void addAccessToken(String accessToken,
                               String email,
                               Integer age,
                               Gender gender,
                               long expirationMillis,
                               String ip,
                               Role role) {
        log.debug("Access Token 저장 시작: email={}, role={}, expirationMs={}", email, role, expirationMillis);

        try {
            String key = "access:" + accessToken;

            // 모든 필드를 하나의 맵으로 구성
            Map<String, String> fields = new HashMap<>();
            fields.put("email", email);
            fields.put("age", String.valueOf(age));
            fields.put("gender", gender.toString());
            fields.put("role", role.toString()); // 권한 정보 추가
            if (ip != null) {
                fields.put("ip", ip);
            }

            // Primary Redis에 쓰기 작업 수행
            writeRedisTemplate.opsForHash().putAll(key, fields);
            writeRedisTemplate.expire(key, Duration.ofMillis(expirationMillis));

            log.info("Access Token 저장 완료 (Primary): email={}", email);

        } catch (Exception e) {
            log.error("Access Token 저장 실패: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Role 정보 조회 메서드
     * 기존 API 유지 - Replica에서 읽기, 실패 시 Primary로 fallback
     */
    public Role getRoleByAccessToken(String accessToken) {
        log.debug("Access Token으로 권한 조회 시작");

        try {
            Object value = safeReadFromReplica("access:" + accessToken, "role");
            Role role = parseRole(value);

            log.debug("Access Token으로 권한 조회 완료: role={}", role);
            return role;

        } catch (Exception e) {
            log.error("Access Token으로 권한 조회 실패", e);
            return null;
        }
    }

    /**
     * Refresh Token 저장 (email)
     * 기존 API 유지 - Primary Redis에 쓰기 작업 수행
     */
    public void addRefreshToken(String refreshToken, String email, long expirationMillis) {
        log.debug("Refresh Token 저장 시작: email={}, expirationMs={}", email, expirationMillis);

        try {
            String key = "refresh:" + refreshToken;

            // Primary Redis에 쓰기 작업 수행
            writeRedisTemplate.opsForHash().put(key, "email", email);
            writeRedisTemplate.expire(key, Duration.ofMillis(expirationMillis));

            log.info("Refresh Token 저장 완료 (Primary): email={}", email);

        } catch (Exception e) {
            log.error("Refresh Token 저장 실패: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "리프레시 토큰 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Access Token 존재 여부 -> 화이트리스트
     * 기존 API 유지 - Replica에서 읽기, 실패 시 Primary로 fallback
     */
    public boolean isAccessTokenValid(String accessToken) {
        log.debug("Access Token 유효성 검사 시작");

        try {
            boolean isValid = safeHasKey("access:" + accessToken);

            log.debug("Access Token 유효성 검사 완료: valid={}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Access Token 유효성 검사 실패", e);
            return false;
        }
    }

    /**
     * Refresh Token 존재 여부
     * 기존 API 유지 - Replica에서 읽기, 실패 시 Primary로 fallback
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        log.debug("Refresh Token 유효성 검사 시작");

        try {
            boolean isValid = safeHasKey("refresh:" + refreshToken);

            log.debug("Refresh Token 유효성 검사 완료: valid={}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Refresh Token 유효성 검사 실패", e);
            return false;
        }
    }

    /**
     * Access Token -> 이메일 조회
     * 기존 API 유지 - Replica에서 읽기, 실패 시 Primary로 fallback
     */
    public String getEmailByAccessToken(String accessToken) {
        log.debug("Access Token으로 이메일 조회 시작");

        try {
            Object value = safeReadFromReplica("access:" + accessToken, "email");
            String email = (value != null) ? value.toString() : null;

            log.debug("Access Token으로 이메일 조회 완료: email={}", email);
            return email;

        } catch (Exception e) {
            log.error("Access Token으로 이메일 조회 실패", e);
            return null;
        }
    }

    /**
     * 나이 조회
     * 기존 API 유지 - Replica에서 읽기, 실패 시 Primary로 fallback
     */
    public Integer getAgeByAccessToken(String accessToken) {
        log.debug("Access Token으로 나이 조회 시작");

        try {
            Object value = safeReadFromReplica("access:" + accessToken, "age");
            Integer age = parseAge(value);

            log.debug("Access Token으로 나이 조회 완료: age={}", age);
            return age;

        } catch (Exception e) {
            log.error("Access Token으로 나이 조회 실패", e);
            return null;
        }
    }

    /**
     * 성별 조회
     * 기존 API 유지 - Replica에서 읽기, 실패 시 Primary로 fallback
     */
    public Gender getGenderByAccessToken(String accessToken) {
        log.debug("Access Token으로 성별 조회 시작");

        try {
            Object value = safeReadFromReplica("access:" + accessToken, "gender");
            Gender gender = parseGender(value);

            log.debug("Access Token으로 성별 조회 완료: gender={}", gender);
            return gender;

        } catch (Exception e) {
            log.error("Access Token으로 성별 조회 실패", e);
            return null;
        }
    }

    /**
     * 토큰 삭제 (로그아웃)
     * 기존 API 유지 - Primary Redis에서 삭제 작업 수행
     */
    public void removeAccessToken(String accessToken) {
        log.debug("Access Token 삭제 시작");

        try {
            Boolean deleted = writeRedisTemplate.delete("access:" + accessToken);
            log.info("Access Token 삭제 완료 (Primary): deleted={}", deleted);

        } catch (Exception e) {
            log.error("Access Token 삭제 실패", e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Refresh Token 삭제
     * 기존 API 유지 - Primary Redis에서 삭제 작업 수행
     */
    public void removeRefreshToken(String refreshToken) {
        log.debug("Refresh Token 삭제 시작");

        try {
            Boolean deleted = writeRedisTemplate.delete("refresh:" + refreshToken);
            log.info("Refresh Token 삭제 완료 (Primary): deleted={}", deleted);

        } catch (Exception e) {
            log.error("Refresh Token 삭제 실패", e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "리프레시 토큰 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // ==================== 내부 헬퍼 메서드 ====================

    /**
     * Replica에서 안전하게 읽기 (fallback to Primary)
     */
    private Object safeReadFromReplica(String key, String field) {
        try {
            // 먼저 Replica에서 시도
            Object value = readRedisTemplate.opsForHash().get(key, field);

            if (value == null) {
                // Replica에서 null이면 Primary에서 다시 확인 (replication lag 대응)
                log.debug("Replica에서 읽기 실패, Primary에서 재시도: key={}, field={}", key, field);
                value = writeRedisTemplate.opsForHash().get(key, field);
            }

            return value;

        } catch (Exception replicaException) {
            // Replica 실패 시 Primary로 fallback
            log.warn("Replica Redis 접근 실패, Primary로 fallback: key={}, field={}", key, field, replicaException);
            try {
                return writeRedisTemplate.opsForHash().get(key, field);
            } catch (Exception primaryException) {
                log.error("Primary Redis 접근도 실패: key={}, field={}", key, field, primaryException);
                return null;
            }
        }
    }

    /**
     * Replica에서 안전하게 키 존재 확인 (fallback to Primary)
     */
    private boolean safeHasKey(String key) {
        try {
            // 먼저 Replica에서 시도
            boolean exists = readRedisTemplate.hasKey(key);

            if (!exists) {
                // Replica에서 없으면 Primary에서 다시 확인 (replication lag 대응)
                log.debug("Replica에서 키 확인 실패, Primary에서 재시도: key={}", key);
                exists = writeRedisTemplate.hasKey(key);
            }

            return exists;

        } catch (Exception replicaException) {
            // Replica 실패 시 Primary로 fallback
            log.warn("Replica Redis 접근 실패, Primary로 fallback: key={}", key, replicaException);
            try {
                return writeRedisTemplate.hasKey(key);
            } catch (Exception primaryException) {
                log.error("Primary Redis 접근도 실패: key={}", key, primaryException);
                return false;
            }
        }
    }

    // ==================== 파싱 헬퍼 메서드 ====================

    private Role parseRole(Object value) {
        try {
            return (value != null) ? Role.valueOf(value.toString()) : null;
        } catch (IllegalArgumentException e) {
            log.warn("권한 값 파싱 실패: {}", value, e);
            return null;
        }
    }

    private Integer parseAge(Object value) {
        try {
            return (value != null) ? Integer.valueOf(value.toString()) : null;
        } catch (NumberFormatException e) {
            log.warn("나이 값 파싱 실패: {}", value, e);
            return null;
        }
    }

    private Gender parseGender(Object value) {
        try {
            return (value != null) ? Gender.valueOf(value.toString()) : null;
        } catch (IllegalArgumentException e) {
            log.warn("성별 값 파싱 실패: {}", value, e);
            return null;
        }
    }
}