package com.fream.back.domain.faq.service.command;

import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQResponseDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import com.fream.back.domain.faq.entity.FAQ;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.entity.FAQImage;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.exception.FAQFileException;
import com.fream.back.domain.faq.exception.FAQNotFoundException;
import com.fream.back.domain.faq.repository.FAQImageRepository;
import com.fream.back.domain.faq.repository.FAQRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FAQCommandService {

    private final FAQRepository faqRepository;
    private final FAQImageRepository faqImageRepository;
    private final FAQFileStorageUtil fileStorageUtil;

    // FAQ 생성
    public FAQResponseDto createFAQ(FAQCreateRequestDto requestDto) throws IOException {
        if (requestDto == null) {
            throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "FAQ 데이터가 필요합니다.");
        }

        try {
            // 요청 데이터 검증
            validateFAQData(requestDto.getCategory(), requestDto.getQuestion(), requestDto.getAnswer());

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
                List<String> relativePaths;
                try {
                    // "faq_{faqId}" 폴더에 파일 저장
                    relativePaths = fileStorageUtil.saveFiles(requestDto.getFiles(), faqId);
                } catch (FAQFileException e) {
                    log.error("FAQ 생성 중 파일 저장 실패: ", e);
                    throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                            "파일 저장에 실패했습니다. 이미지 형식을 확인하고 다시 시도해주세요.", e);
                }

                // answer 내 <img src> 경로 수정
                String updatedAnswer = fileStorageUtil.updateImagePaths(requestDto.getAnswer(), relativePaths, faqId);
                savedFAQ.update(requestDto.getCategory(), requestDto.getQuestion(), updatedAnswer);

                // DB에 이미지 엔티티 저장
                saveFAQImages(relativePaths, savedFAQ);
            }

            return toResponseDto(savedFAQ);
        } catch (FAQNotFoundException e) {
            // 이미 정의된 예외는 로깅 후 그대로 전파
            log.warn("FAQ를 찾을 수 없음 예외 발생: {}", e.getMessage());
            throw e;
        } catch (FAQFileException e) {
            log.error("FAQ 파일 처리 예외 발생: {}", e.getMessage());
            throw e;
        } catch (FAQException e) {
            log.error("FAQ 예외 발생: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("FAQ 저장 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_SAVE_ERROR,
                    "FAQ를 저장하는 중 데이터베이스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (IOException e) {
            log.error("FAQ 파일 처리 중 IO 오류 발생: ", e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "파일 업로드 중 오류가 발생했습니다. 파일 크기와 형식을 확인해주세요.", e);
        } catch (Exception e) {
            log.error("FAQ 생성 중 예상치 못한 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_SAVE_ERROR,
                    "FAQ 생성 중 오류가 발생했습니다. 관리자에게 문의해주세요.", e);
        }
    }

    // FAQ 수정
    public FAQResponseDto updateFAQ(Long id, FAQUpdateRequestDto requestDto) throws IOException {
        if (id == null) {
            throw new FAQNotFoundException("FAQ ID가 제공되지 않았습니다.");
        }

        if (requestDto == null) {
            throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "수정할 FAQ 데이터가 필요합니다.");
        }

        try {
            // 요청 데이터 검증
            validateFAQData(requestDto.getCategory(), requestDto.getQuestion(), requestDto.getAnswer());

            // 1) FAQ 조회
            FAQ faq = faqRepository.findById(id)
                    .orElseThrow(() -> new FAQNotFoundException("ID가 " + id + "인 FAQ를 찾을 수 없습니다."));

            // 2) 기존 이미지
            List<FAQImage> existingImages = faqImageRepository.findAllByFaqId(id);

            // 3) content 내 이미지 경로
            List<String> contentImagePaths = fileStorageUtil.extractImagePaths(requestDto.getAnswer());

            // 4) content에 없는 이미지만 삭제
            List<FAQImage> imagesToDelete = existingImages.stream()
                    .filter(image -> !contentImagePaths.contains(image.getImageUrl()))
                    .collect(Collectors.toList());

            try {
                fileStorageUtil.deleteFiles(imagesToDelete.stream()
                        .map(FAQImage::getImageUrl)
                        .collect(Collectors.toList()));
                faqImageRepository.deleteAll(imagesToDelete);
            } catch (FAQFileException e) {
                log.warn("FAQ 수정 중 불필요한 이미지 삭제 실패 - 계속 진행합니다: {}", e.getMessage());
                // 이미지 삭제 실패는 중요한 오류가 아니므로 처리 계속 진행
            }

            // 5) 새 파일 저장
            if (fileStorageUtil.hasFiles(requestDto.getNewFiles())) {
                List<String> newPaths;
                try {
                    newPaths = fileStorageUtil.saveFiles(requestDto.getNewFiles(), id);
                } catch (FAQFileException e) {
                    log.error("FAQ 수정 중 새 파일 저장 실패: ", e);
                    throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                            "새 이미지 저장에 실패했습니다. 이미지 형식을 확인하고 다시 시도해주세요.", e);
                }

                // answer 내 경로 수정
                String updatedAnswer = fileStorageUtil.updateImagePaths(requestDto.getAnswer(), newPaths, faq.getId());
                faq.update(requestDto.getCategory(), requestDto.getQuestion(), updatedAnswer);

                saveFAQImages(newPaths, faq);
            } else {
                // 새 이미지 없으면, 기존 content만 업데이트
                faq.update(requestDto.getCategory(), requestDto.getQuestion(), requestDto.getAnswer());
            }

            return toResponseDto(faq);
        } catch (FAQNotFoundException e) {
            log.warn("FAQ 수정 중 FAQ를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (FAQFileException e) {
            log.error("FAQ 수정 중 파일 관련 오류: {}", e.getMessage());
            throw e;
        } catch (FAQException e) {
            log.error("FAQ 수정 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("FAQ 수정 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_UPDATE_ERROR,
                    "FAQ를 수정하는 중 데이터베이스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (IOException e) {
            log.error("FAQ 수정 중 파일 처리 IO 오류 발생: ", e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "파일 처리 중 오류가 발생했습니다. 파일 크기와 형식을 확인해주세요.", e);
        } catch (Exception e) {
            log.error("FAQ 수정 중 예상치 못한 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_UPDATE_ERROR,
                    "FAQ 수정 중 오류가 발생했습니다. 관리자에게 문의해주세요.", e);
        }
    }

    // FAQ 삭제
    public void deleteFAQ(Long id) throws IOException {
        if (id == null) {
            throw new FAQNotFoundException("삭제할 FAQ ID가 제공되지 않았습니다.");
        }

        try {
            FAQ faq = faqRepository.findById(id)
                    .orElseThrow(() -> new FAQNotFoundException("ID가 " + id + "인 FAQ를 찾을 수 없습니다."));

            // 기존 이미지 조회
            List<FAQImage> images = faqImageRepository.findAllByFaqId(id);

            try {
                // 이미지 파일 삭제
                fileStorageUtil.deleteFiles(images.stream()
                        .map(FAQImage::getImageUrl)
                        .collect(Collectors.toList()));
            } catch (FAQFileException e) {
                log.warn("FAQ 삭제 중 이미지 파일 삭제 실패 - DB 삭제는 계속 진행합니다: {}", e.getMessage());
                // 이미지 삭제 실패해도 DB 데이터는 삭제 진행
            }

            // DB에서 이미지 정보 삭제
            faqImageRepository.deleteAll(images);

            // FAQ 삭제
            faqRepository.delete(faq);

            log.info("FAQ 삭제 완료: ID={}", id);
        } catch (FAQNotFoundException e) {
            log.warn("FAQ 삭제 중 FAQ를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("FAQ 삭제 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_DELETE_ERROR,
                    "FAQ를 삭제하는 중 데이터베이스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        } catch (Exception e) {
            log.error("FAQ 삭제 중 예상치 못한 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_DELETE_ERROR,
                    "FAQ 삭제 중 오류가 발생했습니다. 관리자에게 문의해주세요.", e);
        }
    }

    // 입력 데이터 검증
    private void validateFAQData(FAQCategory category, String question, String answer) {
        if (category == null) {
            throw new FAQException(FAQErrorCode.FAQ_INVALID_CATEGORY, "FAQ 카테고리는 필수입니다.");
        }

        if (question == null || question.trim().isEmpty()) {
            throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "FAQ 질문은 필수입니다.");
        }

        if (answer == null || answer.trim().isEmpty()) {
            throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "FAQ 답변은 필수입니다.");
        }

        // 질문 길이 제한 (예: 100자)
        if (question.length() > 100) {
            throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA,
                    "FAQ 질문은 100자 이내로 작성해주세요. (현재: " + question.length() + "자)");
        }
    }

    private void saveFAQImages(List<String> relativePaths, FAQ faq) {
        try {
            relativePaths.forEach(path -> {
                FAQImage image = FAQImage.builder()
                        .imageUrl(path) // "faq_{id}/파일명"
                        .faq(faq)
                        .build();
                faqImageRepository.save(image);
            });
            log.debug("FAQ 이미지 {} 개 저장 완료: FAQ ID={}", relativePaths.size(), faq.getId());
        } catch (DataAccessException e) {
            log.error("FAQ 이미지 저장 중 데이터베이스 오류 발생: ", e);
            throw new FAQException(FAQErrorCode.FAQ_SAVE_ERROR,
                    "FAQ 이미지를 저장하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);
        }
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