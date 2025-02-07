package com.fream.back.domain.notification.controller.command;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/notifications")
public class NotificationCommandController {

    private final NotificationCommandService commandService;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserQueryService userQueryService; // 관리자 권한이 필요하다면, 혹은 사용자 정보 조회용

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
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    // === 단일 사용자 알림 생성 (POST) ===
    @PostMapping
    public NotificationDTO createNotification(
            @RequestParam(name = "userId") Long userId,
            @RequestBody NotificationRequestDTO requestDTO
    ) {
        // 필요시 extractEmail() 해서 권한 체크 가능
        return commandService.createNotification(userId,
                requestDTO.getCategory(), requestDTO.getType(), requestDTO.getMessage());
    }

    // === 전체 사용자 알림 생성 (POST) ===
    @PostMapping("/broadcast")
    public List<NotificationDTO> createNotificationForAll(@RequestBody NotificationRequestDTO requestDTO) {
        // 필요시 관리자 권한 체크
        return commandService.createNotificationForAll(requestDTO);
    }

    // === 알림 읽음 처리 (PATCH) ===
    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable(name = "id") Long id) {
        String email = extractEmailFromSecurityContext();
        commandService.markAsRead(id, email);
    }

    // === WebSocket Ping 처리 ===
    //    MessageMapping은 STOMP 엔드포인트이므로, RestController와 별도 분리하는 경우도 있지만
    //    여기서는 Command에 포함시킴
    @MessageMapping("/ping")
    public void handlePing() {
        String email = extractEmailFromSecurityContext();
        if (email != null) {
            String redisKey = "WebSocket:User:" + email;

            // 남은 TTL(초 단위)
            Long remainingTime = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

            // TTL이 10분 이하인 경우만 30분으로 갱신
            if (remainingTime != null && remainingTime <= 600) {
                redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
                System.out.println("Redis TTL 갱신: 사용자=" + email + ", 남은TTL=" + remainingTime + "초");
            } else {
                System.out.println("TTL 연장 불필요: 사용자=" + email + ", 남은TTL=" + remainingTime + "초");
            }
        }
    }
}
