package com.fream.back.domain.notification.controller.query;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.exception.NotificationErrorCode;
import com.fream.back.domain.notification.exception.NotificationException;
import com.fream.back.domain.notification.service.query.NotificationQueryService;
import com.fream.back.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryController {

    private final NotificationQueryService queryService;

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
     * 카테고리별 알림 조회
     */
    @GetMapping("/filter/category")
    public ResponseEntity<List<NotificationDTO>> filterByCategory(
            @RequestParam(name = "category") NotificationCategory category
    ) {
        String email = extractEmailFromSecurityContext();
        log.info("카테고리별 알림 조회 요청: 사용자={}, 카테고리={}", email, category);

        List<NotificationDTO> results = queryService.filterByCategory(email, category);

        log.info("카테고리별 알림 조회 완료: 사용자={}, 카테고리={}, 조회된 알림 수={}",
                email, category, results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * 유형별 알림 조회
     */
    @GetMapping("/filter/type")
    public ResponseEntity<List<NotificationDTO>> filterByType(
            @RequestParam(name = "type") NotificationType type
    ) {
        String email = extractEmailFromSecurityContext();
        log.info("유형별 알림 조회 요청: 사용자={}, 유형={}", email, type);

        List<NotificationDTO> results = queryService.filterByType(email, type);

        log.info("유형별 알림 조회 완료: 사용자={}, 유형={}, 조회된 알림 수={}",
                email, type, results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * 카테고리 + 읽음 여부 조회
     */
    @GetMapping("/filter/category/read-status")
    public ResponseEntity<List<NotificationDTO>> filterByCategoryAndIsRead(
            @RequestParam(name = "category", required = false) NotificationCategory category,
            @RequestParam(name = "isRead") boolean isRead,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdDate,desc") String sort
    ) {
        validatePaginationParams(page, size);

        String email = extractEmailFromSecurityContext();
        log.info("카테고리+읽음상태별 알림 조회 요청: 사용자={}, 카테고리={}, 읽음상태={}, 페이지={}, 사이즈={}",
                email, category, isRead, page, size);

        // 정렬 처리
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 ?
                ("asc".equalsIgnoreCase(sortParams[1]) ? Sort.Direction.ASC : Sort.Direction.DESC) :
                Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));

        List<NotificationDTO> results = queryService.filterByCategoryAndIsRead(
                email,
                category,
                isRead,
                pageRequest
        );

        log.info("카테고리+읽음상태별 알림 조회 완료: 사용자={}, 카테고리={}, 읽음상태={}, 조회된 알림 수={}",
                email, category, isRead, results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * 유형 + 읽음 여부 조회
     */
    @GetMapping("/filter/type/read-status")
    public ResponseEntity<List<NotificationDTO>> filterByTypeAndIsRead(
            @RequestParam(name = "type", required = false) NotificationType type,
            @RequestParam(name = "isRead") boolean isRead,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdDate,desc") String sort
    ) {
        validatePaginationParams(page, size);

        String email = extractEmailFromSecurityContext();
        log.info("유형+읽음상태별 알림 조회 요청: 사용자={}, 유형={}, 읽음상태={}, 페이지={}, 사이즈={}",
                email, type, isRead, page, size);

        // 정렬 처리
        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 ?
                ("asc".equalsIgnoreCase(sortParams[1]) ? Sort.Direction.ASC : Sort.Direction.DESC) :
                Sort.Direction.DESC;

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortField));

        List<NotificationDTO> results = queryService.filterByTypeAndIsRead(
                email,
                type,
                isRead,
                pageRequest
        );

        log.info("유형+읽음상태별 알림 조회 완료: 사용자={}, 유형={}, 읽음상태={}, 조회된 알림 수={}",
                email, type, isRead, results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @GetMapping("/count/unread")
    public ResponseEntity<Map<String, Long>> countUnreadNotifications() {
        String email = extractEmailFromSecurityContext();
        log.info("읽지 않은 알림 개수 조회 요청: 사용자={}", email);

        long count = queryService.countUnreadNotifications(email);

        log.info("읽지 않은 알림 개수 조회 완료: 사용자={}, 개수={}", email, count);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 페이지네이션 파라미터 유효성 검사
     */
    private void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new NotificationException(
                    GlobalErrorCode.INVALID_INPUT_VALUE,
                    "페이지 번호는 0 이상이어야 합니다."
            );
        }

        if (size <= 0 || size > 100) {
            throw new NotificationException(
                    GlobalErrorCode.INVALID_INPUT_VALUE,
                    "페이지 크기는 1 이상 100 이하여야 합니다."
            );
        }
    }
}