package com.fream.back.domain.faq.service.command;

import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FAQFileStorageUtil {

    // 실제 운영 경로 (예: EC2)
//    private static final String FAQ_BASE_DIR = "/home/ubuntu/fream/faq";
    private static final String FAQ_BASE_DIR = "C:\\Users\\pickj\\webserver\\dockerVolums\\fream\\faq";
    // 도메인 & 엔드포인트
    private static final String FAQ_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String FAQ_FILES_ENDPOINT = "/api/faq/files";
    // 허용된 이미지 확장자
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
    // 최대 파일 크기 (5MB)
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    /**
     * FAQ ID를 받아, "faq_{faqId}" 하위 폴더에 여러 파일 저장
     * 반환값: "faq_{faqId}/파일명" 형태 (상대경로)
     */
    public List<String> saveFiles(List<MultipartFile> files, Long faqId) throws IOException {
        if (faqId == null) {
            throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "FAQ ID가 필요합니다.");
        }

        List<String> savedPaths = new ArrayList<>();
        String subDir = "faq_" + faqId; // 예: faq_10

        for (MultipartFile file : files) {
            try {
                savedPaths.add(saveFileInternal(file, subDir));
            } catch (FAQFileException e) {
                // 이미 저장된 파일 롤백 시도
                for (String path : savedPaths) {
                    try {
                        Path fullPath = Paths.get(FAQ_BASE_DIR, path).normalize();
                        Files.deleteIfExists(fullPath);
                    } catch (IOException rollbackEx) {
                        log.error("파일 저장 실패 후 롤백 중 오류: {}", rollbackEx.getMessage());
                    }
                }
                throw e; // 원래 예외 다시 던지기
            }
        }

        log.info("FAQ ID {}에 대해 총 {}개 파일 저장 완료", faqId, savedPaths.size());
        return savedPaths;
    }

    private String saveFileInternal(MultipartFile file, String subDir) {
        if (file == null) {
            throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "파일이 null입니다.");
        }

        if (file.isEmpty()) {
            throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "빈 파일입니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA,
                    String.format("파일 크기가 너무 큽니다. 최대 %dMB까지 허용됩니다.", MAX_FILE_SIZE / (1024 * 1024)));
        }

        try {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "파일 이름이 없습니다.");
            }

            String extension = "";
            if (originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            // 파일 확장자 검증
            validateFileExtension(extension);

            // 고유한 파일명 생성
            String uniqueFileName = UUID.randomUUID() + extension;

            // 폴더 생성 (존재하지 않는 경우)
            Path dirPath = Paths.get(FAQ_BASE_DIR, subDir);
            Files.createDirectories(dirPath);

            // 최종 파일 경로
            Path filePath = dirPath.resolve(uniqueFileName).normalize();

            // 파일 경로 검증 (디렉토리 탐색 방지)
            if (!filePath.getParent().equals(dirPath)) {
                throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA,
                        "잘못된 파일 경로: " + filePath);
            }

            // 파일 저장
            file.transferTo(filePath.toFile());
            log.debug("파일 저장 완료: {}", filePath);

            // DB에 저장할 경로: "faq_{faqId}/uniqueFileName"
            return subDir + "/" + uniqueFileName;

        } catch (FAQFileException e) {
            // 이미 FAQFileException인 경우 그대로 전파
            throw e;
        } catch (IOException e) {
            log.error("파일 저장 중 IO 오류 발생: ", e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "파일 저장 실패: " + file.getOriginalFilename(), e);
        } catch (Exception e) {
            log.error("파일 저장 중 예상치 못한 오류 발생: ", e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    // 파일 확장자 검증 메서드
    private void validateFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            throw new FAQFileException(FAQErrorCode.FAQ_UNSUPPORTED_FILE_TYPE,
                    "파일 확장자가 없습니다.");
        }

        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new FAQFileException(FAQErrorCode.FAQ_UNSUPPORTED_FILE_TYPE,
                    "지원하지 않는 파일 형식입니다. 지원 형식: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    /**
     * HTML 내용 내 <img src> 경로를 새 파일 경로 리스트로 치환
     */
    public String updateImagePaths(String content, List<String> relativePaths, Long faqId) {
        if (content == null) {
            return "";
        }

        if (relativePaths == null || relativePaths.isEmpty()) {
            return content;
        }

        if (faqId == null) {
            throw new FAQFileException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, "FAQ ID가 필요합니다.");
        }

        try {
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Matcher matcher = Pattern.compile(regex).matcher(content);
            StringBuffer updatedContent = new StringBuffer();

            // 이미지 경로 리스트의 복사본 생성 (원본 리스트를 변경하지 않기 위해)
            List<String> pathsCopy = new ArrayList<>(relativePaths);

            while (matcher.find() && !pathsCopy.isEmpty()) {
                String originalSrc = matcher.group(1);
                String relPath = pathsCopy.remove(0);

                String[] parts = relPath.split("/");
                if (parts.length < 2) {
                    log.warn("잘못된 상대 경로 형식: {}", relPath);
                    continue;
                }

                String fileName = parts[1]; // faq_{id}/xxx.png → xxx.png

                String newSrc = FAQ_DOMAIN_URL + FAQ_FILES_ENDPOINT + "/" + faqId + "/" + fileName;
                matcher.appendReplacement(
                        updatedContent,
                        matcher.group(0).replace(originalSrc, newSrc)
                );
            }
            matcher.appendTail(updatedContent);
            return updatedContent.toString();
        } catch (Exception e) {
            log.error("이미지 경로 업데이트 중 오류 발생: ", e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "이미지 경로 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * answer(HTML) 내 현재 <img src> 경로 추출
     */
    public List<String> extractImagePaths(String content) {
        List<String> imagePaths = new ArrayList<>();
        if (content == null) {
            return imagePaths;
        }

        try {
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Matcher matcher = Pattern.compile(regex).matcher(content);

            while (matcher.find()) {
                String src = matcher.group(1);
                if (src != null && !src.trim().isEmpty()) {
                    imagePaths.add(src);
                }
            }
            return imagePaths;
        } catch (Exception e) {
            log.error("이미지 경로 추출 중 오류 발생: ", e);
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_SAVE_ERROR,
                    "이미지 경로 추출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 삭제 (상대 경로 -> 풀 경로)
     */
    public void deleteFiles(List<String> relativePaths) throws IOException {
        if (relativePaths == null || relativePaths.isEmpty()) {
            return;
        }

        List<String> failedDeletes = new ArrayList<>();

        for (String path : relativePaths) {
            if (path == null || path.trim().isEmpty()) {
                continue;
            }

            try {
                Path fullPath = Paths.get(FAQ_BASE_DIR, path).normalize();

                // 경로 검증 (디렉토리 탐색 방지)
                if (!fullPath.startsWith(Paths.get(FAQ_BASE_DIR))) {
                    log.warn("잘못된 파일 경로 접근 시도: {}", path);
                    continue;
                }

                if (Files.exists(fullPath)) {
                    boolean deleted = Files.deleteIfExists(fullPath);
                    if (!deleted) {
                        failedDeletes.add(path);
                        log.warn("파일 삭제 실패: {}", path);
                    } else {
                        log.debug("파일 삭제 성공: {}", path);
                    }
                } else {
                    log.warn("삭제할 파일이 존재하지 않음: {}", path);
                }
            } catch (IOException e) {
                failedDeletes.add(path);
                log.error("파일 삭제 중 IO 오류 발생: {} - {}", path, e.getMessage());
            }
        }

        // 일부 파일 삭제 실패 시 예외 발생
        if (!failedDeletes.isEmpty()) {
            throw new FAQFileException(FAQErrorCode.FAQ_FILE_DELETE_ERROR,
                    "일부 파일 삭제에 실패했습니다: " + String.join(", ", failedDeletes));
        }
    }

    /**
     * 파일 유무 확인 (MultipartFile 리스트)
     */
    public boolean hasFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return false;
        }

        // 실제로 내용이 있는 파일이 있는지 확인
        return files.stream().anyMatch(file -> file != null && !file.isEmpty());
    }
}