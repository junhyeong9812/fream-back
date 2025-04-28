package com.fream.back.domain.notification.service.query;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.exception.NotificationErrorCode;
import com.fream.back.domain.notification.exception.NotificationException;
import com.fream.back.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 조회 서비스이므로 readOnly = true 설정
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    /**
     * 카테고리별 알림 조회
     */
    public List<NotificationDTO> filterByCategory(String email, NotificationCategory category) {
        try {
            log.debug("카테고리별 알림 조회: 사용자={}, 카테고리={}", email, category);

            if (category == null) {
                throw new NotificationException(
                        NotificationErrorCode.INVALID_NOTIFICATION_CATEGORY,
                        "알림 카테고리가 지정되지 않았습니다."
                );
            }

            List<Notification> notifications = notificationRepository.findByUserEmailAndCategory(email, category);
            log.debug("카테고리별 알림 조회 결과: 사용자={}, 카테고리={}, 조회된 알림 수={}",
                    email, category, notifications.size());

            return notifications.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (NotificationException e) {
            throw e; // 이미 NotificationException이면 그대로 전파
        } catch (Exception e) {
            log.error("카테고리별 알림 조회 중 오류 발생: 사용자={}, 카테고리={}, 오류={}",
                    email, category, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_RETRIEVAL_FAILED,
                    "카테고리별 알림 조회 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 유형별 알림 조회
     */
    public List<NotificationDTO> filterByType(String email, NotificationType type) {
        try {
            log.debug("유형별 알림 조회: 사용자={}, 유형={}", email, type);

            if (type == null) {
                throw new NotificationException(
                        NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                        "알림 유형이 지정되지 않았습니다."
                );
            }

            List<Notification> notifications = notificationRepository.findByUserEmailAndType(email, type);
            log.debug("유형별 알림 조회 결과: 사용자={}, 유형={}, 조회된 알림 수={}",
                    email, type, notifications.size());

            return notifications.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (NotificationException e) {
            throw e; // 이미 NotificationException이면 그대로 전파
        } catch (Exception e) {
            log.error("유형별 알림 조회 중 오류 발생: 사용자={}, 유형={}, 오류={}",
                    email, type, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_RETRIEVAL_FAILED,
                    "유형별 알림 조회 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 카테고리별 + 읽음 여부 알림 조회
     */
    public List<NotificationDTO> filterByCategoryAndIsRead(
            String email, NotificationCategory category, boolean isRead, Pageable pageable) {
        try {
            log.debug("카테고리+읽음상태별 알림 조회: 사용자={}, 카테고리={}, 읽음상태={}, 페이지정보={}",
                    email, category, isRead, pageable);

            Page<Notification> notifications = notificationRepository.findByUserEmailAndCategoryAndIsRead(
                    email, category, isRead, pageable);

            log.debug("카테고리+읽음상태별 알림 조회 결과: 사용자={}, 총 {}개 중 {}개 조회됨",
                    email, notifications.getTotalElements(), notifications.getNumberOfElements());

            return notifications.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("카테고리+읽음상태별 알림 조회 중 오류 발생: 사용자={}, 카테고리={}, 읽음상태={}, 오류={}",
                    email, category, isRead, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_RETRIEVAL_FAILED,
                    "알림 조회 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 유형별 + 읽음 여부 알림 조회
     */
    public List<NotificationDTO> filterByTypeAndIsRead(
            String email, NotificationType type, boolean isRead, Pageable pageable) {
        try {
            log.debug("유형+읽음상태별 알림 조회: 사용자={}, 유형={}, 읽음상태={}, 페이지정보={}",
                    email, type, isRead, pageable);

            Page<Notification> notifications = notificationRepository.findByUserEmailAndTypeAndIsRead(
                    email, type, isRead, pageable);

            log.debug("유형+읽음상태별 알림 조회 결과: 사용자={}, 총 {}개 중 {}개 조회됨",
                    email, notifications.getTotalElements(), notifications.getNumberOfElements());

            return notifications.stream()
                    .map(this::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("유형+읽음상태별 알림 조회 중 오류 발생: 사용자={}, 유형={}, 읽음상태={}, 오류={}",
                    email, type, isRead, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_RETRIEVAL_FAILED,
                    "알림 조회 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 읽지 않은 알림 개수 조회 (추가된 기능)
     */
    public long countUnreadNotifications(String email) {
        try {
            Page<Notification> notifications = notificationRepository.findByUserEmailAndIsRead(
                    email, false, Pageable.unpaged());
            return notifications.getTotalElements();
        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_RETRIEVAL_FAILED,
                    "읽지 않은 알림 개수 조회 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * DTO 변환 메서드
     */
    private NotificationDTO toDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .type(notification.getType())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedDate().toString())
                .build();
    }
}