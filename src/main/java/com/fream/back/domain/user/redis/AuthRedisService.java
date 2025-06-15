package com.fream.back.domain.user.redis;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 예: User 전용 Redis 저장소 (화이트리스트, 토큰 정보 등)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthRedisService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Access Token + (email, age, gender, ip) 등을 해시로 저장
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

            // 한 번의 Redis 연산으로 모든 필드 저장
            redisTemplate.opsForHash().putAll(key, fields);
            redisTemplate.expire(key, Duration.ofMillis(expirationMillis));

            log.info("Access Token 저장 완료: email={}", email);

        } catch (Exception e) {
            log.error("Access Token 저장 실패: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 저장 중 오류가 발생했습니다.", e);
        }
    }

    // Role 정보 조회 메서드 추가
    public Role getRoleByAccessToken(String accessToken) {
        log.debug("Access Token으로 권한 조회 시작");

        try {
            Object value = redisTemplate.opsForHash().get("access:" + accessToken, "role");
            Role role = (value != null) ? Role.valueOf(value.toString()) : null;

            log.debug("Access Token으로 권한 조회 완료: role={}", role);
            return role;

        } catch (IllegalArgumentException e) {
            log.warn("권한 값 파싱 실패", e);
            return null;
        } catch (Exception e) {
            log.error("Access Token으로 권한 조회 실패", e);
            return null;
        }
    }

    /**
     * Refresh Token 저장 (email)
     */
    public void addRefreshToken(String refreshToken, String email, long expirationMillis) {
        log.debug("Refresh Token 저장 시작: email={}, expirationMs={}", email, expirationMillis);

        try {
            String key = "refresh:" + refreshToken;
            redisTemplate.opsForHash().put(key, "email", email);
            redisTemplate.expire(key, Duration.ofMillis(expirationMillis));

            log.info("Refresh Token 저장 완료: email={}", email);

        } catch (Exception e) {
            log.error("Refresh Token 저장 실패: email={}", email, e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "리프레시 토큰 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Access Token 존재 여부 -> 화이트리스트
     */
    public boolean isAccessTokenValid(String accessToken) {
        log.debug("Access Token 유효성 검사 시작");

        try {
            boolean isValid = redisTemplate.hasKey("access:" + accessToken);

            log.debug("Access Token 유효성 검사 완료: valid={}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Access Token 유효성 검사 실패", e);
            return false;
        }
    }

    /**
     * Refresh Token 존재 여부
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        log.debug("Refresh Token 유효성 검사 시작");

        try {
            boolean isValid = redisTemplate.hasKey("refresh:" + refreshToken);

            log.debug("Refresh Token 유효성 검사 완료: valid={}", isValid);
            return isValid;

        } catch (Exception e) {
            log.error("Refresh Token 유효성 검사 실패", e);
            return false;
        }
    }

    /**
     * Access Token -> 이메일 조회
     */
    public String getEmailByAccessToken(String accessToken) {
        log.debug("Access Token으로 이메일 조회 시작");

        try {
            Object value = redisTemplate.opsForHash().get("access:" + accessToken, "email");
            String email = (value != null) ? value.toString() : null;

            log.debug("Access Token으로 이메일 조회 완료: email={}", email);
            return email;

        } catch (Exception e) {
            log.error("Access Token으로 이메일 조회 실패", e);
            return null;
        }
    }

    // 나이, 성별 등도 유사하게...
    public Integer getAgeByAccessToken(String accessToken) {
        log.debug("Access Token으로 나이 조회 시작");

        try {
            Object value = redisTemplate.opsForHash().get("access:" + accessToken, "age");
            Integer age = (value != null) ? Integer.valueOf(value.toString()) : null;

            log.debug("Access Token으로 나이 조회 완료: age={}", age);
            return age;

        } catch (NumberFormatException e) {
            log.warn("나이 값 파싱 실패", e);
            return null;
        } catch (Exception e) {
            log.error("Access Token으로 나이 조회 실패", e);
            return null;
        }
    }

    public Gender getGenderByAccessToken(String accessToken) {
        log.debug("Access Token으로 성별 조회 시작");

        try {
            Object value = redisTemplate.opsForHash().get("access:" + accessToken, "gender");
            Gender gender = (value != null) ? Gender.valueOf(value.toString()) : null;

            log.debug("Access Token으로 성별 조회 완료: gender={}", gender);
            return gender;

        } catch (IllegalArgumentException e) {
            log.warn("성별 값 파싱 실패", e);
            return null;
        } catch (Exception e) {
            log.error("Access Token으로 성별 조회 실패", e);
            return null;
        }
    }

    /**
     * 토큰 삭제 (로그아웃)
     */
    public void removeAccessToken(String accessToken) {
        log.debug("Access Token 삭제 시작");

        try {
            Boolean deleted = redisTemplate.delete("access:" + accessToken);

            log.info("Access Token 삭제 완료: deleted={}", deleted);

        } catch (Exception e) {
            log.error("Access Token 삭제 실패", e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "토큰 삭제 중 오류가 발생했습니다.", e);
        }
    }

    public void removeRefreshToken(String refreshToken) {
        log.debug("Refresh Token 삭제 시작");

        try {
            Boolean deleted = redisTemplate.delete("refresh:" + refreshToken);

            log.info("Refresh Token 삭제 완료: deleted={}", deleted);

        } catch (Exception e) {
            log.error("Refresh Token 삭제 실패", e);
            throw new UserException(UserErrorCode.USER_NOT_FOUND, "리프레시 토큰 삭제 중 오류가 발생했습니다.", e);
        }
    }
}