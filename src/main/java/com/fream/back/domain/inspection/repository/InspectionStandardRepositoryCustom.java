package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionStandard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InspectionStandardRepositoryCustom {
    Page<InspectionStandard> searchStandards(String keyword, Pageable pageable);
}