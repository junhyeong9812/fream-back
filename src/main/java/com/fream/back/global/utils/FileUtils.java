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

    // 서버에서 파일을 저장할 루트 디렉토리 (배포환경 경로)
    private static final String BASE_DIR = "/home/ubuntu/fream";

    /**
     * 파일 존재 여부 확인
     * @param directory 예) "product/10"
     * @param fileName  예) "thumbnail_xxx.jpg"
     */
    public boolean existsFile(String directory, String fileName) {
        // 최종 경로: /home/ubuntu/fream/product/10/thumbnail_xxx.jpg
        File file = new File(BASE_DIR + File.separator + directory + File.separator + fileName);
        return file.exists();
    }

    /**
     * 파일 저장
     * @param directory 예) "product/10", "style/5", "profile_images"
     * @param prefix    예) "thumbnail_"
     * @param file      MultipartFile
     * @return 실제 저장된 파일명 (uniqueFileName), ex) "thumbnail_abcdef.jpg"
     */
    public String saveFile(String directory, String prefix, MultipartFile file) {
        try {
            // 1) 이미지/비디오 MIME 체크
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                throw new IllegalArgumentException("지원되지 않는 파일 형식입니다: " + contentType);
            }

            // 2) 확장자 추출
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown";
            }
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex != -1) {
                extension = originalFilename.substring(dotIndex);  // ".jpg", ".png" 등
            }

            // 3) 고유 파일명 생성
            String uniqueFilename = prefix + UUID.randomUUID() + extension; // "thumbnail_abc123.jpg"

            // 4) 최종 경로: /home/ubuntu/fream/{directory}
            Path dirPath = Paths.get(BASE_DIR, directory).normalize();
            Files.createDirectories(dirPath); // 디렉토리가 없으면 생성

            // 5) 파일 저장
            Path filePath = dirPath.resolve(uniqueFilename).normalize();
            file.transferTo(filePath.toFile());

            // 반환: DB 등에 저장될 "uniqueFilename" (ex: "thumbnail_abc123.jpg")
            return uniqueFilename;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 삭제
     * @param directory 예) "product/10"
     * @param fileName  예) "thumbnail_abc.jpg"
     */
    public boolean deleteFile(String directory, String fileName) {
        File file = new File(BASE_DIR + File.separator + directory + File.separator + fileName);
        return file.exists() && file.delete();
    }

    /**
     * 파일 URL 반환 (필요하면 사용)
     * @param directory 예) "product/10"
     * @param fileName  예) "thumb_abc.jpg"
     */
    public String getFileUrl(String directory, String fileName) {
        // 실제 서버 내부 경로, 혹은 CDN 경로 등등
        return "/api/files/" + directory + "/" + fileName;
        // or "https://cdn.myserver.com/" + directory + "/" + fileName
    }

    /**
     * 디렉토리 삭제 (해당 디렉토리 내의 모든 파일 포함)
     * @param directory 예) "styles/10"
     */
    public boolean deleteDirectory(String directory) {
        File dir = new File(BASE_DIR + File.separator + directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }

        // 디렉토리 내의 모든 파일 삭제
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(directory + File.separator + file.getName());
                } else {
                    file.delete();
                }
            }
        }

        // 빈 디렉토리 삭제
        return dir.delete();
    }

    /**
     * 파일 존재 여부
     */
    public boolean isFileExist(String fullPath) {
        File file = new File(fullPath);
        return file.exists() && file.isFile();
    }

    /**
     * saveMediaFile (image/video)
     */
    public String saveMediaFile(String directory, String prefix, MultipartFile file) {
        // 동일하게 saveFile 내부 호출
        return saveFile(directory, prefix, file);
    }

}
