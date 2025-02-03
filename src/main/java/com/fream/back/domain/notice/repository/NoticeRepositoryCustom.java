package com.fream.back.domain.notice.repository;

import com.fream.back.domain.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeRepositoryCustom {
    Page<Notice> searchNotices(String keyword, Pageable pageable);
}
