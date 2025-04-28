package com.fream.back.domain.inspection.service.command;

import com.fream.back.domain.inspection.dto.InspectionStandardCreateRequestDto;
import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.dto.InspectionStandardUpdateRequestDto;
import com.fream.back.domain.inspection.entity.InspectionCategory;
import com.fream.back.domain.inspection.entity.InspectionStandard;
import com.fream.back.domain.inspection.entity.InspectionStandardImage;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.repository.InspectionStandardImageRepository;
import com.fream.back.domain.inspection.repository.InspectionStandardRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 검수 기준 관리 서비스
 * - FileUtils 활용
 * - BaseTimeEntity 필드명 일치 (createdDate, modifiedDate)
 * - 예외 처리 일관성 강화
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InspectionStandardCommandService {
    private final InspectionStandardRepository inspectionStandardRepository;
    private final InspectionStandardImageRepository inspectionStandardImageRepository;
    private final FileUtils fileUtils;

    // 검수 기준 도메인 URL 상수 정의
    private static final String INSPECTION_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String INSPECTION_FILES_ENDPOINT = "/api/inspections/files";

    /**
     * 검수 기준 생성
     * - 트랜잭션 처리 강화
     * - FileUtils 활용
     * - 예외 처리 일관성 유지
     */
    public InspectionStandardResponseDto createStandard(InspectionStandardCreateRequestDto requestDto) {
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

            // 3) 파일 처리
            if (hasFiles(requestDto.getFiles())) {
                Long inspectionId = standard.getId();
                String directory = "inspection_" + inspectionId;
                List<String> relativePaths = new ArrayList<>();

                // 파일 저장
                for (MultipartFile file : requestDto.getFiles()) {
                    if (file != null && !file.isEmpty()) {
                        try {
                            String fileName = fileUtils.saveFile(directory, "img_", file);
                            relativePaths.add(directory + "/" + fileName);
                            log.debug("검수 기준 이미지 파일 저장 완료: {}", fileName);
                        } catch (Exception e) {
                            log.error("검수 기준 이미지 파일 저장 실패: ", e);
                            throw new InspectionException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                                    "파일 저장 중 오류가 발생했습니다.", e);
                        }
                    }
                }

                // HTML 내용 업데이트
                if (!relativePaths.isEmpty()) {
                    try {
                        String updatedContent = updateImagePaths(requestDto.getContent(), relativePaths, inspectionId);
                        standard.update(requestDto.getCategory(), updatedContent);
                    } catch (Exception e) {
                        log.error("검수 기준 내용의 이미지 경로 수정 중 오류 발생: ", e);
                        throw new InspectionException(InspectionErrorCode.INSPECTION_SAVE_ERROR,
                                "이미지 경로 처리 중 오류가 발생했습니다.", e);
                    }
                }

                // 이미지 엔티티 저장
                saveStandardImages(relativePaths, standard);
            }

            return toResponseDto(standard);
        } catch (Exception e) {
            // 예외 발생 시 전체 롤백 (트랜잭션)
            log.error("검수 기준 생성 중 오류 발생: ", e);
            if (e instanceof InspectionException) {
                throw e;
            }
            throw new InspectionException(InspectionErrorCode.INSPECTION_SAVE_ERROR,
                    "검수 기준 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 수정
     * - 트랜잭션 처리 강화
     * - FileUtils 활용
     * - 예외 처리 일관성 유지
     */
    public InspectionStandardResponseDto updateStandard(Long id, InspectionStandardUpdateRequestDto dto) {
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

            // 기존 이미지 처리
            String directory = "inspection_" + id;
            List<InspectionStandardImage> existingImages = inspectionStandardImageRepository.findAllByInspectionStandardId(id);

            // 수정 후 삭제할 이미지 파일들 찾기
            List<InspectionStandardImage> imagesToDelete = existingImages.stream()
                    .filter(img -> {
                        String imagePath = img.getImageUrl();
                        return !dto.getExistingImageUrls().contains(imagePath) &&
                                !dto.getContent().contains(imagePath);
                    })
                    .collect(Collectors.toList());

            // 삭제 대상 파일 처리
            for (InspectionStandardImage image : imagesToDelete) {
                String imageUrl = image.getImageUrl();
                if (imageUrl != null && imageUrl.contains("/")) {
                    String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                    try {
                        fileUtils.deleteFile(directory, fileName);
                        inspectionStandardImageRepository.delete(image);
                        log.debug("불필요한 이미지 삭제 완료: {}", imageUrl);
                    } catch (Exception e) {
                        log.warn("이미지 파일 삭제 실패 (계속 진행): {}", imageUrl, e);
                        // 파일 삭제 실패해도 데이터베이스 작업은 계속 진행
                        inspectionStandardImageRepository.delete(image);
                    }
                }
            }

            // 새 이미지 처리
            if (hasFiles(dto.getNewFiles())) {
                List<String> newPaths = new ArrayList<>();

                // 파일 저장
                for (MultipartFile file : dto.getNewFiles()) {
                    if (file != null && !file.isEmpty()) {
                        try {
                            String fileName = fileUtils.saveFile(directory, "img_", file);
                            String relativePath = directory + "/" + fileName;
                            newPaths.add(relativePath);
                            log.debug("새 이미지 파일 저장 완료: {}", fileName);
                        } catch (Exception e) {
                            log.error("새 이미지 파일 저장 실패: ", e);
                            throw new InspectionException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                                    "파일 저장 중 오류가 발생했습니다.", e);
                        }
                    }
                }

                // HTML 내용 업데이트
                if (!newPaths.isEmpty()) {
                    try {
                        String updatedContent = updateImagePaths(dto.getContent(), newPaths, id);
                        standard.update(dto.getCategory(), updatedContent);
                    } catch (Exception e) {
                        log.error("검수 기준 내용의 이미지 경로 수정 중 오류 발생: ", e);
                        throw new InspectionException(InspectionErrorCode.INSPECTION_UPDATE_ERROR,
                                "이미지 경로 처리 중 오류가 발생했습니다.", e);
                    }

                    // 새 이미지 엔티티 저장
                    saveStandardImages(newPaths, standard);
                }
            } else {
                // 이미지 업데이트 없을 경우, 내용만 업데이트
                standard.update(dto.getCategory(), dto.getContent());
            }

            log.info("검수 기준 수정 완료: ID={}", id);
            return toResponseDto(standard);
        } catch (Exception e) {
            // 예외 발생 시 전체 롤백 (트랜잭션)
            log.error("검수 기준 수정 중 오류 발생: ", e);
            if (e instanceof InspectionException) {
                throw e;
            }
            throw new InspectionException(InspectionErrorCode.INSPECTION_UPDATE_ERROR,
                    "검수 기준 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 검수 기준 삭제
     * - 트랜잭션 처리 강화
     * - FileUtils 활용
     * - 예외 처리 일관성 유지
     */
    public void deleteStandard(Long id) {
        if (id == null) {
            throw new InspectionNotFoundException("삭제할 검수 기준 ID가 필요합니다.");
        }

        try {
            // 검수 기준 조회
            InspectionStandard standard = inspectionStandardRepository.findById(id)
                    .orElseThrow(() -> new InspectionNotFoundException("ID가 " + id + "인 검수 기준을 찾을 수 없습니다."));

            // 이미지 조회
            List<InspectionStandardImage> images = inspectionStandardImageRepository.findAllByInspectionStandardId(id);

            // 디렉토리 삭제 (이미지 포함)
            try {
                String directory = "inspection_" + id;
                fileUtils.deleteDirectory(directory);
                log.debug("검수 기준 디렉토리 삭제 완료: {}", directory);
            } catch (Exception e) {
                // 파일 시스템 오류가 있어도 DB 작업은 계속 진행
                log.warn("검수 기준 디렉토리 삭제 실패 (계속 진행): {}", e.getMessage());
            }

            // DB에서 이미지 엔티티 및 검수 기준 삭제
            inspectionStandardImageRepository.deleteAll(images);
            inspectionStandardRepository.delete(standard);

            log.info("검수 기준 삭제 완료: ID={}", id);
        } catch (Exception e) {
            // 예외 발생 시 전체 롤백 (트랜잭션)
            log.error("검수 기준 삭제 중 오류 발생: ", e);
            if (e instanceof InspectionNotFoundException) {
                throw e;
            }
            throw new InspectionException(InspectionErrorCode.INSPECTION_DELETE_ERROR,
                    "검수 기준 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 데이터 검증
     */
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

    /**
     * 이미지 엔티티 저장
     */
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

    /**
     * HTML content 내 <img src>를
     * "https://www.pinjun.xyz/api/inspections/files/{inspectionId}/{fileName}" 로 치환
     */
    private String updateImagePaths(String content, List<String> relativePaths, Long inspectionId) {
        if (content == null) {
            return "";
        }

        if (relativePaths == null || relativePaths.isEmpty()) {
            return content;
        }

        if (inspectionId == null) {
            throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_REQUEST_DATA,
                    "검수 기준 ID가 필요합니다.");
        }

        try {
            // 정규식
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Matcher matcher = Pattern.compile(regex).matcher(content);
            StringBuffer updatedContent = new StringBuffer();

            // 이미지 경로 리스트의 복사본 생성 (원본 리스트를 변경하지 않기 위해)
            List<String> pathsCopy = new ArrayList<>(relativePaths);

            while (matcher.find() && !pathsCopy.isEmpty()) {
                // 기존 src="..."
                String originalSrc = matcher.group(1);
                // 예: "inspection_10/abc.png"
                String relPath = pathsCopy.remove(0);

                // 폴더명, 파일명 분리
                String[] parts = relPath.split("/");
                if (parts.length < 2) {
                    log.warn("잘못된 상대 경로 형식: {}", relPath);
                    continue;
                }

                // parts[0] = "inspection_10", parts[1] = "abc.png"
                String fileName = parts[1];

                // 절대 URL
                String newSrc = INSPECTION_DOMAIN_URL + INSPECTION_FILES_ENDPOINT
                        + "/" + inspectionId + "/" + fileName;
                // 예: "https://www.pinjun.xyz/api/inspections/files/10/abc.png"

                matcher.appendReplacement(
                        updatedContent,
                        matcher.group(0).replace(originalSrc, newSrc)
                );
            }
            matcher.appendTail(updatedContent);
            return updatedContent.toString();
        } catch (Exception e) {
            log.error("이미지 경로 업데이트 중 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                    "이미지 경로 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * HTML content 내 이미지 경로 추출
     */
    private List<String> extractImagePaths(String content) {
        if (content == null) {
            return new ArrayList<>();
        }

        try {
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);
            List<String> paths = new ArrayList<>();

            while (matcher.find()) {
                String src = matcher.group(1);
                if (src != null && !src.trim().isEmpty()) {
                    paths.add(src);
                }
            }
            return paths;
        } catch (Exception e) {
            log.error("이미지 경로 추출 중 오류 발생: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                    "이미지 경로 추출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * MultipartFile 리스트에 실제 파일이 있는지 확인
     */
    private boolean hasFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }

        // 실제로 내용이 있는 파일이 있는지 확인
        return files.stream().anyMatch(file -> file != null && !file.isEmpty());
    }

    /**
     * 응답 DTO 변환
     * - BaseTimeEntity 필드명에 맞게 수정 (createdDate, modifiedDate)
     */
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
                    .createdDate(standard.getCreatedDate())
                    .modifiedDate(standard.getModifiedDate())
                    .build();
        } catch (DataAccessException e) {
            log.error("응답 DTO 변환 중 이미지 URL 조회 오류: ", e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_QUERY_ERROR,
                    "검수 기준 이미지 정보를 조회하는 중 오류가 발생했습니다.", e);
        }
    }
}