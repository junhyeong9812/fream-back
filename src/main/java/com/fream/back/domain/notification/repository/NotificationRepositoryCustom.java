package com.fream.back.domain.notification.repository;

import com.fream.back.domain.notification.entity.Notification;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * QueryDSL을 활용한 Notification Repository 커스텀 인터페이스
 */
public interface NotificationRepositoryCustom {

    /**
     * 사용자 이메일과 카테고리로 알림 목록 조회 (최신순)
     */
    List<Notification> findByUserEmailAndCategory(String email, NotificationCategory category);

    /**
     * 사용자 이메일과 알림 유형으로 알림 목록 조회 (최신순)
     */
    List<Notification> findByUserEmailAndType(String email, NotificationType type);

    /**
     * 사용자 이메일, 읽음 상태로 알림 목록 조회 (페이징)
     */
    Page<Notification> findByUserEmailAndIsRead(String email, boolean isRead, Pageable pageable);

    /**
     * 사용자 이메일, 카테고리, 읽음 상태로 알림 목록 조회 (페이징)
     * category가 null이면 카테고리 필터링을 하지 않음
     */
    Page<Notification> findByUserEmailAndCategoryAndIsRead(
            String email, NotificationCategory category, boolean isRead, Pageable pageable);

    /**
     * 사용자 이메일, 알림 유형, 읽음 상태로 알림 목록 조회 (페이징)
     * type이 null이면 유형 필터링을 하지 않음
     */
    Page<Notification> findByUserEmailAndTypeAndIsRead(
            String email, NotificationType type, boolean isRead, Pageable pageable);

    /**
     * 사용자 이메일로 모든 알림 조회
     */
    List<Notification> findAllByUserEmail(String email);
}