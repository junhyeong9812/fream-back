package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.NoticeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 공지사항 이미지 레포지토리
 */
public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    /**
     * 특정 공지사항에 포함된 이미지 조회
     *
     * @param noticeId 공지사항 ID
     * @return 이미지 목록
     */
    List<NoticeImage> findAllByNoticeId(Long noticeId);

    /**
     * 특정 공지사항의 이미지 중 목록에 포함되지 않은 이미지 조회
     *
     * @param noticeId 공지사항 ID
     * @param imageUrls 유지할 이미지 URL 목록
     * @return 삭제 대상 이미지 목록
     */
    @Query("SELECT ni FROM NoticeImage ni WHERE ni.notice.id = :noticeId AND ni.imageUrl NOT IN :imageUrls")
    List<NoticeImage> findImagesToDelete(@Param("noticeId") Long noticeId, @Param("imageUrls") List<String> imageUrls);

    /**
     * 특정 공지사항의 모든 이미지 삭제
     *
     * @param noticeId 공지사항 ID
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM NoticeImage ni WHERE ni.notice.id = :noticeId")
    int deleteAllByNoticeId(@Param("noticeId") Long noticeId);

    /**
     * 지정된 URL을 가진 이미지 삭제
     *
     * @param imageUrls 삭제할 이미지 URL 목록
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM NoticeImage ni WHERE ni.imageUrl IN :imageUrls")
    int deleteByImageUrls(@Param("imageUrls") List<String> imageUrls);
}