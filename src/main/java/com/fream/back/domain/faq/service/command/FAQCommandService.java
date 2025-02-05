package com.fream.back.domain.faq.service.command;


import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQImage;
import com.fream.back.domain.faq.repository.FAQImageRepository;
import com.fream.back.domain.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FAQCommandService {

    private final FAQRepository faqRepository;
    private final FAQImageRepository faqImageRepository;
    private final FAQFileStorageUtil fileStorageUtil;

    // FAQ 생성
    public FAQResponseDto createFAQ(FAQCreateRequestDto requestDto) throws IOException {
        // 1) FAQ 엔티티 생성
        FAQ faq = FAQ.builder()
                .category(requestDto.getCategory())
                .question(requestDto.getQuestion())
                .answer(requestDto.getAnswer())
                .build();

        FAQ savedFAQ = faqRepository.save(faq);

        // 2) 파일 저장
        if (fileStorageUtil.hasFiles(requestDto.getFiles())) {
            Long faqId = savedFAQ.getId();
            // "faq_{faqId}" 폴더에 파일 저장
            List<String> relativePaths = fileStorageUtil.saveFiles(requestDto.getFiles(), faqId);

            // answer 내 <img src> 경로 수정
            String updatedAnswer = fileStorageUtil.updateImagePaths(requestDto.getAnswer(), relativePaths,faqId);
            savedFAQ.update(requestDto.getCategory(), requestDto.getQuestion(), updatedAnswer);

            // DB에 이미지 엔티티 저장
            saveFAQImages(relativePaths, savedFAQ);
        }

        return toResponseDto(savedFAQ);
    }

    // FAQ 수정
    public FAQResponseDto updateFAQ(Long id, FAQUpdateRequestDto requestDto) throws IOException {
        // 1) FAQ 조회
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다."));

        // 2) 기존 이미지
        List<FAQImage> existingImages = faqImageRepository.findAllByFaqId(id);

        // 3) content 내 이미지 경로
        List<String> contentImagePaths = fileStorageUtil.extractImagePaths(requestDto.getAnswer());

        // 4) content에 없는 이미지만 삭제
        List<FAQImage> imagesToDelete = existingImages.stream()
                .filter(image -> !contentImagePaths.contains(image.getImageUrl()))
                .collect(Collectors.toList());

        fileStorageUtil.deleteFiles(imagesToDelete.stream()
                .map(FAQImage::getImageUrl)
                .collect(Collectors.toList()));
        faqImageRepository.deleteAll(imagesToDelete);

        // 5) 새 파일 저장
        if (fileStorageUtil.hasFiles(requestDto.getNewFiles())) {
            List<String> newPaths = fileStorageUtil.saveFiles(requestDto.getNewFiles(), id /*faqId*/);

            // answer 내 경로 수정
            String updatedAnswer = fileStorageUtil.updateImagePaths(requestDto.getAnswer(), newPaths,faq.getId());
            faq.update(requestDto.getCategory(), requestDto.getQuestion(), updatedAnswer);

            saveFAQImages(newPaths, faq);
        } else {
            // 새 이미지 없으면, 기존 content만 업데이트
            faq.update(requestDto.getCategory(), requestDto.getQuestion(), requestDto.getAnswer());
        }

        return toResponseDto(faq);
    }

    // FAQ 삭제
    public void deleteFAQ(Long id) throws IOException {
        FAQ faq = faqRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FAQ를 찾을 수 없습니다."));

        // 기존 이미지 삭제
        List<FAQImage> images = faqImageRepository.findAllByFaqId(id);
        fileStorageUtil.deleteFiles(images.stream().map(FAQImage::getImageUrl).collect(Collectors.toList()));

        faqImageRepository.deleteAll(images);
        faqRepository.delete(faq);
    }

    private void saveFAQImages(List<String> relativePaths, FAQ faq) {
        relativePaths.forEach(path -> {
            FAQImage image = FAQImage.builder()
                    .imageUrl(path) // "faq_{id}/파일명"
                    .faq(faq)
                    .build();
            faqImageRepository.save(image);
        });
    }

    private FAQResponseDto toResponseDto(FAQ faq) {
        List<String> imageUrls = faqImageRepository.findAllByFaqId(faq.getId())
                .stream()
                .map(FAQImage::getImageUrl)  // "faq_{id}/파일명"
                .collect(Collectors.toList());

        return FAQResponseDto.builder()
                .id(faq.getId())
                .category(faq.getCategory().name())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .imageUrls(imageUrls)
                .build();
    }
}