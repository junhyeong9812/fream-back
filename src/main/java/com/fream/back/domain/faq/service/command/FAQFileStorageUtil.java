package com.fream.back.domain.faq.service.command;

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
import java.util.stream.Collectors;

@Component
public class FAQFileStorageUtil {

    // 실제 운영 경로 (예: EC2)
    private static final String FAQ_BASE_DIR = "/home/ubuntu/fream/faq";
    // 도메인 & 엔드포인트
    private static final String FAQ_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String FAQ_FILES_ENDPOINT = "/api/faq/files";

    /**
     * FAQ ID를 받아, "faq_{faqId}" 하위 폴더에 여러 파일 저장
     * 반환값: "faq_{faqId}/파일명" 형태 (상대경로)
     */
    public List<String> saveFiles(List<MultipartFile> files, Long faqId) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        String subDir = "faq_" + faqId; // 예: faq_10

        for (MultipartFile file : files) {
            savedPaths.add(saveFileInternal(file, subDir));
        }
        return savedPaths;
    }

    private String saveFileInternal(MultipartFile file, String subDir) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID() + extension;

            // 최종 디렉토리: /home/ubuntu/fream/faq/faq_{faqId}
            Path dirPath = Paths.get(FAQ_BASE_DIR, subDir);
            Files.createDirectories(dirPath);

            // 최종 파일 경로
            Path filePath = dirPath.resolve(uniqueFileName).normalize();
            file.transferTo(filePath.toFile());

            // DB에 저장할 경로: "faq_{faqId}/uniqueFileName"
            return subDir + "/" + uniqueFileName;

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * HTML 내용 내 <img src> 경로를 새 파일 경로 리스트로 치환
     */
    public String updateImagePaths(String content, List<String> relativePaths, Long faqId) {
        if (content == null || relativePaths == null || relativePaths.isEmpty()) return content;

        String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
        Matcher matcher = Pattern.compile(regex).matcher(content);
        StringBuffer updatedContent = new StringBuffer();

        while (matcher.find() && !relativePaths.isEmpty()) {
            String originalSrc = matcher.group(1);
            String relPath = relativePaths.remove(0);

            String[] parts = relPath.split("/");
            String fileName = parts[1]; // faq_{id}/xxx.png → xxx.png

            String newSrc = FAQ_DOMAIN_URL + FAQ_FILES_ENDPOINT + "/" + faqId + "/" + fileName;
            matcher.appendReplacement(
                    updatedContent,
                    matcher.group(0).replace(originalSrc, newSrc)
            );
        }
        matcher.appendTail(updatedContent);
        return updatedContent.toString();
    }

    /**
     * answer(HTML) 내 현재 <img src> 경로 추출
     */
    public List<String> extractImagePaths(String content) {
        List<String> imagePaths = new ArrayList<>();
        if (content == null) return imagePaths;

        String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
        Matcher matcher = Pattern.compile(regex).matcher(content);

        while (matcher.find()) {
            imagePaths.add(matcher.group(1));
        }
        return imagePaths;
    }

    /**
     * 파일 삭제 (상대 경로 -> 풀 경로)
     */
    public void deleteFiles(List<String> relativePaths) throws IOException {
        for (String path : relativePaths) {
            Path fullPath = Paths.get(FAQ_BASE_DIR, path).normalize();
            if (Files.exists(fullPath)) {
                Files.deleteIfExists(fullPath);
            }
        }
    }

    /**
     * 파일 유무 확인 (MultipartFile 리스트)
     */
    public boolean hasFiles(List<MultipartFile> files) {
        return files != null && !files.isEmpty();
    }
}

