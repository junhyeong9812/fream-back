package com.fream.back.domain.inspection.repository;

import com.fream.back.domain.inspection.entity.InspectionStandardImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InspectionStandardImageRepository extends JpaRepository<InspectionStandardImage, Long> {
    List<InspectionStandardImage> findAllByInspectionStandardId(Long inspectionStandardId);
}