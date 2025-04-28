package com.fream.back.domain.notification.repository;

import com.fream.back.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Notification 엔티티에 대한 레포지토리
 * 기본 JpaRepository 메서드와 커스텀 인터페이스의 메서드를 모두 사용 가능
 */
public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {

}