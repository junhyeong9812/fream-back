package com.fream.back.domain.user.controller.query;

import com.fream.back.domain.user.dto.*;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.redis.AuthRedisService;
import com.fream.back.domain.user.service.command.AuthService;
import com.fream.back.domain.user.service.command.PasswordResetService;
import com.fream.back.domain.user.service.command.UserCommandService;
import com.fream.back.domain.user.service.command.UserUpdateService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.domain.user.validate.UserControllerValidator;
import com.fream.back.global.config.security.JwtTokenProvider;
import com.fream.back.global.config.security.dto.TokenDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserQueryController {

    private final AuthService authService;
    private final UserQueryService userQueryService;
    private final PasswordResetService passwordResetService;
    private final UserUpdateService userUpdateService;
    private final JwtTokenProvider jwtTokenProvider; // JwtTokenProvider 주입
    private final UserCommandService userCommandService;
    private final AuthRedisService redisService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody @Validated UserRegistrationDto dto) {
        try {
            // 1) DTO 검증
            UserControllerValidator.validateUserRegistrationDto(dto);

            // 2) 검증 통과 시 Service 로직
            User user = userCommandService.registerUser(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "userEmail", user.getEmail()));
        } catch (IllegalArgumentException e) {
            // 사용자 입력이 잘못된 경우 - 400 Bad Request
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            // 서버 내부 오류 - 500 Internal Server Error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "회원가입 처리 중 문제가 발생했습니다."));
        }
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody LoginRequestDto loginRequestDto,
            HttpServletRequest request
    ) {
        try {
            // "X-Forwarded-For" 헤더 확인 (프록시나 로드 밸런서가 설정한 IP)
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                // 2) Fallback: 직접 연결된 소켓의 IP
                ip = request.getRemoteAddr();
            }

            // 검증
            UserControllerValidator.validateLoginRequestDto(loginRequestDto);

            // 로직 -> 여기서 TokenDto 받기
            TokenDto tokenDto = authService.login(loginRequestDto,ip);

            // JSON 응답으로 accessToken, refreshToken 모두 내려주기
            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("accessToken", tokenDto.getAccessToken());
            responseBody.put("refreshToken", tokenDto.getRefreshToken());

            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "로그인 처리 중 문제가 발생했습니다."));
        }
    }
    
    //이메일 찾기 
    @PostMapping("/find-email")
    public ResponseEntity<Map<String, String>> findEmail(@RequestBody EmailFindRequestDto emailFindRequestDto) {
        try {
            // ★검증 추가
            UserControllerValidator.validateEmailFindRequestDto(emailFindRequestDto);
            String email = userQueryService.findEmailByPhoneNumber(emailFindRequestDto);
            return ResponseEntity.ok(Map.of("email", email));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "이메일 찾기 처리 중 문제가 발생했습니다."));
        }
    }

    // 비밀번호 찾기 - 사용자 확인
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPasswordEligibility(@RequestBody PasswordResetRequestDto dto) {
        try {
            // ★검증 추가(이메일/폰번호만 확인하는 메서드)
            UserControllerValidator.validatePasswordResetEligibilityDto(dto);

            boolean eligible = passwordResetService.checkPasswordResetEligibility(dto.getEmail(), dto.getPhoneNumber());
            if (eligible) {
                return ResponseEntity.ok(Map.of("status", "ok"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "해당 이메일 및 전화번호로 사용자를 찾을 수 없습니다."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "비밀번호 찾기 처리 중 문제가 발생했습니다."));
        }
    }
    // 비밀번호 찾기 - 사용자 확인
    @PostMapping("/reset-password-sandEmail")
    public ResponseEntity<Map<String, String>> resetPasswordToEmail(@RequestBody PasswordResetRequestDto dto) {
        try {
            // ★검증 추가(이메일/폰번호만 확인)
            UserControllerValidator.validatePasswordResetEligibilityDto(dto);

            boolean eligible = passwordResetService.checkPasswordResetAndSendEmail(dto.getEmail(), dto.getPhoneNumber());
            if (eligible) {
                return ResponseEntity.ok(Map.of("status", "success", "message", "임시 비밀번호가 이메일로 전송되었습니다."));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "error", "message", "해당 이메일 및 전화번호로 사용자를 찾을 수 없습니다."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "비밀번호 찾기 처리 중 문제가 발생했습니다."));
        }
    }

    // 비밀번호 변경
    @PostMapping("/reset")
    public ResponseEntity<String> resetPassword(@RequestBody PasswordResetRequestDto requestDto) {
        try {
            // ★검증 추가(새 비밀번호/확인)
            UserControllerValidator.validatePasswordResetDto(requestDto);

            boolean isReset = passwordResetService.resetPassword(requestDto);
            if (isReset) {
                return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("비밀번호 변경 중 문제가 발생했습니다.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 로그인 정보 변경
    @PutMapping("/update-login-info")
    public ResponseEntity<Map<String, String>> updateLoginInfo(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestHeader(value = "RefreshToken", required = false) String refreshTokenHeader,
            @RequestBody LoginInfoUpdateDto dto,
            HttpServletRequest request) {
        try {
            // "X-Forwarded-For" 헤더 확인 (프록시나 로드 밸런서가 설정한 IP)
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                // 2) Fallback: 직접 연결된 소켓의 IP
                ip = request.getRemoteAddr();
            }
            // 1) 현재 이메일(= oldEmail) 추출
            String oldEmail = SecurityUtils.extractEmailFromSecurityContext();

            // 2) DTO 검증 (비밀번호, 새 이메일 형식, 새 전화번호 정규식 등)
            UserControllerValidator.validateLoginInfoUpdateDto(dto);

            // 3) 로그인 정보 업데이트 (이메일/비밀번호 등)
            //    예외 발생 시 IllegalArgumentException 던짐
            userUpdateService.updateLoginInfo(oldEmail, dto);

            // 4) 새 이메일 플래그 체크
            String newEmail = dto.getNewEmail();
            boolean emailChanged = (newEmail != null && !newEmail.isBlank() && !newEmail.equals(oldEmail));

            // 5) 이메일 변경 시 → 토큰 재발급
            if (emailChanged) {
                // 5-1) 기존 토큰
                String oldAccessToken = authorizationHeader.replace("Bearer ", "");
                String oldRefreshToken = (refreshTokenHeader != null)
                        ? refreshTokenHeader.replace("Bearer ", "")
                        : null;

                // 5-2) userUpdateService 쪽 메서드 호출로 기존 토큰 제거 & 새 토큰 발급
                //      (newEmail 로 DB 다시 조회하여 나이, 성별 꺼낸 다음 TokenPair 생성)
                TokenDto newTokens = userUpdateService.reissueTokenAfterEmailChange(
                        oldAccessToken, oldRefreshToken, oldEmail, newEmail,ip
                );

                // 5-3) 응답에 새 토큰 담아서 반환
                Map<String, String> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "로그인 정보가 성공적으로 변경되었습니다.");
                response.put("accessToken", newTokens.getAccessToken());
                response.put("refreshToken", newTokens.getRefreshToken());

                return ResponseEntity.ok(response);
            }

            // 이메일 변경 안 됐으면 그냥 메시지 반환
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "로그인 정보가 성공적으로 변경되었습니다."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "로그인 정보 변경 처리 중 문제가 발생했습니다."));
        }
    }

    //로그인 정보 조회
    @GetMapping("/login-info")
    public ResponseEntity<LoginInfoDto> getLoginInfo(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();

            LoginInfoDto loginInfoDto = userQueryService.getLoginInfo(email);
            return ResponseEntity.ok(loginInfoDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    //회원 삭제
    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();

            userCommandService.deleteAccount(email);

            return ResponseEntity.ok(Map.of("status", "success", "message", "회원 탈퇴가 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "회원 탈퇴 처리 중 문제가 발생했습니다."));
        }
    }


}