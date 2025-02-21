package com.fream.back.domain.notification.controller.query;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.service.query.NotificationQueryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationQueryController {

    private final NotificationQueryService queryService;

    public NotificationQueryController(NotificationQueryService queryService) {
        this.queryService = queryService;
    }

    // 이메일 추출 (읽기에도 필요하면)
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    // === 카테고리별 알림 조회 (GET) ===
    @GetMapping("/filter/category")
    public List<NotificationDTO> filterByCategory(@RequestParam(name = "category") NotificationCategory category) {
        String email = extractEmailFromSecurityContext();
        return queryService.filterByCategory(email, category);
    }

    // === 유형별 알림 조회 (GET) ===
    @GetMapping("/filter/type")
    public List<NotificationDTO> filterByType(@RequestParam(name = "type") NotificationType type) {
        String email = extractEmailFromSecurityContext();
        return queryService.filterByType(email, type);
    }

    // === 카테고리 + 읽음 여부 조회 (GET) ===
    @GetMapping("/filter/category/read-status")
    public List<NotificationDTO> filterByCategoryAndIsRead(
            @RequestParam(name = "category", required = false) NotificationCategory category,
            @RequestParam(name = "isRead") boolean isRead,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "size") int size
    ) {
        String email = extractEmailFromSecurityContext();
        return queryService.filterByCategoryAndIsRead(
                email,
                category,
                isRead,
                PageRequest.of(page, size)
        );
    }

    // === 유형 + 읽음 여부 조회 (GET) ===
    @GetMapping("/filter/type/read-status")
    public List<NotificationDTO> filterByTypeAndIsRead(
            @RequestParam(name = "type") NotificationType type,
            @RequestParam(name = "isRead") boolean isRead,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "size") int size
    ) {
        String email = extractEmailFromSecurityContext();
        return queryService.filterByTypeAndIsRead(email, type, isRead, PageRequest.of(page, size));
    }
}
