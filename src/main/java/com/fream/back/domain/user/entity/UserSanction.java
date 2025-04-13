package com.fream.back.domain.user.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_sanctions")
public class UserSanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 제재 대상 사용자

    @Column(nullable = false)
    private String reason; // 제재 사유

    private String details; // 제재 상세 내용

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SanctionType type; // 제재 유형

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SanctionStatus status; // 제재 상태

    @Column(nullable = false)
    private LocalDateTime startDate; // 제재 시작일

    private LocalDateTime endDate; // 제재 종료일 (영구 정지의 경우 null)

    private Long targetId; // 대상 컨텐츠 ID (관련 게시물, 댓글 등)
    private String targetType; // 대상 컨텐츠 타입 (STYLE, COMMENT 등)

    @Column(nullable = false)
    private String createdBy; // 등록한 관리자 이메일

    private String approvedBy; // 승인한 관리자 이메일
    private String rejectedBy; // 거부한 관리자 이메일
    private String rejectionReason; // 거부 사유

    // 연관관계 편의 메서드
    public void assignUser(User user) {
        this.user = user;
    }

    // 제재 상태 변경 메서드
    public void updateStatus(SanctionStatus status) {
        this.status = status;
    }

    // 제재 승인
    public void approve(String adminEmail) {
        this.status = SanctionStatus.ACTIVE;
        this.approvedBy = adminEmail;
    }

    // 제재 거부
    public void reject(String adminEmail, String rejectionReason) {
        this.status = SanctionStatus.REJECTED;
        this.rejectedBy = adminEmail;
        this.rejectionReason = rejectionReason;
    }

    // 제재 취소
    public void cancel() {
        this.status = SanctionStatus.CANCELED;
    }

    // 제재 만료 체크
    public boolean isExpired() {
        if (this.endDate == null) {
            return false; // 영구 정지는 만료되지 않음
        }
        return LocalDateTime.now().isAfter(this.endDate);
    }

    // 제재 만료 처리
    public void expire() {
        if (this.status == SanctionStatus.ACTIVE && isExpired()) {
            this.status = SanctionStatus.EXPIRED;
        }
    }
    /**
     * 사유 설정
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * 상세 내용 설정
     */
    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * 제재 유형 설정
     */
    public void setType(SanctionType type) {
        this.type = type;
    }

    /**
     * 시작일 설정
     */
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    /**
     * 종료일 설정
     */
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}