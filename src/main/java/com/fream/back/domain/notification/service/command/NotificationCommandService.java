package com.fream.back.domain.notification.service.command;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.exception.NotificationErrorCode;
import com.fream.back.domain.notification.exception.NotificationException;
import com.fream.back.domain.notification.repository.NotificationRepository;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "WebSocket:User:";
    private static final String WEBSOCKET_DESTINATION = "/queue/notifications";
    private static final int REDIS_TTL_MINUTES = 30;

    /**
     * 단일 사용자 알림 생성
     */
    public NotificationDTO createNotification(Long userId, NotificationCategory category, NotificationType type, String message) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotificationException(
                            NotificationErrorCode.NOTIFICATION_USER_NOT_FOUND,
                            "사용자 ID: " + userId + "를 찾을 수 없습니다."
                    ));

            Notification notification = buildNotification(user, category, type, message);
            Notification savedNotification = notificationRepository.save(notification);
            log.info("알림 생성 완료: 사용자={}, 카테고리={}, 타입={}", user.getEmail(), category, type);

            // 실시간 알림 전송 시도
            sendRealTimeNotification(user.getEmail(), toDTO(savedNotification));

            return toDTO(savedNotification);
        } catch (NotificationException e) {
            throw e; // 이미 NotificationException이면 그대로 전파
        } catch (Exception e) {
            log.error("알림 생성 중 오류 발생: 사용자ID={}, 카테고리={}, 타입={}, 오류={}",
                    userId, category, type, e.getMessage(), e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CREATION_FAILED, e);
        }
    }

    /**
     * 모든 사용자 알림 생성
     */
    public List<NotificationDTO> createNotificationForAll(NotificationRequestDTO requestDTO) {
        try {
            List<User> allUsers = userRepository.findAll();
            if (allUsers.isEmpty()) {
                log.warn("알림 대상 사용자가 없습니다.");
                return List.of();
            }

            log.info("전체 사용자 알림 생성 시작: 카테고리={}, 타입={}, 대상 사용자 수={}",
                    requestDTO.getCategory(), requestDTO.getType(), allUsers.size());

            List<Notification> notifications = createNotificationsForUsers(
                    allUsers, requestDTO.getCategory(), requestDTO.getType(), requestDTO.getMessage());

            List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
            log.info("전체 사용자 알림 생성 완료: 카테고리={}, 타입={}, 생성된 알림 수={}",
                    requestDTO.getCategory(), requestDTO.getType(), savedNotifications.size());

            // 연결된 사용자에게만 실시간 알림 전송
            broadcastNotificationsToConnectedUsers(allUsers, savedNotifications);

            return toDTOList(savedNotifications);
        } catch (Exception e) {
            log.error("전체 사용자 알림 생성 중 오류 발생: 카테고리={}, 타입={}, 오류={}",
                    requestDTO.getCategory(), requestDTO.getType(), e.getMessage(), e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CREATION_FAILED, e);
        }
    }

    /**
     * 알림 읽음 처리
     */
    public void markAsRead(Long notificationId, String email) {
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new NotificationException(
                            NotificationErrorCode.NOTIFICATION_NOT_FOUND,
                            "알림 ID: " + notificationId + "를 찾을 수 없습니다."
                    ));

            // 알림의 사용자 이메일과 요청한 이메일이 일치하는지 확인
            if (!notification.getUser().getEmail().equals(email)) {
                log.warn("알림 읽음 처리 권한 없음: 알림ID={}, 요청자={}, 소유자={}",
                        notificationId, email, notification.getUser().getEmail());
                throw new NotificationException(
                        NotificationErrorCode.NOTIFICATION_ACCESS_DENIED,
                        "해당 알림은 현재 사용자에게 속하지 않습니다."
                );
            }

            notification.markAsRead(); // 읽음 처리
            log.info("알림 읽음 처리 완료: 알림ID={}, 사용자={}", notificationId, email);
        } catch (NotificationException e) {
            throw e; // 이미 NotificationException이면 그대로 전파
        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류 발생: 알림ID={}, 사용자={}, 오류={}",
                    notificationId, email, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_READ_FAILED,
                    "알림 읽음 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 모든 알림 읽음 처리 (추가된 기능)
     */
    public void markAllAsRead(String email) {
        try {
            List<Notification> notifications = notificationRepository.findAllByUserEmail(email);
            if (notifications.isEmpty()) {
                log.info("읽음 처리할 알림이 없습니다: 사용자={}", email);
                return;
            }

            for (Notification notification : notifications) {
                if (!notification.isRead()) {
                    notification.markAsRead();
                }
            }

            log.info("모든 알림 읽음 처리 완료: 사용자={}, 처리된 알림 수={}", email, notifications.size());
        } catch (Exception e) {
            log.error("알림 읽음 처리 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_READ_FAILED,
                    "알림 읽음 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 알림 삭제
     */
    public void deleteNotification(Long notificationId) {
        try {
            // 삭제 전에 알림이 존재하는지 확인
            boolean exists = notificationRepository.existsById(notificationId);
            if (!exists) {
                throw new NotificationException(
                        NotificationErrorCode.NOTIFICATION_NOT_FOUND,
                        "삭제할 알림을 찾을 수 없습니다. ID: " + notificationId
                );
            }

            notificationRepository.deleteById(notificationId);
            log.info("알림 삭제 완료: 알림ID={}", notificationId);
        } catch (NotificationException e) {
            throw e; // 이미 NotificationException이면 그대로 전파
        } catch (Exception e) {
            log.error("알림 삭제 중 오류 발생: 알림ID={}, 오류={}", notificationId, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_DELETE_FAILED,
                    "알림 삭제 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 사용자 알림 전체 삭제
     */
    public void deleteNotificationsByUser(String email) {
        try {
            List<Notification> notifications = notificationRepository.findAllByUserEmail(email);
            if (notifications.isEmpty()) {
                log.info("삭제할 알림이 없습니다: 사용자={}", email);
                return;
            }

            notificationRepository.deleteAll(notifications);
            log.info("사용자 알림 전체 삭제 완료: 사용자={}, 삭제된 알림 수={}", email, notifications.size());
        } catch (Exception e) {
            log.error("사용자 알림 삭제 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_DELETE_FAILED,
                    "사용자 알림 삭제 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 배송 완료 알림 생성
     */
    public void notifyShipmentCompleted(Order order) {
        try {
            User buyer = order.getUser();
            log.info("배송 완료 알림 생성: 주문ID={}, 사용자ID={}", order.getId(), buyer.getId());

            createNotification(
                    buyer.getId(),
                    NotificationCategory.SHOPPING,
                    NotificationType.BID,
                    "[배송 완료] 상품이 배송 완료되었습니다. 주문 ID: " + order.getId()
            );
        } catch (Exception e) {
            // 비즈니스 로직 중단을 방지하기 위해 예외를 로깅만 함
            log.error("배송 완료 알림 생성 중 오류 발생: 주문ID={}, 오류={}", order.getId(), e.getMessage(), e);
        }
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * Notification 엔티티 생성
     */
    private Notification buildNotification(User user, NotificationCategory category, NotificationType type, String message) {
        return Notification.builder()
                .category(category)
                .type(type)
                .message(message)
                .isRead(false)
                .user(user)
                .build();
    }

    /**
     * 여러 사용자를 위한 알림 엔티티 생성
     */
    private List<Notification> createNotificationsForUsers(
            List<User> users, NotificationCategory category, NotificationType type, String message) {
        List<Notification> notifications = new ArrayList<>();

        for (User user : users) {
            Notification notification = buildNotification(user, category, type, message);
            notifications.add(notification);
        }

        return notifications;
    }

    /**
     * 연결된 사용자들에게 실시간 알림 브로드캐스트
     */
    private void broadcastNotificationsToConnectedUsers(List<User> users, List<Notification> notifications) {
        int deliveredCount = 0;

        for (User user : users) {
            String redisKey = REDIS_KEY_PREFIX + user.getEmail();

            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                try {
                    // 해당 사용자의 알림만 필터링
                    List<NotificationDTO> userNotifications = notifications.stream()
                            .filter(notification -> notification.getUser().getId().equals(user.getId()))
                            .map(this::toDTO)
                            .collect(Collectors.toList());

                    // 실시간 알림 전송
                    messagingTemplate.convertAndSendToUser(
                            user.getEmail(),
                            WEBSOCKET_DESTINATION,
                            userNotifications
                    );
                    deliveredCount++;
                } catch (Exception e) {
                    log.warn("사용자 알림 실시간 전송 실패: 사용자={}, 오류={}", user.getEmail(), e.getMessage());
                    // 개별 전송 실패는 무시하고 계속 진행
                }
            }
        }

        log.info("실시간 알림 전송 완료: 총 {}명 중 {}명에게 전송됨", users.size(), deliveredCount);
    }

    /**
     * 단일 사용자에게 실시간 알림 전송
     */
    private void sendRealTimeNotification(String email, NotificationDTO notification) {
        String redisKey = REDIS_KEY_PREFIX + email;

        // 연결된 사용자에게만 알림 전송
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            try {
                messagingTemplate.convertAndSendToUser(
                        email,
                        WEBSOCKET_DESTINATION,
                        notification
                );
                log.debug("알림 실시간 전송 완료: 사용자={}, 알림ID={}", email, notification.getId());
            } catch (Exception e) {
                log.warn("알림 실시간 전송 실패: 사용자={}, 알림ID={}, 오류={}",
                        email, notification.getId(), e.getMessage());
                // 실시간 전송 실패해도 알림은 이미 저장되었으므로 예외 발생시키지 않음
            }
        } else {
            log.debug("사용자 WebSocket 연결 없음: 사용자={}, 알림은 저장됨", email);
        }
    }

    /**
     * WebSocket 연결 유지를 위한 Redis TTL 업데이트
     */
    public void updateWebSocketConnectionTTL(String email) {
        try {
            if (email != null) {
                String redisKey = REDIS_KEY_PREFIX + email;

                // 남은 TTL(초 단위)
                Long remainingTime = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

                // TTL이 10분 이하인 경우만 30분으로 갱신
                if (remainingTime != null && remainingTime <= 600) {
                    redisTemplate.expire(redisKey, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
                    log.debug("Redis TTL 갱신: 사용자={}, 남은TTL={}초", email, remainingTime);
                } else {
                    log.debug("TTL 연장 불필요: 사용자={}, 남은TTL={}초", email, remainingTime);
                }
            }
        } catch (Exception e) {
            log.error("WebSocket 연결 TTL 갱신 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.WEBSOCKET_CONNECTION_MAINTENANCE_FAILED,
                    "WebSocket 연결 유지 중 오류가 발생했습니다.",
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

    /**
     * 알림 목록을 DTO 목록으로 변환
     */
    private List<NotificationDTO> toDTOList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}