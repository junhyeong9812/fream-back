package com.fream.back.global.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.global.config.security.dto.TokenDto;
import com.fream.back.global.exception.security.ExpiredTokenException;
import com.fream.back.global.exception.security.InvalidTokenException;
import com.fream.back.global.exception.security.TokenCreationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * JWT 생성, 검증을 담당하는 컴포넌트
 */
@Slf4j
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
     *
     * @param email 사용자 이메일
     * @param age 사용자 나이
     * @param gender 사용자 성별
     * @param ip 요청 IP 주소
     * @return 액세스 토큰과 리프레시 토큰을 포함한 DTO
     * @throws RuntimeException 토큰 생성 중 오류 발생 시
     */
    public TokenDto generateTokenPair(String email, Integer age, Gender gender, String ip, Role role) {
        log.info("토큰 생성 시작: 이메일={}, 나이={}, 성별={}, IP={}, 권한={}", email, age, gender, ip, role);

        try {
            long now = System.currentTimeMillis();

            // Access Token 생성 (role 정보 추가)
            Date accessExpiry = new Date(now + accessTokenValidityMs);
            String accessToken = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .withExpiresAt(accessExpiry)
                    .withClaim("role", role.name()) // 권한 정보 추가
                    .sign(Algorithm.HMAC512(secretKey));

            // Refresh Token 생성
            Date refreshExpiry = new Date(now + refreshTokenValidityMs);
            String refreshToken = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .withExpiresAt(refreshExpiry)
                    .sign(Algorithm.HMAC512(secretKey));

            // Redis에 저장 (role 정보도 함께 저장)
            authRedisService.addAccessToken(accessToken, email, age, gender, accessTokenValidityMs, ip, role);
            authRedisService.addRefreshToken(refreshToken, email, refreshTokenValidityMs);

            log.info("토큰 생성 완료: 이메일={}, 액세스 토큰 만료={}, 리프레시 토큰 만료={}",
                    email, accessExpiry, refreshExpiry);

            return new TokenDto(accessToken, refreshToken);
        } catch (JWTCreationException e) {
            log.error("토큰 생성 실패: 이메일={}, 오류={}", email, e.getMessage(), e);
            throw new TokenCreationException(e);
        }
    }

    // role 정보 추출 메서드 추가
    public Role getRoleFromToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            String roleName = decoded.getClaim("role").asString();
            log.debug("토큰에서 권한 추출: {}", roleName);
            return Role.valueOf(roleName);
        } catch (Exception e) {
            log.error("토큰에서 권한 추출 중 오류: {}", e.getMessage(), e);
            throw new InvalidTokenException("토큰에서 권한을 추출하는 중 오류가 발생했습니다.");
        }
    }

    /**
     * JWT 서명 + 만료시간 검증
     *
     * @param token 검증할 JWT 토큰
     * @return 유효한 토큰인 경우 true
     * @throws ExpiredTokenException 토큰이 만료된 경우
     * @throws InvalidTokenException 토큰이 유효하지 않은 경우
     */
    public boolean validateToken(String token) {
        try {
            JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            log.debug("토큰 검증 성공");
            return true;
        } catch (TokenExpiredException e) {
            log.warn("만료된 토큰: {}", e.getMessage());
            throw new ExpiredTokenException();
        } catch (JWTVerificationException e) {
            log.warn("유효하지 않은 토큰: {}", e.getMessage());
            throw new InvalidTokenException();
        } catch (Exception e) {
            log.error("토큰 검증 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InvalidTokenException("토큰 검증 중 오류가 발생했습니다.");
        }
    }

    /**
     * 토큰에서 subject(email) 추출
     *
     * @param token 이메일을 추출할 JWT 토큰
     * @return 토큰에서 추출한 이메일
     * @throws ExpiredTokenException 토큰이 만료된 경우
     * @throws InvalidTokenException 토큰이 유효하지 않은 경우
     */
    public String getEmailFromToken(String token) {
        try {
            DecodedJWT decoded = JWT.require(Algorithm.HMAC512(secretKey))
                    .build()
                    .verify(token);
            String email = decoded.getSubject();
            log.debug("토큰에서 이메일 추출: {}", email);
            return email;
        } catch (TokenExpiredException e) {
            log.warn("만료된 토큰에서 이메일 추출 시도: {}", e.getMessage());
            throw new ExpiredTokenException();
        } catch (JWTVerificationException e) {
            log.warn("유효하지 않은 토큰에서 이메일 추출 시도: {}", e.getMessage());
            throw new InvalidTokenException();
        } catch (Exception e) {
            log.error("토큰에서 이메일 추출 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InvalidTokenException("토큰에서 이메일을 추출하는 중 오류가 발생했습니다.");
        }
    }
}