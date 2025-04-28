package com.fream.back.domain.notice.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 공지사항 이미지 엔티티
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_notice_image_notice_id", columnList = "notice_id")
})
public class NoticeImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // 이미지 ID

    @Column(nullable = false)
    private String imageUrl;  // 이미지 URL

    @Column(nullable = false)
    private boolean isVideo;  // 비디오 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;  // 소속 공지사항

    /**
     * 공지사항 설정
     *
     * @param notice 소속 공지사항
     */
    public void setNotice(Notice notice) {
        this.notice = notice;
    }

    /**
     * 파일 URL에서 파일 이름만 추출
     *
     * @return 파일명
     */
    public String getFileName() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "";
        }

        int lastSlashIndex = imageUrl.lastIndexOf('/');
        if (lastSlashIndex >= 0 && lastSlashIndex < imageUrl.length() - 1) {
            return imageUrl.substring(lastSlashIndex + 1);
        }
        return imageUrl;
    }

    /**
     * 파일 디렉토리 경로 추출
     *
     * @return 디렉토리 경로
     */
    public String getDirectory() {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return "";
        }

        int lastSlashIndex = imageUrl.lastIndexOf('/');
        if (lastSlashIndex > 0) {
            return imageUrl.substring(0, lastSlashIndex);
        }
        return "";
    }
}