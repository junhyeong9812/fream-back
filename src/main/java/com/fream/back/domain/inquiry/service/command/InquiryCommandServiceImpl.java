package com.fream.back.domain.inquiry.service.command;

import com.fream.back.domain.inquiry.dto.InquiryAnswerRequestDto;
import com.fream.back.domain.inquiry.dto.InquiryCreateRequestDto;
import com.fream.back.domain.inquiry.dto.InquiryResponseDto;
import com.fream.back.domain.inquiry.dto.InquiryUpdateRequestDto;
import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryImage;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.exception.InquiryErrorCode;
import com.fream.back.domain.inquiry.exception.InquiryException;
import com.fream.back.domain.inquiry.exception.InquiryFileException;
import com.fream.back.domain.inquiry.exception.InquiryNotFoundException;
import com.fream.back.domain.inquiry.repository.InquiryImageRepository;
import com.fream.back.domain.inquiry.repository.InquiryRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 1대1 문의 커맨드 서비스 구현체
 * 문의 생성, 수정, 삭제, 답변 등 비즈니스 로직 구현
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InquiryCommandServiceImpl implements InquiryCommandService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final UserRepository userRepository;
    private final FileUtils fileUtils;

    private static final String IMAGE_PREFIX = "img_";

    @Override
    public InquiryResponseDto createInquiry(Long userId, InquiryCreateRequestDto requestDto) {
        try {
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("ID가 " + userId + "인 사용자를 찾을 수 없습니다."));

            // 1. 문의 엔티티 저장
            Inquiry inquiry = requestDto.toEntity(user);
            Inquiry savedInquiry = inquiryRepository.save(inquiry);

            // 2. 파일 저장 및 이미지 URL 처리
            if (!CollectionUtils.isEmpty(requestDto.getFiles())) {
                String fileDirectory = savedInquiry.getFileDirectory();

                // 이미지 저장 및 HTML 내 이미지 경로 업데이트
                String updatedContent = processImagesAndUpdateHtml(savedInquiry, requestDto.getFiles(), savedInquiry.getContent(), false);

                // 업데이트된 content로 문의 내용 업데이트
                savedInquiry.updateInquiry(
                        savedInquiry.getTitle(),
                        updatedContent,
                        savedInquiry.getCategory(),
                        savedInquiry.isPrivate(),
                        savedInquiry.isPushNotification()
                );
            }

            // 3. 응답 DTO 생성
            List<InquiryImage> images = inquiryImageRepository.findAllByInquiryId(savedInquiry.getId());
            return InquiryResponseDto.from(savedInquiry, images);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("문의 생성 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_SAVE_ERROR, "문의 생성 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public InquiryResponseDto updateInquiry(Long inquiryId, Long userId, InquiryUpdateRequestDto requestDto) {
        try {
            // 1. 문의 조회 및 권한 확인
            Inquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new InquiryNotFoundException("ID가 " + inquiryId + "인 문의를 찾을 수 없습니다."));

            // 본인 문의만 수정 가능
            if (!inquiry.getUser().getId().equals(userId)) {
                throw new InquiryException(InquiryErrorCode.INQUIRY_ACCESS_DENIED, "본인이 작성한 문의만 수정할 수 있습니다.");
            }

            // 답변 완료된 문의는 수정 불가
            if (inquiry.getStatus() == InquiryStatus.ANSWERED) {
                throw new InquiryException(InquiryErrorCode.INQUIRY_ALREADY_ANSWERED, "답변이 완료된 문의는 수정할 수 없습니다.");
            }

            // 2. 이미지 처리
            // 2-1. 현재 DB에 저장된 이미지들
            List<InquiryImage> currentImages = inquiryImageRepository.findAllByInquiryIdAndIsAnswerFalse(inquiryId);

            // 2-2. 삭제할 이미지 파일 찾기 (현재 이미지 중 유지될 이미지에 없는 것들)
            List<InquiryImage> imagesToDelete = currentImages.stream()
                    .filter(image -> !requestDto.getRetainedImageUrls().contains(image.getImageUrl()))
                    .collect(Collectors.toList());

            // 2-3. 이미지 파일 삭제 및 DB 레코드 삭제
            deleteImages(inquiry, imagesToDelete);

            // 2-4. 새 이미지 저장 및 HTML 내 이미지 경로 업데이트
            String updatedContent = requestDto.getContent();
            if (!CollectionUtils.isEmpty(requestDto.getNewFiles())) {
                updatedContent = processImagesAndUpdateHtml(inquiry, requestDto.getNewFiles(), updatedContent, false);
            }

            // 3. 문의 업데이트
            inquiry.updateInquiry(
                    requestDto.getTitle(),
                    updatedContent,
                    requestDto.getCategory(),
                    requestDto.isPrivate(),
                    requestDto.isPushNotification()
            );

            // 4. 응답 DTO 생성
            List<InquiryImage> updatedImages = inquiryImageRepository.findAllByInquiryId(inquiry.getId());
            return InquiryResponseDto.from(inquiry, updatedImages);

        } catch (InquiryNotFoundException | IllegalArgumentException e) {
            throw e;
        } catch (InquiryException e) {
            throw e;
        } catch (Exception e) {
            log.error("문의 수정 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_UPDATE_ERROR, "문의 수정 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteInquiry(Long inquiryId, Long userId, boolean isAdmin) {
        try {
            // 1. 문의 조회 및 권한 확인
            Inquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new InquiryNotFoundException("ID가 " + inquiryId + "인 문의를 찾을 수 없습니다."));

            // 본인 문의 또는 관리자만 삭제 가능
            if (!isAdmin && !inquiry.getUser().getId().equals(userId)) {
                throw new InquiryException(InquiryErrorCode.INQUIRY_ACCESS_DENIED, "본인이 작성한 문의만 삭제할 수 있습니다.");
            }

            // 2. 이미지 조회
            List<InquiryImage> images = inquiryImageRepository.findAllByInquiryId(inquiryId);

            // 3. 이미지 삭제
            deleteImages(inquiry, images);

            // 4. 디렉토리 삭제
            fileUtils.deleteDirectory(inquiry.getFileDirectory());

            // 5. 문의 삭제
            inquiryRepository.delete(inquiry);

            log.info("문의 삭제 완료: ID={}", inquiryId);
        } catch (InquiryNotFoundException e) {
            throw e;
        } catch (InquiryException e) {
            throw e;
        } catch (Exception e) {
            log.error("문의 삭제 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_DELETE_ERROR, "문의 삭제 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public InquiryResponseDto updateInquiryStatus(Long inquiryId, InquiryStatus status) {
        try {
            // 1. 문의 조회
            Inquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new InquiryNotFoundException("ID가 " + inquiryId + "인 문의를 찾을 수 없습니다."));

            // 2. 상태 업데이트
            inquiry.updateStatus(status);

            // 3. 응답 DTO 생성
            List<InquiryImage> images = inquiryImageRepository.findAllByInquiryId(inquiry.getId());
            return InquiryResponseDto.from(inquiry, images);

        } catch (InquiryNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("문의 상태 변경 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_UPDATE_ERROR, "문의 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public InquiryResponseDto answerInquiry(Long inquiryId, InquiryAnswerRequestDto requestDto) {
        try {
            // 1. 문의 조회
            Inquiry inquiry = inquiryRepository.findById(inquiryId)
                    .orElseThrow(() -> new InquiryNotFoundException("ID가 " + inquiryId + "인 문의를 찾을 수 없습니다."));

            // 2. 파일 저장 및 이미지 URL 처리
            String answer = requestDto.getAnswer();
            if (!CollectionUtils.isEmpty(requestDto.getFiles())) {
                answer = processImagesAndUpdateHtml(inquiry, requestDto.getFiles(), answer, true);
            }

            // 3. 답변 설정 및 상태 변경
            inquiry.setAnswer(answer, requestDto.getAnsweredBy());

            // 4. 응답 DTO 생성
            List<InquiryImage> images = inquiryImageRepository.findAllByInquiryId(inquiry.getId());
            return InquiryResponseDto.from(inquiry, images);

        } catch (InquiryNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("문의 답변 작성 중 예상치 못한 오류: {}", e.getMessage(), e);
            throw new InquiryException(InquiryErrorCode.INQUIRY_UPDATE_ERROR, "문의 답변 작성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 파일 저장, DB 등록 및 HTML 내 이미지 경로 업데이트
     * @param inquiry 문의 엔티티
     * @param files 업로드된 이미지 파일 목록
     * @param htmlContent HTML 내용 (질문 또는 답변 본문)
     * @param isAnswer 답변 여부 (true: 답변 이미지, false: 질문 이미지)
     * @return 이미지 경로가 업데이트된 HTML 내용
     */
    private String processImagesAndUpdateHtml(Inquiry inquiry, List<MultipartFile> files, String htmlContent, boolean isAnswer) {
        if (CollectionUtils.isEmpty(files)) {
            return htmlContent;
        }

        String fileDirectory = inquiry.getFileDirectory();
        List<String> savedFileNames = new ArrayList<>();
        List<InquiryImage> savedImages = new ArrayList<>();

        // 1. 이미지 파일 저장 및 DB 등록
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            try {
                // 이미지 저장 (FileUtils 활용)
                String savedFileName = fileUtils.saveFile(fileDirectory, IMAGE_PREFIX, file);
                savedFileNames.add(savedFileName);

                // 이미지 정보 DB 저장
                InquiryImage image = InquiryImage.builder()
                        .imageUrl(savedFileName)
                        .inquiry(inquiry)
                        .originalFileName(file.getOriginalFilename())
                        .fileSize(String.valueOf(file.getSize()))
                        .isAnswer(isAnswer)
                        .build();

                InquiryImage savedImage = inquiryImageRepository.save(image);
                savedImages.add(savedImage);

                log.debug("문의 이미지 저장 완료: 문의 ID={}, 파일명={}", inquiry.getId(), savedFileName);
            } catch (Exception e) {
                log.error("문의 이미지 저장 실패: {}", e.getMessage(), e);
                throw new InquiryFileException(InquiryErrorCode.INQUIRY_FILE_SAVE_ERROR, "이미지 저장 중 오류가 발생했습니다.", e);
            }
        }

        // 2. HTML 내용 이미지 경로 업데이트
        return updateHtmlImageSources(htmlContent, inquiry, savedImages);
    }

    /**
     * HTML 내용의 이미지 태그 src 속성 업데이트
     * @param htmlContent 원본 HTML 내용
     * @param inquiry 문의 엔티티
     * @param savedImages 저장된 이미지 엔티티 목록
     * @return 이미지 src가 업데이트된 HTML 내용
     */
    private String updateHtmlImageSources(String htmlContent, Inquiry inquiry, List<InquiryImage> savedImages) {
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

                InquiryImage image = savedImages.get(imageIndex++);
                String imageUrl = inquiry.getImageUrlPath(image.getImageUrl());
                img.attr("src", imageUrl);
            }

            // 이미지 태그가 없거나, 남은 이미지가 있다면 본문 끝에 추가
            if (imgTags.isEmpty() || imageIndex < savedImages.size()) {
                Element body = doc.body();
                for (int i = imageIndex; i < savedImages.size(); i++) {
                    InquiryImage image = savedImages.get(i);
                    String imageUrl = inquiry.getImageUrlPath(image.getImageUrl());
                    body.append("<p><img src=\"" + imageUrl + "\" alt=\"첨부 이미지\"></p>");
                }
            }

            return doc.body().html();
        } catch (Exception e) {
            log.error("HTML 이미지 태그 업데이트 중 오류: {}", e.getMessage(), e);
            throw new InquiryFileException(InquiryErrorCode.INQUIRY_FILE_SAVE_ERROR,
                    "이미지 경로 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 파일 및 DB 레코드 삭제
     */
    private void deleteImages(Inquiry inquiry, List<InquiryImage> images) {
        if (CollectionUtils.isEmpty(images)) {
            return;
        }

        String fileDirectory = inquiry.getFileDirectory();

        // 이미지 파일 삭제
        for (InquiryImage image : images) {
            try {
                fileUtils.deleteFile(fileDirectory, image.getImageUrl());
                log.debug("문의 이미지 파일 삭제 완료: 문의 ID={}, 파일명={}", inquiry.getId(), image.getImageUrl());
            } catch (Exception e) {
                log.warn("문의({}) 이미지 파일 삭제 실패: {}, {}", inquiry.getId(), image.getImageUrl(), e.getMessage());
                // 파일 삭제 실패해도 계속 진행
            }
        }

        // DB에서 이미지 정보 삭제
        inquiryImageRepository.deleteAll(images);
        log.debug("문의 이미지 DB 레코드 삭제 완료: 문의 ID={}, 개수={}", inquiry.getId(), images.size());
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