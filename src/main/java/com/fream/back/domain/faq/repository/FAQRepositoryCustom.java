package com.fream.back.domain.faq.repository;

import com.fream.back.domain.faq.entity.FAQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FAQRepositoryCustom {
    Page<FAQ> searchFAQs(String keyword, Pageable pageable);
}
