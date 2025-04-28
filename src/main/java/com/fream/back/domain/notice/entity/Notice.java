package com.fream.back.domain.notice.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 공지사항 엔티티
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_notice_category", columnList = "category"),
        @Index(name = "idx_notice_created_date", columnList = "created_date")
})
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 공지사항 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeCategory category;  // 공지사항 카테고리

    @Column(nullable = false, length = 255)
    private String title;  // 제목

    @Lob
    @Column(nullable = false)
    private String content;  // 내용

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NoticeImage> images = new ArrayList<>();  // 공지사항 이미지 목록

    /**
     * 공지사항 이미지 추가
     *
     * @param noticeImage 추가할 이미지
     */
    public void addImage(NoticeImage noticeImage) {
        this.images.add(noticeImage);
        noticeImage.setNotice(this);
    }

    /**
     * 공지사항 이미지 제거
     *
     * @param noticeImage 제거할 이미지
     */
    public void removeImage(NoticeImage noticeImage) {
        this.images.remove(noticeImage);
        noticeImage.setNotice(null);
    }

    /**
     * 공지사항 업데이트
     *
     * @param title 새 제목
     * @param content 새 내용
     * @param category 새 카테고리
     */
    public void update(String title, String content, NoticeCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    /**
     * 공지사항 내용만 업데이트
     *
     * @param content 새 내용
     */
    public void updateContent(String content) {
        this.content = content;
    }
}