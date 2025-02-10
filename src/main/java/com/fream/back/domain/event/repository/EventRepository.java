package com.fream.back.domain.event.repository;


import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.product.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByBrand(Brand brand); // 특정 브랜드의 이벤트 조회
    List<Event> findByStartDateBeforeAndEndDateAfter(LocalDateTime startDate, LocalDateTime endDate); // 활성 이벤트 조회
}