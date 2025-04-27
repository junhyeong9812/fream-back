package com.fream.back.domain.faq.service.command;

import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQImage;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.exception.FAQFileException;
import com.fream.back.domain.faq.exception.FAQNotFoundException;
import com.fream.back.domain.faq.repository.FAQImageRepository;
import com.fream.back.domain.faq.repository.FAQRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FAQCommandService {

    private final FAQRepository faqRepository;
    private final FAQImageRepository faqImageRepository;
    private final FileUtils fileUtils;

    private static final String IMAGE_PREFIX = "img_";

    /**
     * FAQ 생성 (캐시 전부 삭제)
     */
    @Caching(evict = {
            @CacheEvict(value = "faqList", allEntries = true),
            @CacheEvict(value = "faqCategoryList", allEntries = true),
            @CacheEvict(value = "faqSearchResults", allEntries = true)
    })
    public FAQResponseDto createFAQ(FAQCreateRequestDto requestDto) {
        try {
            // 1. FAQ 엔티티 저장
            FAQ faq = FAQ.builder()
                    .category(requestDto.getCategory())
                    .question(requestDto.getQuestion())
                    .answer(requestDto.getAnswer())
                    .build();

            FAQ savedFAQ = faqRepository.save(faq);

            // 2. 파일 저장 및 이미지 URL 처리
            if (!CollectionUtils.isEmpty(requestDto.getFiles())) {
                String fileDirectory = savedFAQ.getFileDirectory();

                // 이미지 저장 및 HTML 내 이미지 경로 업데이트
                String updatedAnswer = processImagesAndUpdateHtml(savedFAQ, requestDto.getFiles(), savedFAQ.getAnswer());

                // 업데이트된 answer로 FAQ 업데이트
                savedFAQ.update(savedFAQ.getCategory(), savedFAQ.getQuestion(), updatedAnswer);
            }

            // 3. 응답 DTO 생성
            List<FAQImage> images = faqImageRepository.findAllByFaqId(savedFAQ.getId());
            return FAQResponseDto.from(savedFAQ, images);

        } catch (Exception e) {
            log.error("FAQ 생성 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new FAQException(FAQErrorCode.FAQ_SAVE_ERROR, "FAQ 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * FAQ 수정 (특정 FAQ 캐시 및 목록 캐시 삭제)
     */
    @Caching(evict = {
            @CacheEvict(value = "faqDetail", key = "#id"),
            @CacheEvict(value = "faqList", allEntries = true),
            @CacheEvict(value = "faqCategoryList", allEntries = true),
            @CacheEvict(value = "faqSearchResults", allEntries = true)
    })
    public FAQResponseDto updateFAQ(Long id, FAQUpdateRequestDto requestDto) {
        try {
            // 1. FAQ 조회
            FAQ faq = faqRepository.findById(id)
                    .orElseThrow(() -> new FAQNotFoundException("ID가 " + id + "인 FAQ를 찾을 수 없습니다."));

            // 2. 이미지 처리
            // 2-1. 현재 DB에 저장된 이미지들
            List<FAQImage> currentImages = faqImageRepository.findAllByFaqId(id);

            // 2-2. 삭제할 이미지 파일 찾기 (현재 이미지 중 유지될 이미지에 없는 것들)
            List<FAQImage> imagesToDelete = currentImages.stream()
                    .filter(image -> !requestDto.getRetainedImageUrls().contains(image.getImageUrl()))
                    .collect(Collectors.toList());

            // 2-3. 이미지 파일 삭제 및 DB 레코드 삭제
            deleteImages(faq, imagesToDelete);

            // 2-4. 새 이미지 저장 및 HTML 내 이미지 경로 업데이트
            String updatedAnswer = requestDto.getAnswer();
            if (!CollectionUtils.isEmpty(requestDto.getNewFiles())) {
                updatedAnswer = processImagesAndUpdateHtml(faq, requestDto.getNewFiles(), updatedAnswer);
            }

            // 3. FAQ 업데이트
            faq.update(requestDto.getCategory(), requestDto.getQuestion(), updatedAnswer);

            // 4. 응답 DTO 생성
            List<FAQImage> updatedImages = faqImageRepository.findAllByFaqId(faq.getId());
            return FAQResponseDto.from(faq, updatedImages);

        } catch (FAQNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("FAQ 수정 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new FAQException(FAQErrorCode.FAQ_UPDATE_ERROR, "FAQ 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * FAQ 삭제 (특정 FAQ 캐시 및 목록 캐시 삭제)
     */
    @Caching(evict = {
            @CacheEvict(value = "faqDetail", key = "#id"),
            @CacheEvict(value = "faqList", allEntries = true),
            @CacheEvict(value = "faqCategoryList", allEntries = true),
            @CacheEvict(value = "faqSearchResults", allEntries = true)
    })
    public void deleteFAQ(Long id) {
        try {
            // 1. FAQ 조회
            FAQ faq = faqRepository.findById(id)
                    .orElseThrow(() -> new FAQNotFoundException("ID가 " + id + "인 FAQ를 찾을 수 없습니다."));

            // 2. 이미지 조회
            List<FAQImage> images = faqImageRepository.findAllByFaqId(id);

            // 3. 이미지 삭제
            deleteImages(faq, images);

            // 4. 디렉토리 삭제
            fileUtils.deleteDirectory(faq.getFileDirectory());

            // 5. FAQ 삭제
            faqRepository.delete(faq);

            log.info("FAQ 삭제 완료: ID={}", id);
        } catch (FAQNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("FAQ 삭제 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new FAQException(FAQErrorCode.FAQ_DELETE_ERROR, "FAQ 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 파일 저장, DB 등록 및 HTML 내 이미지 경로 업데이트
     * @param faq FAQ 엔티티
     * @param files 업로드된 이미지 파일 목록
     * @param htmlContent HTML 내용 (답변 본문)
     * @return 이미지 경로가 업데이트된 HTML 내용
     */
    private String processImagesAndUpdateHtml(FAQ faq, List<MultipartFile> files, String htmlContent) {
        if (CollectionUtils.isEmpty(files)) {
            return htmlContent;
        }

        String fileDirectory = faq.getFileDirectory();
        List<String> savedFileNames = new ArrayList<>();
        List<FAQImage> savedImages = new ArrayList<>();

        // 1. 이미지 파일 저장 및 DB 등록
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            try {
                // 이미지 저장 (FileUtils 활용)
                String savedFileName = fileUtils.saveFile(fileDirectory, IMAGE_PREFIX, file);
                savedFileNames.add(savedFileName);

                // 이미지 정보 DB 저장
                FAQImage image = FAQImage.builder()
                        .imageUrl(savedFileName)
                        .faq(faq)
                        .build();

                FAQImage savedImage = faqImageRepository.save(image);
                savedImages.add(savedImage);

                log.debug("FAQ 이미지 저장 완료: FAQ ID={}, 파일명={}", faq.getId(), savedFileName);
            } catch (Exception e) {
                log.error("FAQ 이미지 저장 실패: {}", e.getMessage(), e);
                throw e;  // 상위 메서드에서 처리
            }
        }

        // 2. HTML 내용 이미지 경로 업데이트
        return updateHtmlImageSources(htmlContent, faq, savedImages);
    }

    /**
     * HTML 내용의 이미지 태그 src 속성 업데이트
     * @param htmlContent 원본 HTML 내용
     * @param faq FAQ 엔티티
     * @param savedImages 저장된 이미지 엔티티 목록
     * @return 이미지 src가 업데이트된 HTML 내용
     */
    private String updateHtmlImageSources(String htmlContent, FAQ faq, List<FAQImage> savedImages) {
        if (savedImages.isEmpty() || htmlContent == null || htmlContent.isEmpty()) {
            return htmlContent;
        }

        try {
            // HTML 파싱
            Document doc = Jsoup.parse(htmlContent);

            // 이미지 태그 찾기 (data:image 형식으로 된 것들)
            Elements imgTags = doc.select("img[src^='data:image']");

            // 데이터 URL을 실제 저장된 이미지 URL로 변경
            int imageIndex = 0;
            for (Element img : imgTags) {
                if (imageIndex >= savedImages.size()) break;

                FAQImage image = savedImages.get(imageIndex++);
                String imageUrl = faq.getImageUrlPath(image.getImageUrl());
                img.attr("src", imageUrl);
            }

            // 이미지 태그가 없거나, 남은 이미지가 있다면 본문 끝에 추가
            if (imgTags.isEmpty() || imageIndex < savedImages.size()) {
                Element body = doc.body();
                for (int i = imageIndex; i < savedImages.size(); i++) {
                    FAQImage image = savedImages.get(i);
                    String imageUrl = faq.getImageUrlPath(image.getImageUrl());
                    body.append("<p><img src=\"" + imageUrl + "\" alt=\"첨부 이미지\"></p>");
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            log.error("HTML 이미지 태그 업데이트 중 오류: {}", e.getMessage(), e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "이미지 경로 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 파일 및 DB 레코드 삭제
     */
    private void deleteImages(FAQ faq, List<FAQImage> images) {
        if (CollectionUtils.isEmpty(images)) {
            return;
        }

        String fileDirectory = faq.getFileDirectory();

        // 이미지 파일 삭제
        for (FAQImage image : images) {
            try {
                fileUtils.deleteFile(fileDirectory, image.getImageUrl());
                log.debug("FAQ 이미지 파일 삭제 완료: FAQ ID={}, 파일명={}", faq.getId(), image.getImageUrl());
            } catch (Exception e) {
                log.warn("FAQ({}) 이미지 파일 삭제 실패: {}, {}", faq.getId(), image.getImageUrl(), e.getMessage());
                // 파일 삭제 실패해도 계속 진행
            }
        }

        // DB에서 이미지 정보 삭제
        faqImageRepository.deleteAll(images);
        log.debug("FAQ 이미지 DB 레코드 삭제 완료: FAQ ID={}, 개수={}", faq.getId(), images.size());
    }

    /**
     * HTML 내용에서 이미지 URL 추출
     */
    private List<String> extractImageUrlsFromHtml(String htmlContent) {
        List<String> imageUrls = new ArrayList<>();

        if (htmlContent == null || htmlContent.isEmpty()) {
            return imageUrls;
        }

        try {
            Document doc = Jsoup.parse(htmlContent);
            Elements imgTags = doc.select("img[src]");

            for (Element img : imgTags) {
                String src = img.attr("src");
                if (src != null && !src.isEmpty() && !src.startsWith("data:")) {
                    // 이미 저장된 이미지 URL만 추출 (data:image 형식 제외)
                    imageUrls.add(src);
                }
            }
        } catch (Exception e) {
            log.warn("HTML에서 이미지 URL 추출 중 오류: {}", e.getMessage());
            // 오류가 발생해도 빈 리스트 반환
        }

        return imageUrls;
    }
}