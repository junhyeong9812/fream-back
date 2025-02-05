package com.fream.back.domain.notice.service.command;

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
public class NoticeFileStorageUtil {

    private static final String NOTICE_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String NOTICE_FILES_ENDPOINT = "/api/notices/files";
    private static final String NOTICE_BASE_DIR = "/home/ubuntu/fream/notice";

    public List<String> saveFiles(List<MultipartFile> files, Long noticeId) throws IOException {
        List<String> savedPaths = new ArrayList<>();
        String subDir = "notice_" + noticeId;

        for (MultipartFile file : files) {
            savedPaths.add(saveFileInternal(file, subDir));
        }
        return savedPaths;
    }

    private String saveFileInternal(MultipartFile file, String subDir) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID() + extension;

            Path dirPath = Paths.get(NOTICE_BASE_DIR, subDir);
            Files.createDirectories(dirPath);

            Path filePath = dirPath.resolve(uniqueFileName).normalize();
            file.transferTo(filePath.toFile());

            // DB 저장 경로: "notice_{id}/uniqueFileName"
            return subDir + "/" + uniqueFileName;

        } catch (IOException e) {
            throw new RuntimeException("파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    public void deleteFiles(List<String> relativePaths) throws IOException {
        for (String path : relativePaths) {
            Path fullPath = Paths.get(NOTICE_BASE_DIR, path).normalize();
            if (Files.exists(fullPath)) {
                Files.deleteIfExists(fullPath);
            }
        }
    }

    public boolean isVideo(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".mp4") || lower.endsWith(".avi") || lower.endsWith(".mov");
    }

    // 파일 존재 여부 확인
    public boolean fileExists(String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path);
    }

    // 파일 경로 반환
    public Path getFilePath(String fileName) {
        return Paths.get(NOTICE_BASE_DIR + fileName);
    }

    public List<String> extractImagePaths(String content) {
        List<String> paths = new ArrayList<>();
        String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            paths.add(matcher.group(1)); // src 값만 추출
        }
        return paths;
    }
    /**
     * HTML content 내 <img src> 경로를
     * "https://www.pinjun.xyz/api/notices/files/{noticeId}/{fileName}" 로 치환하는 메서드
     */
    public String updateImagePaths(String content, List<String> relativePaths, Long noticeId) {
        if (content == null || relativePaths == null || relativePaths.isEmpty()) {
            return content;
        }

        // 정규식: HTML img 태그의 src="..."
        String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        // 치환된 결과를 담을 객체
        StringBuffer updatedContent = new StringBuffer();

        while (matcher.find() && !relativePaths.isEmpty()) {
            // 기존 src="..." 값
            String originalSrc = matcher.group(1);

            // relativePath 예: "notice_{id}/abc.png"
            String relativePath = relativePaths.remove(0);

            // 폴더명 / 파일명 분리: notice_{id}, abc.png
            String[] parts = relativePath.split("/");
            // parts[0] = "notice_10"
            // parts[1] = "abc.png"
            String fileName = parts[1];  // 실제 파일명만 추출 (폴더부분 제외)

            // ★ 절대 URL 생성
            // e.g. https://www.pinjun.xyz/api/notices/files/{noticeId}/{fileName}
            String newSrc = NOTICE_DOMAIN_URL + NOTICE_FILES_ENDPOINT
                    + "/" + noticeId + "/" + fileName;

            // 정규식 치환
            matcher.appendReplacement(
                    updatedContent,
                    matcher.group(0).replace(originalSrc, newSrc)
            );
        }
        matcher.appendTail(updatedContent);

        return updatedContent.toString();
    }

}
