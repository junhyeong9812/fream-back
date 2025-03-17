package com.fream.back.domain.user.redis;

import com.fream.back.domain.user.entity.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 예: User 전용 Redis 저장소 (화이트리스트, 토큰 정보 등)
 */
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
                               String ip) {
        String key = "access:" + accessToken;

        // 모든 필드를 하나의 맵으로 구성
        Map<String, String> fields = new HashMap<>();
        fields.put("email", email);
        fields.put("age", String.valueOf(age));
        fields.put("gender", gender.toString());
        if (ip != null) {
            fields.put("ip", ip);
        }

        // 한 번의 Redis 연산으로 모든 필드 저장
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, Duration.ofMillis(expirationMillis));
    }
//    public void addAccessToken(String accessToken,
//                               String email,
//                               Integer age,
//                               Gender gender,
//                               long expirationMillis,
//                               String ip)
//    {
//        String key = "access:" + accessToken;
//        redisTemplate.opsForHash().put(key, "email", email);
//        redisTemplate.opsForHash().put(key, "age", String.valueOf(age));
//        redisTemplate.opsForHash().put(key, "gender", gender.toString());
//        if (ip != null) {
//            redisTemplate.opsForHash().put(key, "ip", ip);
//        }
//        redisTemplate.expire(key, Duration.ofMillis(expirationMillis));
//    }

    /**
     * Refresh Token 저장 (email)
     */
    public void addRefreshToken(String refreshToken, String email, long expirationMillis) {
        String key = "refresh:" + refreshToken;
        redisTemplate.opsForHash().put(key, "email", email);
        redisTemplate.expire(key, Duration.ofMillis(expirationMillis));
    }

    /**
     * Access Token 존재 여부 -> 화이트리스트
     */
    public boolean isAccessTokenValid(String accessToken) {
        return redisTemplate.hasKey("access:" + accessToken);
    }

    /**
     * Refresh Token 존재 여부
     */
    public boolean isRefreshTokenValid(String refreshToken) {
        return redisTemplate.hasKey("refresh:" + refreshToken);
    }

    /**
     * Access Token -> 이메일 조회
     */
    public String getEmailByAccessToken(String accessToken) {
        Object value = redisTemplate.opsForHash().get("access:" + accessToken, "email");
        return (value != null) ? value.toString() : null;
    }

    // 나이, 성별 등도 유사하게...
    public Integer getAgeByAccessToken(String accessToken) {
        Object value = redisTemplate.opsForHash().get("access:" + accessToken, "age");
        return (value != null) ? Integer.valueOf(value.toString()) : null;
    }

    public Gender getGenderByAccessToken(String accessToken) {
        Object value = redisTemplate.opsForHash().get("access:" + accessToken, "gender");
        return (value != null) ? Gender.valueOf(value.toString()) : null;
    }

    /**
     * 토큰 삭제 (로그아웃)
     */
    public void removeAccessToken(String accessToken) {
        redisTemplate.delete("access:" + accessToken);
    }

    public void removeRefreshToken(String refreshToken) {
        redisTemplate.delete("refresh:" + refreshToken);
    }
}
