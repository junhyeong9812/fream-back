package com.fream.back.domain.user.controller.command;

import com.fream.back.domain.user.dto.LoginInfoDto;
import com.fream.back.domain.user.dto.LoginInfoUpdateDto;
import com.fream.back.domain.user.dto.UserRegistrationDto;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.command.UserCommandService;
import com.fream.back.domain.user.service.command.UserUpdateService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.dto.TokenDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;
    private final UserUpdateService userUpdateService;
    private final UserQueryService userQueryService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationDto dto) {
        // 회원가입 로직
        User user = userCommandService.registerUser(dto);
        return ResponseEntity.ok("회원가입 성공: " + user.getEmail());
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateUserInfo(
            @RequestHeader(value = "RefreshToken", required = false) String refreshTokenHeader,
            @RequestBody LoginInfoUpdateDto updateDto,
            HttpServletRequest request) {
        try {
            // IP 주소 추출
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }

            // SecurityContext에서 현재 이메일 추출
            String currentEmail = SecurityUtils.extractEmailFromSecurityContext();

            // 이메일 변경 여부 확인
            boolean isEmailChanged = updateDto.getNewEmail() != null &&
                    !updateDto.getNewEmail().equals(currentEmail);

            // 사용자 정보 업데이트
            userUpdateService.updateLoginInfo(currentEmail, updateDto);

            // 응답 데이터 준비
            Map<String, Object> response = new HashMap<>();

            // 변경된 정보 조회
            LoginInfoDto updatedInfo = userQueryService.getLoginInfo(
                    isEmailChanged ? updateDto.getNewEmail() : currentEmail
            );
            response.put("userInfo", updatedInfo);

            // 이메일이 변경된 경우 새로운 토큰 발급
            if (isEmailChanged) {
                String oldRefreshToken = refreshTokenHeader != null
                        ? refreshTokenHeader.replace("Bearer ", "")
                        : null;

                TokenDto newTokens = userUpdateService.reissueTokenAfterEmailChange(
                        null, // 기존 accessToken은 필요 없음
                        oldRefreshToken,
                        currentEmail,
                        updateDto.getNewEmail(),
                        ip
                );

                response.put("accessToken", newTokens.getAccessToken());
                response.put("refreshToken", newTokens.getRefreshToken());
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "사용자 정보 업데이트 중 오류가 발생했습니다."
            ));
        }
    }
}

