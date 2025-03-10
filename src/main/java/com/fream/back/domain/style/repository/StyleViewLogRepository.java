package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.entity.StyleViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StyleViewLogRepository extends JpaRepository<StyleViewLog, Long> {
}