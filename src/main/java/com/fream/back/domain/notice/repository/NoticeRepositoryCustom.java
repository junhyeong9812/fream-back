package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 공지사항 레포지토리 커스텀 인터페이스
 * QueryDSL을 사용한 복잡한 검색 기능 제공
 */
public interface NoticeRepositoryCustom {

    /**
     * 제목과 내용에서 키워드로 공지사항 검색
     *
     * @param keyword 검색 키워드 (null 가능)
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    Page<Notice> searchNotices(String keyword, Pageable pageable);

    /**
     * 카테고리와 키워드로 공지사항 검색
     *
     * @param category 카테고리 (null 가능)
     * @param keyword 검색 키워드 (null 가능)
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    Page<Notice> searchNoticesByCategoryAndKeyword(NoticeCategory category, String keyword, Pageable pageable);
}