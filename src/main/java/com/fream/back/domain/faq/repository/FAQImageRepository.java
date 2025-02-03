package com.fream.back.domain.faq.repository;


import com.fream.back.domain.faq.entity.FAQImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FAQImageRepository extends JpaRepository<FAQImage, Long> {
    List<FAQImage> findAllByFaqId(Long faqId);
}
