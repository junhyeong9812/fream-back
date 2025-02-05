package com.fream.back.domain.inspection.service.query;


import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.repository.InspectionStandardImageRepository;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InspectionStandardQueryService {

    private final InspectionStandardRepository inspectionStandardRepository;
    private final InspectionStandardImageRepository inspectionStandardImageRepository;

    public Page<InspectionStandardResponseDto> getStandards(Pageable pageable) {
        return inspectionStandardRepository.findAll(pageable).map(this::toResponseDto);
    }

    public Page<InspectionStandardResponseDto> getStandardsByCategory(InspectionCategory category, Pageable pageable) {
        return inspectionStandardRepository.findByCategory(category, pageable)
                .map(this::toResponseDto);
    }

    public Page<InspectionStandardResponseDto> searchStandards(String keyword, Pageable pageable) {
        return inspectionStandardRepository.searchStandards(keyword, pageable).map(this::toResponseDto);
    }

    public InspectionStandardResponseDto getStandard(Long id) {
        InspectionStandard standard = inspectionStandardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("검수 기준을 찾을 수 없습니다."));
        return toResponseDto(standard);
    }

    private InspectionStandardResponseDto toResponseDto(InspectionStandard standard) {
        return InspectionStandardResponseDto.builder()
                .id(standard.getId())
                .category(standard.getCategory().name())
                .content(standard.getContent())
                .imageUrls(inspectionStandardImageRepository.findAllByInspectionStandardId(standard.getId())
                        .stream()
                        .map(image -> image.getImageUrl())
                        .toList())
                .build();
    }
}
