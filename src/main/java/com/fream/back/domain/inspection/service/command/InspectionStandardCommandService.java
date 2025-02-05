package com.fream.back.domain.inspection.service.command;

import com.fream.back.domain.inspection.dto.InspectionStandardCreateRequestDto;
import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.dto.InspectionStandardUpdateRequestDto;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.entity.InspectionStandardImage;
import com.fream.back.domain.inspection.repository.InspectionStandardImageRepository;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InspectionStandardCommandService {
    private final InspectionStandardRepository inspectionStandardRepository;
    private final InspectionStandardImageRepository inspectionStandardImageRepository;
    private final InspectionFileStorageUtil fileStorageUtil;

    // 생성
    public InspectionStandardResponseDto createStandard(InspectionStandardCreateRequestDto requestDto) throws IOException {
        // 1) 엔티티 생성
        InspectionStandard standard = InspectionStandard.builder()
                .category(requestDto.getCategory())
                .content(requestDto.getContent())
                .build();

        // 2) 저장
        inspectionStandardRepository.save(standard);

        // 3) 파일
        if (fileStorageUtil.hasFiles(requestDto.getFiles())) {
            Long inspectionId = standard.getId();
            // "inspection_10/xxx.png"
            List<String> relativePaths = fileStorageUtil.saveFiles(requestDto.getFiles(), inspectionId);

            // HTML 치환 -> 절대 URL
            String updatedContent = fileStorageUtil.updateImagePaths(requestDto.getContent(), relativePaths, inspectionId);
            standard.update(requestDto.getCategory(), updatedContent);

            // 이미지 엔티티
            saveStandardImages(relativePaths, standard);
        }

        return toResponseDto(standard);
    }

    // 수정
    public InspectionStandardResponseDto updateStandard(Long id, InspectionStandardUpdateRequestDto dto) throws IOException {
        InspectionStandard standard = inspectionStandardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("검수 기준을 찾을 수 없습니다."));

        List<InspectionStandardImage> existingImages = inspectionStandardImageRepository.findAllByInspectionStandardId(id);

        List<String> contentImagePaths = fileStorageUtil.extractImagePaths(dto.getContent());

        // 삭제할 이미지
        List<InspectionStandardImage> imagesToDelete = existingImages.stream()
                .filter(img -> !contentImagePaths.contains(img.getImageUrl()))
                .collect(Collectors.toList());
        fileStorageUtil.deleteFiles(imagesToDelete.stream().map(InspectionStandardImage::getImageUrl).toList());
        inspectionStandardImageRepository.deleteAll(imagesToDelete);

        // 새 이미지
        if (fileStorageUtil.hasFiles(dto.getNewFiles())) {
            Long inspectionId = standard.getId();
            List<String> newPaths = fileStorageUtil.saveFiles(dto.getNewFiles(), inspectionId);

            String updatedContent = fileStorageUtil.updateImagePaths(dto.getContent(), newPaths, inspectionId);
            standard.update((dto).getCategory(), updatedContent);

            saveStandardImages(newPaths, standard);
        } else {
            standard.update(dto.getCategory(), dto.getContent());
        }

        return toResponseDto(standard);
    }

    // 삭제
    public void deleteStandard(Long id) throws IOException {
        InspectionStandard standard = inspectionStandardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("검수 기준을 찾을 수 없습니다."));

        List<InspectionStandardImage> images = inspectionStandardImageRepository.findAllByInspectionStandardId(id);
        fileStorageUtil.deleteFiles(images.stream().map(InspectionStandardImage::getImageUrl).toList());

        inspectionStandardImageRepository.deleteAll(images);
        inspectionStandardRepository.delete(standard);
    }

    private void saveStandardImages(List<String> relativePaths, InspectionStandard standard) {
        for (String path : relativePaths) {
            InspectionStandardImage image = InspectionStandardImage.builder()
                    .imageUrl(path) // "inspection_{id}/파일명"
                    .inspectionStandard(standard)
                    .build();
            inspectionStandardImageRepository.save(image);
        }
    }

    private InspectionStandardResponseDto toResponseDto(InspectionStandard standard) {
        List<String> imageUrls = inspectionStandardImageRepository
                .findAllByInspectionStandardId(standard.getId())
                .stream()
                .map(InspectionStandardImage::getImageUrl)
                .collect(Collectors.toList());

        return InspectionStandardResponseDto.builder()
                .id(standard.getId())
                .category(standard.getCategory().name())
                .content(standard.getContent())
                .imageUrls(imageUrls)
                .build();
    }
}
