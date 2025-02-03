package com.fream.back.domain.notice.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 공지사항 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NoticeCategory category; // 공지사항 카테고리 (ENUM)

    @Column(nullable = false)
    private String title; // 제목

    @Lob
    @Column(nullable = false)
    private String content; // 내용

    /**
     * 공지사항 업데이트 메서드
     * @param title   새로운 제목
     * @param content 새로운 내용
     * @param category 새로운 카테고리
     */
    public void update(String title, String content, NoticeCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }
    public void updateContent(String updatedContent) {
        this.content = updatedContent;
    }
}
