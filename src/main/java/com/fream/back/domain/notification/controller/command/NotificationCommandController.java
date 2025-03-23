package com.fream.back.domain.notification.controller.command;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.exception.NotificationErrorCode;
import com.fream.back.domain.notification.exception.NotificationException;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/notifications")
@Slf4j
public class NotificationCommandController {

    private final NotificationCommandService commandService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserQueryService userQueryService;

    public NotificationCommandController(
            NotificationCommandService commandService,
            RedisTemplate<String, String> redisTemplate,
            UserQueryService userQueryService
    ) {
        this.commandService = commandService;
        this.redisTemplate = redisTemplate;
        this.userQueryService = userQueryService;
    }

    // === SecurityContextHolder에서 이메일 추출 (공통) ===
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new NotificationException(
                NotificationErrorCode.NOTIFICATION_USER_NOT_FOUND,
                "인증된 사용자 정보를 찾을 수 없습니다."
        );
    }

    // === 단일 사용자 알림 생성 (POST) ===
    @PostMapping
    public NotificationDTO createNotification(
            @RequestParam(name = "userId") Long userId,
            @Validated @RequestBody NotificationRequestDTO requestDTO
    ) {
        log.info("알림 생성 요청: 사용자ID={}, 카테고리={}, 타입={}",
                userId, requestDTO.getCategory(), requestDTO.getType());

        // 관리자 권한 체크 로직 추가 가능

        NotificationDTO result = commandService.createNotification(
                userId,
                requestDTO.getCategory(),
                requestDTO.getType(),
                requestDTO.getMessage()
        );

        log.info("알림 생성 완료: 알림ID={}, 사용자ID={}", result.getId(), userId);
        return result;
    }

    // === 전체 사용자 알림 생성 (POST) ===
    @PostMapping("/broadcast")
    public List<NotificationDTO> createNotificationForAll(@Validated @RequestBody NotificationRequestDTO requestDTO) {
        log.info("전체 사용자 알림 생성 요청: 카테고리={}, 타입={}",
                requestDTO.getCategory(), requestDTO.getType());

        // 관리자 권한 체크 로직 추가 가능

        List<NotificationDTO> results = commandService.createNotificationForAll(requestDTO);

        log.info("전체 사용자 알림 생성 완료: 생성된 알림 수={}", results.size());
        return results;
    }

    // === 알림 읽음 처리 (PATCH) ===
    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable(name = "id") Long id) {
        String email = extractEmailFromSecurityContext();
        log.info("알림 읽음 처리 요청: 알림ID={}, 사용자={}", id, email);

        commandService.markAsRead(id, email);

        log.info("알림 읽음 처리 완료: 알림ID={}, 사용자={}", id, email);
    }

    // === 알림 삭제 (DELETE) ===
    @DeleteMapping("/{id}")
    public void deleteNotification(@PathVariable(name = "id") Long id) {
        String email = extractEmailFromSecurityContext();
        log.info("알림 삭제 요청: 알림ID={}, 요청자={}", id, email);

        // 본인 소유의 알림인지 확인하는 로직 필요

        commandService.deleteNotification(id);

        log.info("알림 삭제 완료: 알림ID={}", id);
    }

    // === 사용자 알림 전체 삭제 (DELETE) ===
    @DeleteMapping("/user")
    public void deleteAllUserNotifications() {
        String email = extractEmailFromSecurityContext();
        log.info("사용자 알림 전체 삭제 요청: 사용자={}", email);

        commandService.deleteNotificationsByUser(email);

        log.info("사용자 알림 전체 삭제 완료: 사용자={}", email);
    }

    // === WebSocket Ping 처리 ===
    @MessageMapping("/ping")
    public void handlePing() {
        try {
            String email = extractEmailFromSecurityContext();
            if (email != null) {
                String redisKey = "WebSocket:User:" + email;

                // 남은 TTL(초 단위)
                Long remainingTime = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

                // TTL이 10분 이하인 경우만 30분으로 갱신
                if (remainingTime != null && remainingTime <= 600) {
                    redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
                    log.debug("Redis TTL 갱신: 사용자={}, 남은TTL={}초", email, remainingTime);
                } else {
                    log.debug("TTL 연장 불필요: 사용자={}, 남은TTL={}초", email, remainingTime);
                }
            }
        } catch (Exception e) {
            log.error("WebSocket Ping 처리 중 오류 발생: {}", e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.WEBSOCKET_CONNECTION_MAINTENANCE_FAILED,
                    "WebSocket 연결 유지 중 오류가 발생했습니다.",
                    e
            );
        }
    }
}