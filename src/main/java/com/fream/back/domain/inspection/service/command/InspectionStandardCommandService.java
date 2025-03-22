package com.fream.back.domain.inspection.service.command;

import com.fream.back.domain.inspection.dto.InspectionStandardCreateRequestDto;
import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.dto.InspectionStandardUpdateRequestDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.entity.InspectionStandardImage;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionFileException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.repository.InspectionStandardImageRepository;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
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
public class InspectionStandardCommandService {
    private final InspectionStandardRepository inspectionStandardRepository;
    private final InspectionStandardImageRepository inspectionStandardImageRepository;
    private final InspectionFileStorageUtil fileStorageUtil;

    // 검수 기준 생성
    public InspectionStandardResponseDto createStandard(InspectionStandardCreateRequestDto requestDto) throws IOException {
        if (requestDto == null) {
            throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_REQUEST_DATA,
                    "검수 기준 데이터가 필요합니다.");
        }

        try {
            // 요청 데이터 검증
            validateStandardData(requestDto.getCategory(), requestDto.getContent());

            // 1) 엔티티 생성
            InspectionStandard standard = InspectionStandard.builder()
                    .category(requestDto.getCategory())
                    .content(requestDto.getContent())
                    .build();

            // 2) 저장
            try {
                inspectionStandardRepository.save(standard);
                log.debug("검수 기준 엔티티 저장 완료: ID={}", standard.getId());
            } catch (DataAccessException e) {
                log.error("검수 기준 저장 중 데이터베이스 오류 발생: ", e);
                throw new InspectionException(InspectionErrorCode.INSPECTION_SAVE_ERROR,
                        "검수 기준을 저장하는 중 데이터베이스 오류가 발생했습니다.", e);
            }

            // 3) 파일
            if (fileStorageUtil.hasFiles(requestDto.getFiles())) {
                Long inspectionId = standard.getId();
                List<String> relativePaths;

                try {
                    // "inspection_10/xxx.png"
                    relativePaths = fileStorageUtil.saveFiles(requestDto.getFiles(), inspectionId);
                    log.debug("검수 기준 이미지 파일 {} 개 저장 완료", relativePaths.size());
                } catch (InspectionFileException e) {
                    log.error("검수 기준 생성 중 파일 저장 실패: ", e);
                    throw e;
                }

                try {
                    // HTML 치환 -> 절대 URL
                    String updatedContent = fileStorageUtil.updateImagePaths(requestDto.getContent(), relativePaths, inspectionId);
                    standard.update(requestDto.getCategory(), updatedContent);
                } catch (InspectionFileException e) {
                    log.error("검수 기준 내용의 이미지 경로 수정 중 오류 발생: ", e);
                    throw e;
                }

                // 이미지 엔티티
                saveStandardImages(relativePaths, standard);
            }

            return toResponseDto(standard);
        } catch (InspectionFileException e) {
            // 파일 관련 예외는 로깅 후 그대로 전파
            log.error("검수 기준 생성 중 파일 처리 예외 발생: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 관련 예외는 로깅 후 그대로 전파
            log.error("검수 기준 생성 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("검수 기준 생성 중 IO 오류 발생: ", e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                    "파일 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("검수 기준 생성 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_SAVE_ERROR,
                    "검수 기준 생성 중 오류가 발생했습니다.", e);
        }
    }

    // 검수 기준 수정
    public InspectionStandardResponseDto updateStandard(Long id, InspectionStandardUpdateRequestDto dto) throws IOException {
        if (id == null) {
            throw new InspectionNotFoundException("검수 기준 ID가 필요합니다.");
        }

        if (dto == null) {
            throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_REQUEST_DATA,
                    "수정할 검수 기준 데이터가 필요합니다.");
        }

        try {
            // 요청 데이터 검증
            validateStandardData(dto.getCategory(), dto.getContent());

            // 검수 기준 조회
            InspectionStandard standard = inspectionStandardRepository.findById(id)
                    .orElseThrow(() -> new InspectionNotFoundException("ID가 " + id + "인 검수 기준을 찾을 수 없습니다."));

            // 기존 이미지 조회
            List<InspectionStandardImage> existingImages = inspectionStandardImageRepository.findAllByInspectionStandardId(id);

            // content 내 이미지 경로 추출
            List<String> contentImagePaths = fileStorageUtil.extractImagePaths(dto.getContent());

            try {
                // 삭제할 이미지 (content에 없는 이미지들)
                List<InspectionStandardImage> imagesToDelete = existingImages.stream()
                        .filter(img -> !contentImagePaths.contains(img.getImageUrl()))
                        .collect(Collectors.toList());

                if (!imagesToDelete.isEmpty()) {
                    fileStorageUtil.deleteFiles(imagesToDelete.stream()
                            .map(InspectionStandardImage::getImageUrl)
                            .collect(Collectors.toList()));
                    inspectionStandardImageRepository.deleteAll(imagesToDelete);
                    log.debug("검수 기준 수정 중 불필요한 이미지 {} 개 삭제 완료", imagesToDelete.size());
                }
            } catch (InspectionFileException e) {
                // 이미지 삭제 실패는 경고로 로그하고 계속 진행
                log.warn("검수 기준 수정 중 불필요한 이미지 삭제 실패 - 계속 진행합니다: {}", e.getMessage());
            }

            // 새 이미지 처리
            if (fileStorageUtil.hasFiles(dto.getNewFiles())) {
                List<String> newPaths;
                try {
                    newPaths = fileStorageUtil.saveFiles(dto.getNewFiles(), id);
                    log.debug("검수 기준 수정 중 새 이미지 {} 개 저장 완료", newPaths.size());
                } catch (InspectionFileException e) {
                    log.error("검수 기준 수정 중 새 파일 저장 실패: ", e);
                    throw e;
                }

                try {
                    // HTML content 내 이미지 경로 업데이트
                    String updatedContent = fileStorageUtil.updateImagePaths(dto.getContent(), newPaths, standard.getId());
                    standard.update(dto.getCategory(), updatedContent);
                } catch (InspectionFileException e) {
                    log.error("검수 기준 내용의 이미지 경로 수정 중 오류 발생: ", e);
                    throw e;
                }

                // 이미지 엔티티 저장
                saveStandardImages(newPaths, standard);
            } else {
                // 새 이미지 없으면, 기존 content만 업데이트
                standard.update(dto.getCategory(), dto.getContent());
            }

            log.info("검수 기준 수정 완료: ID={}", id);
            return toResponseDto(standard);
        } catch (InspectionNotFoundException e) {
            // 조회 실패 시 NOT_FOUND 예외 그대로 전파
            log.warn("검수 기준 수정 중 기준을 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (InspectionFileException e) {
            // 파일 관련 예외 그대로 전파
            log.error("검수 기준 수정 중 파일 관련 오류: {}", e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 관련 예외 그대로 전파
            log.error("검수 기준 수정 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("검수 기준 수정 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_UPDATE_ERROR,
                    "검수 기준을 수정하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (IOException e) {
            log.error("검수 기준 수정 중 파일 처리 IO 오류 발생: ", e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                    "파일 처리 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("검수 기준 수정 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_UPDATE_ERROR,
                    "검수 기준 수정 중 오류가 발생했습니다.", e);
        }
    }

    // 검수 기준 삭제
    public void deleteStandard(Long id) throws IOException {
        if (id == null) {
            throw new InspectionNotFoundException("삭제할 검수 기준 ID가 필요합니다.");
        }

        try {
            // 검수 기준 조회
            InspectionStandard standard = inspectionStandardRepository.findById(id)
                    .orElseThrow(() -> new InspectionNotFoundException("ID가 " + id + "인 검수 기준을 찾을 수 없습니다."));

            // 이미지 조회
            List<InspectionStandardImage> images = inspectionStandardImageRepository.findAllByInspectionStandardId(id);

            try {
                // 이미지 파일 삭제
                if (!images.isEmpty()) {
                    fileStorageUtil.deleteFiles(images.stream()
                            .map(InspectionStandardImage::getImageUrl)
                            .collect(Collectors.toList()));
                    log.debug("검수 기준 삭제 중 이미지 {} 개 삭제 완료", images.size());
                }
            } catch (InspectionFileException e) {
                // 이미지 삭제 실패는 경고로 로깅하고 DB 삭제는 계속 진행
                log.warn("검수 기준 삭제 중 이미지 파일 삭제 실패 - DB 삭제는 계속 진행합니다: {}", e.getMessage());
            }

            // DB에서 이미지 엔티티 및 검수 기준 삭제
            inspectionStandardImageRepository.deleteAll(images);
            inspectionStandardRepository.delete(standard);

            log.info("검수 기준 삭제 완료: ID={}", id);
        } catch (InspectionNotFoundException e) {
            // 조회 실패 시 NOT_FOUND 예외 그대로 전파
            log.warn("검수 기준 삭제 중 기준을 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (InspectionFileException e) {
            // 파일 관련 예외는 로깅 후 그대로 전파
            log.error("검수 기준 삭제 중 파일 관련 오류: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("검수 기준 삭제 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_DELETE_ERROR,
                    "검수 기준을 삭제하는 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("검수 기준 삭제 중 예상치 못한 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_DELETE_ERROR,
                    "검수 기준 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // 데이터 검증
    private void validateStandardData(InspectionCategory category, String content) {
        if (category == null) {
            throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_CATEGORY,
                    "검수 기준 카테고리는 필수입니다.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_REQUEST_DATA,
                    "검수 기준 내용은 필수입니다.");
        }
    }

    // 이미지 엔티티 저장
    private void saveStandardImages(List<String> relativePaths, InspectionStandard standard) {
        try {
            for (String path : relativePaths) {
                InspectionStandardImage image = InspectionStandardImage.builder()
                        .imageUrl(path) // "inspection_{id}/파일명"
                        .inspectionStandard(standard)
                        .build();
                inspectionStandardImageRepository.save(image);
            }
            log.debug("검수 기준 이미지 {} 개를 DB에 저장 완료: 검수 기준 ID={}",
                    relativePaths.size(), standard.getId());
        } catch (DataAccessException e) {
            log.error("검수 기준 이미지 저장 중 데이터베이스 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_SAVE_ERROR,
                    "검수 기준 이미지를 저장하는 중 오류가 발생했습니다.", e);
        }
    }

    // 응답 DTO 변환
    private InspectionStandardResponseDto toResponseDto(InspectionStandard standard) {
        try {
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
        } catch (DataAccessException e) {
            log.error("응답 DTO 변환 중 이미지 URL 조회 오류: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 이미지 정보를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}