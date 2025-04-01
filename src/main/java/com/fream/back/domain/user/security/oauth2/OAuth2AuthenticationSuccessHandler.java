package com.fream.back.domain.user.security.oauth2;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRedisService authRedisService;
    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("OAuth2 인증 성공 처리");

        try {
            // OAuth2 인증 정보 가져오기
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oAuth2User = oauthToken.getPrincipal();
            String registrationId = oauthToken.getAuthorizedClientRegistrationId();

            log.info("OAuth2 로그인 성공 - 제공자: {}, 속성: {}", registrationId, oAuth2User.getAttributes());

            // 이메일 가져오기 - 로그인 제공자별로 다르게 처리
            String emailTemp = null;

            if ("naver".equals(registrationId)) {
                // 네이버 로그인의 경우 response 객체 안에 사용자 정보가 있음
                Map<String, Object> attributes = oAuth2User.getAttributes();
                if (attributes.containsKey("response")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseObj = (Map<String, Object>) attributes.get("response");
                    emailTemp = (String) responseObj.get("email");
                    log.info("네이버 로그인 이메일: {}", emailTemp);
                }
            } else if ("kakao".equals(registrationId)) {
                // 카카오 로그인 처리
                Map<String, Object> attributes = oAuth2User.getAttributes();

                // 카카오는 kakao_account 객체 내에 이메일이 있음
                if (attributes.containsKey("kakao_account")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                    emailTemp = (String) kakaoAccount.get("email");
                    log.info("카카오 로그인 이메일: {}", emailTemp);
                }
            } else {
                // 구글 등 다른 제공자
                emailTemp = oAuth2User.getAttribute("email");
                log.info("구글 로그인 이메일: {}", emailTemp);
            }

            // 이메일 검증
            final String email = emailTemp;
            if (email == null || email.isEmpty()) {
                log.error("OAuth2 인증 성공했지만 이메일을 찾을 수 없음");
                response.sendRedirect("/login?error=email_not_found");
                return;
            }

            // 사용자 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + email));

            // 클라이언트 IP 주소 가져오기
            String ipAddress = getClientIp(request);

            // 새로 가입한 사용자이고 필수 정보가 없는 경우 추가 정보 입력 페이지로 리다이렉트
            boolean needsAdditionalInfo = !user.isVerified() || user.getAge() == null || user.getGender() == null;

            if (needsAdditionalInfo) {
                // 임시 세션 ID나 토큰을 생성하여 추가 정보 입력 페이지로 전달
                String tempToken = generateTempToken(user.getEmail());
                log.info("사용자 추가 정보 입력 필요: {}", email);
                response.sendRedirect("/oauth/complete-signup?token=" + tempToken);
                return;
            }

            // JWT 토큰 생성
            TokenDto tokenDto = jwtTokenProvider.generateTokenPair(
                    user.getEmail(),
                    user.getAge(),
                    user.getGender(),
                    ipAddress,
                    user.getRole() != null ? user.getRole() : Role.USER
            );

            // 쿠키에 토큰 설정
            setCookie(response, "ACCESS_TOKEN", tokenDto.getAccessToken(), 30 * 60); // 30분
            setCookie(response, "REFRESH_TOKEN", tokenDto.getRefreshToken(), 24 * 60 * 60); // 24시간

            log.info("사용자 로그인 성공: {}, 제공자: {}", email, registrationId);

            // 프론트엔드 페이지로 리다이렉트
            response.sendRedirect("http://www.pinjun.xyz");
        } catch (Exception e) {
            log.error("OAuth2 인증 중 오류 발생", e);
            response.sendRedirect("http://www.pinjun.xyz/login?error=oauth_failed");
        }
    }

    // 임시 토큰 생성 메서드 (회원가입 완료 전용)
    private String generateTempToken(String email) {
        // 간단한 구현: 이메일 + 타임스탬프를 암호화
        String plainToken = email + "|" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(plainToken.getBytes());
        // 실제 구현에서는 보안을 위해 더 강력한 암호화 방식 사용 권장
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }

    private void setCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}