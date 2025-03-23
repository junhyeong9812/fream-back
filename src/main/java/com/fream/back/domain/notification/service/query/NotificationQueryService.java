package com.fream.back.domain.notification.service.query;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.exception.NotificationErrorCode;
import com.fream.back.domain.notification.exception.NotificationException;
import com.fream.back.domain.notification.repository.NotificationRepository;
import com.fream.back.domain.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    // 카테고리별 알림 조회
    public List<NotificationDTO> filterByCategory(String email, NotificationCategory category) {
        try {
            log.debug("카테고리별 알림 조회: 사용자={}, 카테고리={}", email, category);

            if (category == null) {
                throw new NotificationException(
                        NotificationErrorCode.INVALID_NOTIFICATION_CATEGORY,
                        "알림 카테고리가 지정되지 않았습니다."
                );
            }

            List<Notification> notifications = notificationRepository.findAllByUserEmailAndCategory(email, category);
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

    // 유형별 알림 조회
    public List<NotificationDTO> filterByType(String email, NotificationType type) {
        try {
            log.debug("유형별 알림 조회: 사용자={}, 유형={}", email, type);

            if (type == null) {
                throw new NotificationException(
                        NotificationErrorCode.INVALID_NOTIFICATION_TYPE,
                        "알림 유형이 지정되지 않았습니다."
                );
            }

            List<Notification> notifications = notificationRepository.findAllByUserEmailAndType(email, type);
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

    // 카테고리별 + 읽음 여부 알림 조회
    public List<NotificationDTO> filterByCategoryAndIsRead(String email, NotificationCategory category, boolean isRead, Pageable pageable) {
        try {
            log.debug("카테고리+읽음상태별 알림 조회: 사용자={}, 카테고리={}, 읽음상태={}, 페이지정보={}",
                    email, category, isRead, pageable);

            Page<Notification> notifications;

            if (category == null) {
                // 카테고리가 지정되지 않았다면 모든 카테고리에서 읽음 상태만으로 필터링
                notifications = notificationRepository.findAllByUserEmailAndIsRead(email, isRead, pageable);
                log.debug("모든 카테고리에서 읽음상태별 알림 조회: 사용자={}, 읽음상태={}", email, isRead);
            } else {
                // 카테고리와 읽음 상태 모두로 필터링
                notifications = notificationRepository.findAllByUserEmailAndCategoryAndIsRead(email, category, isRead, pageable);
                log.debug("특정 카테고리에서 읽음상태별 알림 조회: 사용자={}, 카테고리={}, 읽음상태={}",
                        email, category, isRead);
            }

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

    // 유형별 + 읽음 여부 알림 조회
    public List<NotificationDTO> filterByTypeAndIsRead(String email, NotificationType type, boolean isRead, Pageable pageable) {
        try {
            log.debug("유형+읽음상태별 알림 조회: 사용자={}, 유형={}, 읽음상태={}, 페이지정보={}",
                    email, type, isRead, pageable);

            Page<Notification> notifications;

            if (type == null) {
                // 유형이 null이면 모든 알림을 조회
                notifications = notificationRepository.findAllByUserEmailAndIsRead(email, isRead, pageable);
                log.debug("모든 유형에서 읽음상태별 알림 조회: 사용자={}, 읽음상태={}", email, isRead);
            } else {
                // 특정 type에 해당하는 알림만 조회
                notifications = notificationRepository.findAllByUserEmailAndTypeAndIsRead(email, type, isRead, pageable);
                log.debug("특정 유형에서 읽음상태별 알림 조회: 사용자={}, 유형={}, 읽음상태={}",
                        email, type, isRead);
            }

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

    // DTO 변환 메서드
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