package com.fream.back.domain.notification.controller.command;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.exception.NotificationErrorCode;
import com.fream.back.domain.notification.exception.NotificationException;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandController {

    private final NotificationCommandService commandService;

    /**
     * SecurityContextHolder에서 이메일 추출
     */
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

    /**
     * 단일 사용자 알림 생성
     */
    @PostMapping
    public ResponseEntity<NotificationDTO> createNotification(
            @RequestParam(name = "userId") Long userId,
            @Validated @RequestBody NotificationRequestDTO requestDTO
    ) {
        log.info("알림 생성 요청: 사용자ID={}, 카테고리={}, 타입={}",
                userId, requestDTO.getCategory(), requestDTO.getType());

        // 관리자 권한 체크 로직 추가 필요 (미구현)

        NotificationDTO result = commandService.createNotification(
                userId,
                requestDTO.getCategory(),
                requestDTO.getType(),
                requestDTO.getMessage()
        );

        log.info("알림 생성 완료: 알림ID={}, 사용자ID={}", result.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 전체 사용자 알림 생성
     */
    @PostMapping("/broadcast")
    public ResponseEntity<List<NotificationDTO>> createNotificationForAll(
            @Validated @RequestBody NotificationRequestDTO requestDTO
    ) {
        log.info("전체 사용자 알림 생성 요청: 카테고리={}, 타입={}",
                requestDTO.getCategory(), requestDTO.getType());

        // 관리자 권한 체크 로직 추가 필요 (미구현)

        List<NotificationDTO> results = commandService.createNotificationForAll(requestDTO);

        log.info("전체 사용자 알림 생성 완료: 생성된 알림 수={}", results.size());
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }

    /**
     * 알림 읽음 처리
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable(name = "id") Long id) {
        String email = extractEmailFromSecurityContext();
        log.info("알림 읽음 처리 요청: 알림ID={}, 사용자={}", id, email);

        commandService.markAsRead(id, email);

        log.info("알림 읽음 처리 완료: 알림ID={}, 사용자={}", id, email);
        return ResponseEntity.ok().build();
    }

    /**
     * 모든 알림 읽음 처리
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        String email = extractEmailFromSecurityContext();
        log.info("모든 알림 읽음 처리 요청: 사용자={}", email);

        commandService.markAllAsRead(email);

        log.info("모든 알림 읽음 처리 완료: 사용자={}", email);
        return ResponseEntity.ok().build();
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable(name = "id") Long id) {
        String email = extractEmailFromSecurityContext();
        log.info("알림 삭제 요청: 알림ID={}, 요청자={}", id, email);

        // 본인 소유의 알림인지 확인하는 로직은 서비스에서 구현 필요

        commandService.deleteNotification(id);

        log.info("알림 삭제 완료: 알림ID={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자 알림 전체 삭제
     */
    @DeleteMapping("/user")
    public ResponseEntity<Void> deleteAllUserNotifications() {
        String email = extractEmailFromSecurityContext();
        log.info("사용자 알림 전체 삭제 요청: 사용자={}", email);

        commandService.deleteNotificationsByUser(email);

        log.info("사용자 알림 전체 삭제 완료: 사용자={}", email);
        return ResponseEntity.noContent().build();
    }

    /**
     * WebSocket Ping 처리
     */
    @MessageMapping("/ping")
    public void handlePing() {
        try {
            String email = extractEmailFromSecurityContext();
            if (email != null) {
                commandService.updateWebSocketConnectionTTL(email);
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