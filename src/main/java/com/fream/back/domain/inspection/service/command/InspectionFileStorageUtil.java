package com.fream.back.domain.inspection.service.command;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class InspectionFileStorageUtil {


    // 운영 서버 디렉터리 (예시)
    private static final String INSPECTION_BASE_DIR = "/home/ubuntu/fream/inspection";

    // 배포 도메인 및 엔드포인트 (하드코딩 예시)
    private static final String INSPECTION_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String INSPECTION_FILES_ENDPOINT = "/api/inspections/files";

    /**
     * 여러 MultipartFile을 "inspection_{inspectionId}" 폴더에 저장 후,
     * DB에는 "inspection_{inspectionId}/파일명" (상대 경로)만 저장
     */
    public List<String> saveFiles(List<MultipartFile> files, Long inspectionId) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        String subDir = "inspection_" + inspectionId; // ex) inspection_10

        for (MultipartFile file : files) {
            savedPaths.add(saveFileInternal(file, subDir));
        }
        return savedPaths;
    }

    /**
     * 내부적으로 실제 파일 저장
     * 반환값은 "inspection_{id}/uniqueFileName"
     */
    private String saveFileInternal(MultipartFile file, String subDir) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID() + extension;

            // 최종 디렉토리: /home/ubuntu/fream/inspection/inspection_{id}
            Path dirPath = Paths.get(INSPECTION_BASE_DIR, subDir);
            Files.createDirectories(dirPath);

            // 파일 저장
            Path filePath = dirPath.resolve(uniqueFileName).normalize();
            file.transferTo(filePath.toFile());

            // DB 기록용: "inspection_{id}/uniqueFileName"
            return subDir + "/" + uniqueFileName;

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }


    // HTML content 내 이미지 경로 추출
    public List<String> extractImagePaths(String content) {
        String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        List<String> paths = new ArrayList<>();

        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }

    /**
     * HTML content 내 <img src>를
     * "https://www.pinjun.xyz/api/inspections/files/{inspectionId}/{fileName}" 로 치환
     */
    public String updateImagePaths(String content, List<String> relativePaths, Long inspectionId) {
        if (content == null || relativePaths == null || relativePaths.isEmpty()) {
            return content;
        }

        // 정규식
        String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
        Matcher matcher = Pattern.compile(regex).matcher(content);
        StringBuffer updatedContent = new StringBuffer();

        while (matcher.find() && !relativePaths.isEmpty()) {
            // 기존 src="..."
            String originalSrc = matcher.group(1);

            // 예: "inspection_10/abc.png"
            String relPath = relativePaths.remove(0);

            // 폴더명, 파일명 분리
            String[] parts = relPath.split("/");
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
    }

    /**
     * 파일 삭제
     * DB에는 "inspection_{id}/파일명"이 들어 있으므로,
     * 실제 경로 = baseDir + relativePath
     */
    public void deleteFiles(List<String> relativePaths) throws IOException {
        for (String path : relativePaths) {
            Path fullPath = Paths.get(INSPECTION_BASE_DIR, path).normalize();
            if (Files.exists(fullPath)) {
                Files.deleteIfExists(fullPath);
            }
        }
    }

    public boolean hasFiles(List<MultipartFile> files) {
        return files != null && !files.isEmpty();
    }
}
