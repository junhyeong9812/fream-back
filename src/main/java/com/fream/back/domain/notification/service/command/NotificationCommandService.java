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

    // 알림 생성
    public NotificationDTO createNotification(Long userId, NotificationCategory category, NotificationType type, String message) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotificationException(
                            NotificationErrorCode.NOTIFICATION_USER_NOT_FOUND,
                            "사용자 ID: " + userId + "를 찾을 수 없습니다."
                    ));

            Notification notification = Notification.builder()
                    .category(category)
                    .type(type)
                    .message(message)
                    .isRead(false)
                    .user(user) // 사용자 연관
                    .build();

            Notification savedNotification = notificationRepository.save(notification);
            log.info("알림 생성 완료: 사용자={}, 카테고리={}, 타입={}", user.getEmail(), category, type);

            // 이메일 기반 Redis 키 생성
            String email = user.getEmail();
            String redisKey = "WebSocket:User:" + email;

            // 연결된 사용자에게만 알림 전송
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            email, // 이메일 기반으로 전송
                            "/queue/notifications",
                            toDTO(savedNotification)
                    );
                    log.debug("알림 실시간 전송 완료: 사용자={}, 알림ID={}", email, savedNotification.getId());
                } catch (Exception e) {
                    log.warn("알림 실시간 전송 실패: 사용자={}, 알림ID={}, 오류={}", email, savedNotification.getId(), e.getMessage());
                    // 실시간 전송 실패해도 알림은 이미 저장되었으므로 예외 발생시키지 않음
                }
            } else {
                log.debug("사용자 WebSocket 연결 없음: 사용자={}, 알림은 저장됨", email);
            }

            return toDTO(savedNotification);
        } catch (NotificationException e) {
            throw e; // 이미 NotificationException이면 그대로 전파
        } catch (Exception e) {
            log.error("알림 생성 중 오류 발생: 사용자ID={}, 카테고리={}, 타입={}, 오류={}", userId, category, type, e.getMessage(), e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CREATION_FAILED, e);
        }
    }

    // 모든 사용자 알림
    public List<NotificationDTO> createNotificationForAll(NotificationRequestDTO requestDTO) {
        try {
            List<User> allUsers = userRepository.findAll();
            if (allUsers.isEmpty()) {
                log.warn("알림 대상 사용자가 없습니다.");
                return List.of();
            }

            log.info("전체 사용자 알림 생성 시작: 카테고리={}, 타입={}, 대상 사용자 수={}",
                    requestDTO.getCategory(), requestDTO.getType(), allUsers.size());

            List<Notification> notifications = new ArrayList<>();

            for (User user : allUsers) {
                Notification notification = Notification.builder()
                        .category(requestDTO.getCategory())
                        .type(requestDTO.getType())
                        .message(requestDTO.getMessage())
                        .isRead(false)
                        .user(user) // 모든 사용자와 연관
                        .build();
                notifications.add(notification);
            }

            List<Notification> savedNotifications = notificationRepository.saveAll(notifications);
            log.info("전체 사용자 알림 생성 완료: 카테고리={}, 타입={}, 생성된 알림 수={}",
                    requestDTO.getCategory(), requestDTO.getType(), savedNotifications.size());

            // 연결된 사용자에게만 알림 전송
            int deliveredCount = 0;
            for (User user : allUsers) {
                String redisKey = "WebSocket:User:" + user.getEmail();
                if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                    try {
                        // 해당 사용자에게 할당된 알림만 찾아서 전송
                        List<NotificationDTO> userNotifications = savedNotifications.stream()
                                .filter(notification -> notification.getUser().getId().equals(user.getId()))
                                .map(this::toDTO)
                                .collect(Collectors.toList());

                        messagingTemplate.convertAndSendToUser(
                                user.getEmail(),
                                "/queue/notifications",
                                userNotifications
                        );
                        deliveredCount++;
                    } catch (Exception e) {
                        log.warn("사용자 알림 실시간 전송 실패: 사용자={}, 오류={}", user.getEmail(), e.getMessage());
                        // 개별 전송 실패는 무시하고 계속 진행
                    }
                }
            }

            log.info("실시간 알림 전송 완료: 총 {}명 중 {}명에게 전송됨", allUsers.size(), deliveredCount);
            return toDTOList(savedNotifications);
        } catch (Exception e) {
            log.error("전체 사용자 알림 생성 중 오류 발생: 카테고리={}, 타입={}, 오류={}",
                    requestDTO.getCategory(), requestDTO.getType(), e.getMessage(), e);
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_CREATION_FAILED, e);
        }
    }

    // 알림 읽음 처리
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
            log.error("알림 읽음 처리 중 오류 발생: 알림ID={}, 사용자={}, 오류={}", notificationId, email, e.getMessage(), e);
            throw new NotificationException(
                    NotificationErrorCode.NOTIFICATION_READ_FAILED,
                    "알림 읽음 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    // 알림 삭제
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

    // dtoList변환처리
    private List<NotificationDTO> toDTOList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // 사용자 알림 삭제
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

    public void notifyShipmentCompleted(Order order) {
        try {
            User buyer = order.getUser();
            log.info("배송 완료 알림 생성: 주문ID={}, 사용자ID={}", order.getId(), buyer.getId());

            // 예: "구매자"를 기준으로 알림 생성
            // 필요하다면 seller(판매자)에게도 알림을 보낼 수 있음
            createNotification(
                    buyer.getId(),
                    NotificationCategory.SHOPPING,
                    NotificationType.BID,
                    "[배송 완료] 상품이 배송 완료되었습니다. 주문 ID: " + order.getId()
            );
        } catch (Exception e) {
            log.error("배송 완료 알림 생성 중 오류 발생: 주문ID={}, 오류={}", order.getId(), e.getMessage(), e);
            // 알림 생성 실패가 비즈니스 로직을 중단시키지 않도록 예외를 로깅만 하고 전파하지 않음
        }
    }
}