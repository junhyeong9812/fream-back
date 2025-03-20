package com.fream.back.global.utils;

import com.fream.back.global.exception.file.DirectoryCreationException;
import com.fream.back.global.exception.file.FileNotFoundException;
import com.fream.back.global.exception.file.FileUploadException;
import com.fream.back.global.exception.file.UnsupportedFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 파일 처리 유틸리티 클래스
 * 파일 업로드, 다운로드, 삭제 등의 기능을 제공합니다.
 */
@Slf4j
@Component
public class FileUtils {

    // 서버에서 파일을 저장할 루트 디렉토리 (배포환경 경로)
    private static final String BASE_DIR = "/home/ubuntu/fream";

    /**
     * 파일 존재 여부 확인
     *
     * @param directory 예) "product/10"
     * @param fileName  예) "thumbnail_xxx.jpg"
     * @return 파일 존재 여부
     */
    public boolean existsFile(String directory, String fileName) {
        log.debug("파일 존재 여부 확인: 디렉토리={}, 파일명={}", directory, fileName);

        // 최종 경로: /home/ubuntu/fream/product/10/thumbnail_xxx.jpg
        File file = new File(BASE_DIR + File.separator + directory + File.separator + fileName);
        boolean exists = file.exists();

        if (!exists) {
            log.debug("파일이 존재하지 않음: {}", file.getAbsolutePath());
        } else {
            log.debug("파일 존재 확인: {}", file.getAbsolutePath());
        }

        return exists;
    }

    /**
     * 파일 저장
     *
     * @param directory 예) "product/10", "style/5", "profile_images"
     * @param prefix    예) "thumbnail_"
     * @param file      MultipartFile
     * @return 실제 저장된 파일명 (uniqueFileName), ex) "thumbnail_abcdef.jpg"
     * @throws UnsupportedFileTypeException 지원되지 않는 파일 타입인 경우
     * @throws DirectoryCreationException 디렉토리 생성 실패 시
     * @throws FileUploadException 파일 저장 중 오류 발생 시
     */
    public String saveFile(String directory, String prefix, MultipartFile file) {
        log.info("파일 저장 시작: 디렉토리={}, 접두어={}, 원본파일명={}, 크기={}bytes",
                directory, prefix, file.getOriginalFilename(), file.getSize());

        try {
            // 1) 이미지/비디오 MIME 체크
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
                log.warn("지원되지 않는 파일 형식: {}", contentType);
                throw new UnsupportedFileTypeException("지원되지 않는 파일 형식입니다: " + contentType);
            }

            // 2) 확장자 추출
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                originalFilename = "unknown";
                log.warn("원본 파일명이 null입니다. 'unknown'으로 대체합니다.");
            }
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex != -1) {
                extension = originalFilename.substring(dotIndex);  // ".jpg", ".png" 등
            }

            // 3) 고유 파일명 생성
            String uniqueFilename = prefix + UUID.randomUUID() + extension; // "thumbnail_abc123.jpg"
            log.debug("고유 파일명 생성: {}", uniqueFilename);

            // 4) 최종 경로: /home/ubuntu/fream/{directory}
            Path dirPath = Paths.get(BASE_DIR, directory).normalize();
            try {
                Files.createDirectories(dirPath); // 디렉토리가 없으면 생성
                log.debug("디렉토리 생성 완료: {}", dirPath);
            } catch (IOException e) {
                log.error("디렉토리 생성 실패: {}", dirPath, e);
                throw new DirectoryCreationException(e);
            }

            // 5) 파일 저장
            Path filePath = dirPath.resolve(uniqueFilename).normalize();
            file.transferTo(filePath.toFile());
            log.info("파일 저장 완료: 경로={}, 크기={}bytes", filePath, file.getSize());

            // 반환: DB 등에 저장될 "uniqueFilename" (ex: "thumbnail_abc123.jpg")
            return uniqueFilename;
        } catch (UnsupportedFileTypeException | DirectoryCreationException e) {
            // 이미 로깅 처리됨
            throw e;
        } catch (IOException e) {
            log.error("파일 저장 중 IO 오류 발생: {}", e.getMessage(), e);
            throw new FileUploadException(e);
        } catch (Exception e) {
            log.error("파일 저장 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new FileUploadException(e);
        }
    }

    /**
     * 파일 삭제
     *
     * @param directory 예) "product/10"
     * @param fileName  예) "thumbnail_abc.jpg"
     * @return 삭제 성공 여부
     */
    public boolean deleteFile(String directory, String fileName) {
        log.info("파일 삭제 시작: 디렉토리={}, 파일명={}", directory, fileName);

        File file = new File(BASE_DIR + File.separator + directory + File.separator + fileName);
        if (!file.exists()) {
            log.warn("삭제할 파일이 존재하지 않음: {}", file.getAbsolutePath());
            return false;
        }

        boolean deleted = file.delete();
        if (deleted) {
            log.info("파일 삭제 성공: {}", file.getAbsolutePath());
        } else {
            log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
        }

        return deleted;
    }

    /**
     * 파일 URL 반환 (필요하면 사용)
     *
     * @param directory 예) "product/10"
     * @param fileName  예) "thumb_abc.jpg"
     * @return 파일에 접근할 수 있는 URL
     */
    public String getFileUrl(String directory, String fileName) {
        String url = "/api/files/" + directory + "/" + fileName;
        log.debug("파일 URL 생성: {}", url);
        return url;
    }

    /**
     * 디렉토리 삭제 (해당 디렉토리 내의 모든 파일 포함)
     *
     * @param directory 예) "styles/10"
     * @return 삭제 성공 여부
     */
    public boolean deleteDirectory(String directory) {
        log.info("디렉토리 삭제 시작: {}", directory);

        File dir = new File(BASE_DIR + File.separator + directory);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("삭제할 디렉토리가 존재하지 않거나 디렉토리가 아님: {}", dir.getAbsolutePath());
            return false;
        }

        // 디렉토리 내의 모든 파일 삭제
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(directory + File.separator + file.getName());
                    log.debug("하위 디렉토리 삭제: {}", file.getAbsolutePath());
                } else {
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.debug("파일 삭제 성공: {}", file.getAbsolutePath());
                    } else {
                        log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
                    }
                }
            }
        }

        // 빈 디렉토리 삭제
        boolean deleted = dir.delete();
        if (deleted) {
            log.info("디렉토리 삭제 성공: {}", dir.getAbsolutePath());
        } else {
            log.warn("디렉토리 삭제 실패: {}", dir.getAbsolutePath());
        }

        return deleted;
    }

    /**
     * 파일 존재 여부
     *
     * @param fullPath 파일의 전체 경로
     * @return 파일 존재 여부
     */
    public boolean isFileExist(String fullPath) {
        log.debug("파일 존재 여부 확인 (전체 경로): {}", fullPath);

        File file = new File(fullPath);
        boolean exists = file.exists() && file.isFile();

        if (!exists) {
            log.debug("파일이 존재하지 않음: {}", fullPath);
        } else {
            log.debug("파일 존재 확인: {}", fullPath);
        }

        return exists;
    }

    /**
     * 미디어 파일(이미지/비디오) 저장
     * saveFile 메소드와 동일한 기능
     *
     * @param directory 예) "product/10"
     * @param prefix    예) "thumbnail_"
     * @param file      MultipartFile
     * @return 실제 저장된 파일명
     */
    public String saveMediaFile(String directory, String prefix, MultipartFile file) {
        log.debug("미디어 파일 저장 호출: 디렉토리={}, 접두어={}", directory, prefix);
        // 동일하게 saveFile 내부 호출
        return saveFile(directory, prefix, file);
    }
}