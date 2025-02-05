package com.fream.back.global.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileUtils {

    public boolean existsFile(String directory, String filePath) {
        File file = new File(directory + filePath);
        return file.exists();
    }

    // 파일 저장
    public String saveFile(String directory, String prefix, MultipartFile file) {
        try {

            // 파일 MIME 타입 확인
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("지원되지 않는 파일 형식입니다: " + contentType);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".")); // 확장자 추출
            String uniqueFilename = prefix + UUID.randomUUID() + extension; // 고유 파일명 생성
            Path filePath = Paths.get(directory, uniqueFilename);

            // 디렉토리가 없으면 생성
            Files.createDirectories(filePath.getParent());

            // 파일 저장
            file.transferTo(filePath.toFile());

            return uniqueFilename; // 저장된 파일 이름 반환
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    // 파일 삭제
    public boolean deleteFile(String directory, String fileName) {
        File file = new File(directory + File.separator + fileName);
        return file.exists() && file.delete();
    }

    // 파일 URL 반환
    public String getFileUrl(String directory, String fileName) {
        return directory + "/" + fileName; // 실제 URL 경로는 프론트와 협의 필요
    }

    // 고유 파일명 생성
    private String generateUniqueFileName(String baseName) {
        String uuid = UUID.randomUUID().toString();
        String extension = baseName.substring(baseName.lastIndexOf(".")); // 확장자 추출
        return uuid + "_" + baseName.replaceAll("\\s+", "_") + extension;
    }

    // 파일 존재 여부 확인
    public boolean isFileExist(String fullPath) {
        File file = new File(fullPath);
        return file.exists() && file.isFile();
    }

    public String saveMediaFile(String directory, String prefix, MultipartFile file) {
        try {
            // 파일 유형 확인
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("지원되지 않는 파일 형식입니다: " + contentType);
            }

            return saveFile(directory, prefix, file);
        }  catch (RuntimeException e) {
            // 기존의 RuntimeException 사용
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

}
