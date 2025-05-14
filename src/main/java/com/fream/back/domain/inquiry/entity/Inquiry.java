package com.fream.back.domain.inquiry.entity;

import com.fream.back.domain.user.entity.User;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1대1 문의 엔티티
 * 사용자가 관리자에게 보내는 1대1 문의를 관리합니다.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "inquiries")
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 문의 ID

    @Column(nullable = false, length = 100)
    private String title; // 문의 제목

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 문의 내용 (HTML 형식)

    @Column(columnDefinition = "TEXT")
    private String answer; // 관리자 답변

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 문의 작성자

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus status = InquiryStatus.REQUESTED; // 문의 상태

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InquiryCategory category; // 문의 카테고리

    @Column
    private boolean isPrivate; // 비공개 여부

    @Column
    private String answeredBy; // 답변자 정보 (관리자 ID 또는 이름)

    @Column
    private boolean pushNotification; // 답변 시 알림 수신 여부

    // 답변 설정 메서드
    public void setAnswer(String answer, String answeredBy) {
        this.answer = answer;
        this.answeredBy = answeredBy;
        this.status = InquiryStatus.ANSWERED;
    }

    // 상태 변경 메서드
    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }

    // 문의 내용 수정 메서드 (답변 전에만 가능)
    public void updateInquiry(String title, String content, InquiryCategory category, boolean isPrivate, boolean pushNotification) {
        if (this.status != InquiryStatus.ANSWERED) {
            this.title = title;
            this.content = content;
            this.category = category;
            this.isPrivate = isPrivate;
            this.pushNotification = pushNotification;
        }
    }

    /**
     * 파일 디렉토리 경로 반환
     * 문의 ID를 기반으로 파일이 저장될 디렉토리 경로 생성
     */
    public String getFileDirectory() {
        return "inquiry/" + this.id;
    }

    /**
     * 이미지 URL 경로 반환
     * 저장된 파일명을 기반으로 이미지 URL 경로 생성
     * API 경로를 사용하여 이미지를 접근할 수 있도록 함
     */
    public String getImageUrlPath(String fileName) {
        return "/api/inquiry/files/" + this.id + "/" + fileName;
    }
}