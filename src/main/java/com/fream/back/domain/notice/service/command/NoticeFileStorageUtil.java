package com.fream.back.domain.notice.service.command;

import com.fream.back.domain.notice.exception.NoticeErrorCode;
import com.fream.back.domain.notice.exception.NoticeFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

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
public class NoticeFileStorageUtil {

    private static final String NOTICE_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String NOTICE_FILES_ENDPOINT = "/api/notices/files";
    private static final String NOTICE_BASE_DIR = "/home/ubuntu/fream/notice";

    // 허용된 이미지 확장자
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
    // 허용된 비디오 확장자
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(".mp4", ".avi", ".mov");
    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 여러 파일을 "notice_{noticeId}" 폴더에 저장
     */
    public List<String> saveFiles(List<MultipartFile> files, Long noticeId) throws IOException {
        if (noticeId == null) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                    "공지사항 ID가 필요합니다.");
        }

        List<String> savedPaths = new ArrayList<>();
        String subDir = "notice_" + noticeId;

        for (MultipartFile file : files) {
            try {
                savedPaths.add(saveFileInternal(file, subDir));
            } catch (NoticeFileException e) {
                // 이미 저장된 파일 롤백 시도
                for (String path : savedPaths) {
                    try {
                        Path fullPath = Paths.get(NOTICE_BASE_DIR, path).normalize();
                        Files.deleteIfExists(fullPath);
                    } catch (IOException rollbackEx) {
                        log.error("파일 저장 실패 후 롤백 중 오류: {}", rollbackEx.getMessage());
                    }
                }
                throw e; // 원래 예외 다시 던지기
            }
        }

        log.info("공지사항 ID {}에 대해 총 {}개 파일 저장 완료", noticeId, savedPaths.size());
        return savedPaths;
    }

    /**
     * 단일 파일 저장 내부 메서드
     */
    private String saveFileInternal(MultipartFile file, String subDir) {
        if (file == null) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                    "파일이 null입니다.");
        }

        if (file.isEmpty()) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                    "빈 파일입니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                    String.format("파일 크기가 너무 큽니다. 최대 %dMB까지 허용됩니다.", MAX_FILE_SIZE / (1024 * 1024)));
        }

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                        "파일 이름이 없습니다.");
            }

            String extension = "";
            if (originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 파일 확장자 검증
            validateFileExtension(extension);

            String uniqueFileName = UUID.randomUUID() + extension;

            Path dirPath = Paths.get(NOTICE_BASE_DIR, subDir);
            Files.createDirectories(dirPath);

            // 파일 경로 검증 (디렉토리 탐색 방지)
            Path filePath = dirPath.resolve(uniqueFileName).normalize();
            if (!filePath.getParent().equals(dirPath)) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                        "잘못된 파일 경로: " + filePath);
            }

            file.transferTo(filePath.toFile());
            log.debug("파일 저장 완료: {}", filePath);

            // DB 저장 경로: "notice_{id}/uniqueFileName"
            return subDir + "/" + uniqueFileName;

        } catch (NoticeFileException e) {
            // 이미 NoticeFileException인 경우 그대로 전파
            throw e;
        } catch (IOException e) {
            log.error("파일 저장 중 IO 오류 발생: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                    "파일 저장 실패: " + file.getOriginalFilename(), e);
        } catch (Exception e) {
            log.error("파일 저장 중 예상치 못한 오류 발생: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                    "파일 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * 파일 확장자 검증
     */
    private void validateFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_UNSUPPORTED_FILE_TYPE,
                    "파일 확장자가 없습니다.");
        }

        String lowerExt = extension.toLowerCase();
        boolean isAllowed = ALLOWED_IMAGE_EXTENSIONS.contains(lowerExt) || ALLOWED_VIDEO_EXTENSIONS.contains(lowerExt);

        if (!isAllowed) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_UNSUPPORTED_FILE_TYPE,
                    "지원하지 않는 파일 형식입니다. 이미지(jpg, jpeg, png, gif) 또는 비디오(mp4, avi, mov)만 허용됩니다.");
        }
    }

    /**
     * 파일 삭제
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
                Path fullPath = Paths.get(NOTICE_BASE_DIR, path).normalize();

                // 경로 검증 (디렉토리 탐색 방지)
                if (!fullPath.startsWith(Paths.get(NOTICE_BASE_DIR))) {
                    log.warn("잘못된 파일 경로 접근: {}", path);
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
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_DELETE_ERROR,
                    "일부 파일 삭제에 실패했습니다: " + String.join(", ", failedDeletes));
        }
    }

    /**
     * 파일이 비디오인지 확인
     */
    public boolean isVideo(String path) {
        if (path == null) {
            return false;
        }

        String lower = path.toLowerCase();
        return ALLOWED_VIDEO_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /**
     * 파일 존재 여부 확인
     */
    public boolean fileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        try {
            Path path = Paths.get(NOTICE_BASE_DIR, filePath).normalize();
            // 경로 검증 (디렉토리 탐색 방지)
            if (!path.startsWith(Paths.get(NOTICE_BASE_DIR))) {
                log.warn("잘못된 파일 경로 접근: {}", filePath);
                return false;
            }

            return Files.exists(path);
        } catch (Exception e) {
            log.error("파일 존재 여부 확인 중 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 파일 경로 반환
     */
    public Path getFilePath(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 이름이 비어있습니다.");
        }

        try {
            Path path = Paths.get(NOTICE_BASE_DIR, fileName).normalize();

            // 경로 검증 (디렉토리 탐색 방지)
            if (!path.startsWith(Paths.get(NOTICE_BASE_DIR))) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                        "잘못된 파일 경로: " + fileName);
            }

            return path;
        } catch (NoticeFileException e) {
            throw e;
        } catch (Exception e) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 경로 처리 중 오류 발생: " + fileName, e);
        }
    }

    /**
     * HTML 내용에서 이미지 경로 추출
     */
    public List<String> extractImagePaths(String content) {
        List<String> paths = new ArrayList<>();
        if (content == null) {
            return paths;
        }

        try {
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                String src = matcher.group(1);
                if (src != null && !src.trim().isEmpty()) {
                    paths.add(src); // src 값만 추출
                }
            }
            return paths;
        } catch (Exception e) {
            log.error("이미지 경로 추출 중 오류 발생: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                    "이미지 경로 추출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * HTML content 내 <img src> 경로를
     * "https://www.pinjun.xyz/api/notices/files/{noticeId}/{fileName}" 로 치환하는 메서드
     */
    public String updateImagePaths(String content, List<String> relativePaths, Long noticeId) {
        if (content == null) {
            return "";
        }

        if (relativePaths == null || relativePaths.isEmpty()) {
            return content;
        }

        if (noticeId == null) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA,
                    "공지사항 ID가 필요합니다.");
        }

        try {
            // 정규식: HTML img 태그의 src="..."
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);

            // 이미지 경로 리스트의 복사본 생성 (원본 리스트를 변경하지 않기 위해)
            List<String> pathsCopy = new ArrayList<>(relativePaths);

            // 치환된 결과를 담을 객체
            StringBuffer updatedContent = new StringBuffer();

            while (matcher.find() && !pathsCopy.isEmpty()) {
                // 기존 src="..." 값
                String originalSrc = matcher.group(1);

                // relativePath 예: "notice_{id}/abc.png"
                String relativePath = pathsCopy.remove(0);

                // 폴더명 / 파일명 분리
                String[] parts = relativePath.split("/");
                if (parts.length < 2) {
                    log.warn("잘못된 상대 경로 형식: {}", relativePath);
                    continue;
                }

                // parts[0] = "notice_10", parts[1] = "abc.png"
                String fileName = parts[1];  // 실제 파일명만 추출 (폴더부분 제외)

                // 절대 URL 생성
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
        } catch (Exception e) {
            log.error("이미지 경로 업데이트 중 오류 발생: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                    "이미지 경로 업데이트 중 오류가 발생했습니다.", e);
        }
    }
}