package com.fream.back.global.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.global.config.security.dto.TokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 생성, 검증을 담당하는 컴포넌트
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;  // JWT 서명 비밀키

    @Value("${jwt.expiration}")
    private long accessTokenValidityMs; // AccessToken 만료(밀리초)

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenValidityMs; // RefreshToken 만료(밀리초)

    private final AuthRedisService authRedisService;

    /**
     * AccessToken + RefreshToken 동시 발급
     * role 등 추가 클레임을 담으려면 withClaim("role", role) 형태로도 가능
     */
    public TokenDto generateTokenPair(String email, Integer age, Gender gender, String ip) {
        long now = System.currentTimeMillis();

        // Access Token 생성
        Date accessExpiry = new Date(now + accessTokenValidityMs);
        String accessToken = JWT.create()
                .withSubject(email)
                .withIssuedAt(new Date())
                .withExpiresAt(accessExpiry)
                .sign(Algorithm.HMAC512(secretKey));

        // Refresh Token 생성
        Date refreshExpiry = new Date(now + refreshTokenValidityMs);
        String refreshToken = JWT.create()
                .withSubject(email)
                .withIssuedAt(new Date())
                .withExpiresAt(refreshExpiry)
                .sign(Algorithm.HMAC512(secretKey));

        // Redis에 저장
        authRedisService.addAccessToken(accessToken, email, age, gender, accessTokenValidityMs, ip);
        authRedisService.addRefreshToken(refreshToken, email, refreshTokenValidityMs);

        return new TokenDto(accessToken, refreshToken);
    }

    /**
     * JWT 서명 + 만료시간 검증
     */
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰에서 subject(email) 추출
     */
    public String getEmailFromToken(String token) {
        DecodedJWT decoded = JWT.require(Algorithm.HMAC512(secretKey))
                .build()
                .verify(token);
        return decoded.getSubject();
    }
}
