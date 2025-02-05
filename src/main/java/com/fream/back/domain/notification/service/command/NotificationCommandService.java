package com.fream.back.domain.notification.service.command;

import com.fream.back.domain.notification.dto.NotificationDTO;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.repository.NotificationRepository;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    // 알림 생성
    public NotificationDTO createNotification(Long userId, NotificationCategory category, NotificationType type, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Notification notification = Notification.builder()
                .category(category)
                .type(type)
                .message(message)
                .isRead(false)
                .user(user) // 사용자 연관
                .build();

        Notification savedNotification = notificationRepository.save(notification);

        // 이메일 기반 Redis 키 생성
        String email = user.getEmail();
        String redisKey = "WebSocket:User:" + email;

        // 연결된 사용자에게만 알림 전송
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            messagingTemplate.convertAndSendToUser(
                    email, // 이메일 기반으로 전송
                    "/queue/notifications",
                    toDTO(savedNotification)
            );
        }

        return toDTO(savedNotification);
    }

    //모든사용자 알림
    public List<NotificationDTO> createNotificationForAll(NotificationRequestDTO requestDTO) {
        List<User> allUsers = userRepository.findAll();
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

        // 연결된 사용자에게만 알림 전송
        for (User user : allUsers) {
            String redisKey = "WebSocket:User:" + user.getId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                messagingTemplate.convertAndSendToUser(
                        user.getId().toString(),
                        "/queue/notifications",
                        toDTOList(savedNotifications)
                );
            }
        }

        return toDTOList(savedNotifications);
    }

    // 알림 읽음 처리
    public void markAsRead(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림이 존재하지 않습니다."));

        // 알림의 사용자 이메일과 요청한 이메일이 일치하는지 확인
        if (!notification.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("해당 알림은 현재 사용자에게 속하지 않습니다.");
        }

        notification.markAsRead(); // 읽음 처리
    }

    // 알림 삭제
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
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
    //dtoList변환처리
    private List<NotificationDTO> toDTOList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    // 사용자 알림 삭제
    public void deleteNotificationsByUser(String email) {
        List<Notification> notifications = notificationRepository.findAllByUserEmail(email);
        notificationRepository.deleteAll(notifications);
    }
    public void notifyShipmentCompleted(Order order) {
        User buyer = order.getUser();
        // 예: "구매자"를 기준으로 알림 생성
        // 필요하다면 seller(판매자)에게도 알림을 보낼 수 있음

        createNotification(
                buyer.getId(),
                NotificationCategory.SHOPPING,
                NotificationType.BID,
                "[notifyShipmentCompleted] 상품이 배송 완료되었습니다. 주문 ID: " + order.getId()
        );
    }
}
