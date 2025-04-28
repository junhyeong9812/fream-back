package com.fream.back.domain.notice.service.command;

import com.fream.back.domain.notice.dto.NoticeCreateRequestDto;
import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.dto.NoticeUpdateRequestDto;
import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeImage;
import com.fream.back.domain.notice.exception.NoticeErrorCode;
import com.fream.back.domain.notice.exception.NoticeException;
import com.fream.back.domain.notice.exception.NoticeFileException;
import com.fream.back.domain.notice.exception.NoticeNotFoundException;
import com.fream.back.domain.notice.repository.NoticeImageRepository;
import com.fream.back.domain.notice.repository.NoticeRepository;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 공지사항 생성/수정/삭제 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NoticeCommandService {

    // 상수 정의
    private static final String NOTICE_DOMAIN_URL = "https://www.pinjun.xyz";
    private static final String NOTICE_FILES_ENDPOINT = "/api/notices/files";
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".jpg", ".jpeg", ".png", ".gif");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(".mp4", ".avi", ".mov");

    // 의존성 주입
    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final NotificationCommandService notificationCommandService;
    private final FileUtils fileUtils;

    /**
     * 공지사항 생성
     *
     * @param requestDto 생성 요청 DTO
     * @return 생성된 공지사항 정보
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    public NoticeResponseDto createNotice(NoticeCreateRequestDto requestDto) throws IOException {
        log.debug("공지사항 생성 시작: {}", requestDto);

        Notice notice = Notice.builder()
                .title(requestDto.getTitle())
                .content(requestDto.getContent())
                .category(requestDto.getCategory())
                .build();

        try {
            // 공지사항 저장
            noticeRepository.save(notice);
            log.debug("공지사항 DB 저장 완료: id={}", notice.getId());

            // 알림 발송 (실패해도 공지 등록은 진행)
            try {
                sendNotificationToAllUsers(notice);
                log.debug("공지사항 알림 발송 완료: id={}", notice.getId());
            } catch (Exception e) {
                log.error("공지사항 알림 발송 중 오류 발생: id={}, error={}", notice.getId(), e.getMessage(), e);
            }

            // 파일 처리
            if (!requestDto.getFiles().isEmpty()) {
                String directory = "notice_" + notice.getId();

                // 1. 파일 저장 및 URL 목록 생성
                List<String> savedFiles = saveNoticeFiles(directory, requestDto.getFiles());
                log.debug("공지사항 파일 저장 완료: id={}, count={}", notice.getId(), savedFiles.size());

                // 2. 이미지 URL을 HTML 내용에 반영
                String updatedContent = updateImageUrlsInContent(notice.getContent(), savedFiles, notice.getId());
                notice.updateContent(updatedContent);
                log.debug("공지사항 내용 경로 업데이트 완료: id={}", notice.getId());

                // 3. 이미지 정보 저장
                saveNoticeImageEntities(savedFiles, notice);
                log.debug("공지사항 이미지 정보 DB 저장 완료: id={}", notice.getId());
            }

            // 응답 DTO 생성
            log.info("공지사항 생성 완료: id={}, title={}", notice.getId(), notice.getTitle());
            return createResponseDto(notice);
        } catch (DataAccessException e) {
            log.error("공지사항 DB 저장 중 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR, "공지사항 저장 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (NoticeFileException e) {
            log.error("공지사항 파일 처리 중 오류 발생: id={}, error={}", notice.getId(), e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            log.error("공지사항 파일 저장 중 IO 오류 발생: id={}", notice.getId(), e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR, "공지사항 파일 저장 중 오류가 발생했습니다.", e);
        } catch (NoticeException e) {
            log.error("공지사항 생성 중 오류 발생: ", e);
            throw e;
        } catch (Exception e) {
            log.error("공지사항 생성 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR, "공지사항 저장 중 시스템 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 수정
     *
     * @param noticeId 공지사항 ID
     * @param requestDto 수정 요청 DTO
     * @return 수정된 공지사항 정보
     * @throws IOException 파일 처리 중 오류 발생 시
     */
    public NoticeResponseDto updateNotice(Long noticeId, NoticeUpdateRequestDto requestDto) throws IOException {
        log.debug("공지사항 수정 시작: id={}, {}", noticeId, requestDto);

        // 공지사항 조회
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException("ID가 " + noticeId + "인 공지사항을 찾을 수 없습니다."));
        log.debug("수정할 공지사항 조회 완료: id={}", noticeId);

        try {
            // 1. 현재 콘텐츠에서 사용 중인 이미지 경로 추출
            List<String> currentContentPaths = extractImagePathsFromContent(requestDto.getContent());
            log.debug("현재 사용 중인 이미지 경로 추출 완료: count={}", currentContentPaths.size());

            // 2. 사용하지 않는 이미지 삭제 처리
            List<NoticeImage> existingImages = noticeImageRepository.findAllByNoticeId(noticeId);

            List<NoticeImage> imagesToDelete = existingImages.stream()
                    .filter(img -> !currentContentPaths.contains(img.getImageUrl()))
                    .collect(Collectors.toList());

            if (!imagesToDelete.isEmpty()) {
                log.debug("삭제할 이미지 필터링 완료: count={}", imagesToDelete.size());

                // DB에서 먼저 삭제
                noticeImageRepository.deleteAll(imagesToDelete);
                log.debug("사용하지 않는 이미지 DB 정보 삭제 완료: count={}", imagesToDelete.size());

                // 파일 시스템에서 삭제
                for (NoticeImage image : imagesToDelete) {
                    String directory = image.getDirectory();
                    String fileName = image.getFileName();
                    fileUtils.deleteFile(directory, fileName);
                }
                log.debug("사용하지 않는 이미지 파일 삭제 완료: count={}", imagesToDelete.size());
            }

            // 3. 새 파일 처리
            String updatedContent = requestDto.getContent();

            if (!requestDto.getNewFiles().isEmpty()) {
                String directory = "notice_" + noticeId;

                // 파일 저장
                List<String> newPaths = saveNoticeFiles(directory, requestDto.getNewFiles());
                log.debug("새 파일 저장 완료: count={}", newPaths.size());

                // 내용 업데이트
                updatedContent = updateImageUrlsInContent(updatedContent, newPaths, noticeId);
                log.debug("이미지 경로가 업데이트된 내용으로 공지사항 수정");

                // 이미지 정보 저장
                saveNoticeImageEntities(newPaths, notice);
                log.debug("새 이미지 정보 DB 저장 완료: count={}", newPaths.size());
            }

            // 4. 공지사항 업데이트
            notice.update(requestDto.getTitle(), updatedContent, requestDto.getCategory());
            log.debug("공지사항 정보 수정 완료");

            // 5. 응답 생성
            log.info("공지사항 수정 완료: id={}, title={}", notice.getId(), notice.getTitle());
            return createResponseDto(notice);
        } catch (NoticeFileException e) {
            log.error("공지사항 파일 처리 중 오류 발생: id={}, error={}", noticeId, e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            log.error("공지사항 파일 처리 중 IO 오류 발생: id={}", noticeId, e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR, "파일 처리 중 오류가 발생했습니다.", e);
        } catch (DataAccessException e) {
            log.error("공지사항 DB 처리 중 오류 발생: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_UPDATE_ERROR, "공지사항 업데이트 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (NoticeNotFoundException e) {
            log.warn("수정할 공지사항을 찾을 수 없음: id={}", noticeId);
            throw e;
        } catch (NoticeException e) {
            log.error("공지사항 수정 중 오류 발생: id={}", noticeId, e);
            throw e;
        } catch (Exception e) {
            log.error("공지사항 수정 중 예상치 못한 오류 발생: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_UPDATE_ERROR, "공지사항 수정 중 시스템 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 삭제
     *
     * @param noticeId 공지사항 ID
     * @throws IOException 파일 삭제 중 오류 발생 시
     */
    public void deleteNotice(Long noticeId) throws IOException {
        log.debug("공지사항 삭제 시작: id={}", noticeId);

        // 공지사항 조회
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException("ID가 " + noticeId + "인 공지사항을 찾을 수 없습니다."));
        log.debug("삭제할 공지사항 조회 완료: id={}", noticeId);

        try {
            // 이미지 처리
            List<NoticeImage> images = noticeImageRepository.findAllByNoticeId(noticeId);
            log.debug("공지사항 이미지 조회 완료: count={}", images.size());

            if (!images.isEmpty()) {
                // DB에서 이미지 정보 삭제
                noticeImageRepository.deleteAll(images);
                log.debug("공지사항 이미지 DB 정보 삭제 완료: count={}", images.size());

                // 파일 시스템에서 삭제
                String directory = "notice_" + noticeId;
                fileUtils.deleteDirectory(directory);
                log.debug("공지사항 파일 디렉토리 삭제 완료: directory={}", directory);
            }

            // 공지사항 삭제
            noticeRepository.delete(notice);
            log.info("공지사항 삭제 완료: id={}", noticeId);
        } catch (DataAccessException e) {
            log.error("공지사항 DB 처리 중 오류 발생: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_DELETE_ERROR, "공지사항 삭제 중 데이터베이스 오류가 발생했습니다.", e);
        } catch (NoticeNotFoundException e) {
            log.warn("삭제할 공지사항을 찾을 수 없음: id={}", noticeId);
            throw e;
        } catch (NoticeException e) {
            log.error("공지사항 삭제 중 오류 발생: id={}", noticeId, e);
            throw e;
        } catch (Exception e) {
            log.error("공지사항 삭제 중 예상치 못한 오류 발생: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_DELETE_ERROR, "공지사항 삭제 중 시스템 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 파일 저장
     *
     * @param directory 디렉토리 경로
     * @param files 파일 목록
     * @return 저장된 파일의 상대 경로 목록
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    private List<String> saveNoticeFiles(String directory, List<MultipartFile> files) throws IOException {
        List<String> savedPaths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalFilename = file.getOriginalFilename();
            // 확장자 유효성 검사
            validateFileExtension(originalFilename);

            // 파일 저장 (prefix로 이미지/비디오 구분 없이 사용)
            String savedFileName = fileUtils.saveFile(directory, "notice_", file);
            String relativePath = directory + "/" + savedFileName;
            savedPaths.add(relativePath);

            log.debug("파일 저장 완료: {}", relativePath);
        }

        return savedPaths;
    }

    /**
     * 공지사항 이미지 엔티티 저장
     *
     * @param relativePaths 이미지 상대 경로 목록
     * @param notice 공지사항 엔티티
     */
    private void saveNoticeImageEntities(List<String> relativePaths, Notice notice) {
        for (String path : relativePaths) {
            try {
                NoticeImage image = NoticeImage.builder()
                        .imageUrl(path)
                        .isVideo(isVideoFile(path))
                        .notice(notice)
                        .build();

                noticeImageRepository.save(image);
                log.debug("이미지 정보 저장 완료: notice_id={}, path={}", notice.getId(), path);
            } catch (DataAccessException e) {
                log.error("이미지 정보 저장 중 DB 오류 발생: path={}", path, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR, "이미지 정보 저장 중 오류가 발생했습니다.", e);
            } catch (Exception e) {
                log.error("이미지 정보 저장 중 예상치 못한 오류 발생: path={}", path, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR, "이미지 정보 저장 중 예상치 못한 오류가 발생했습니다.", e);
            }
        }
    }

    /**
     * HTML 내용에서 이미지 경로 추출
     *
     * @param content HTML 콘텐츠
     * @return 추출된 이미지 경로 목록
     */
    private List<String> extractImagePathsFromContent(String content) {
        List<String> paths = new ArrayList<>();
        if (content == null) {
            return paths;
        }

        try {
            // 정규식: <img> 태그의 src 속성값 추출
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
            throw new NoticeException(NoticeErrorCode.NOTICE_HTML_PARSING_ERROR, "HTML 콘텐츠에서 이미지 경로 추출 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * HTML 콘텐츠 내 이미지 경로 업데이트
     *
     * @param content 원본 HTML 콘텐츠
     * @param imagePaths 이미지 경로 목록
     * @param noticeId 공지사항 ID
     * @return 업데이트된 HTML 콘텐츠
     */
    private String updateImageUrlsInContent(String content, List<String> imagePaths, Long noticeId) {
        if (content == null) {
            return "";
        }

        if (imagePaths == null || imagePaths.isEmpty()) {
            return content;
        }

        try {
            // 정규식: <img> 태그의 src 속성 검색
            String regex = "<img\\s+[^>]*src=\"([^\"]*)\"";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(content);

            // 이미지 경로 목록 복사본 (원본 리스트 보존)
            List<String> pathsCopy = new ArrayList<>(imagePaths);

            // 치환 결과를 담을 버퍼
            StringBuffer updatedContent = new StringBuffer();

            while (matcher.find() && !pathsCopy.isEmpty()) {
                // 원본 src 속성값
                String originalSrc = matcher.group(1);

                // 새 이미지 경로
                String relativePath = pathsCopy.remove(0);

                // 파일명 추출
                String fileName = "";
                int lastSlashIndex = relativePath.lastIndexOf('/');
                if (lastSlashIndex >= 0 && lastSlashIndex < relativePath.length() - 1) {
                    fileName = relativePath.substring(lastSlashIndex + 1);
                } else {
                    fileName = relativePath;
                }

                // API URL 생성
                String newSrc = String.format("%s%s/%d/%s",
                        NOTICE_DOMAIN_URL,
                        NOTICE_FILES_ENDPOINT,
                        noticeId,
                        fileName);

                // 치환 실행
                matcher.appendReplacement(updatedContent, matcher.group(0).replace(originalSrc, newSrc));
            }

            // 나머지 부분 추가
            matcher.appendTail(updatedContent);

            return updatedContent.toString();
        } catch (Exception e) {
            log.error("이미지 경로 업데이트 중 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_HTML_PARSING_ERROR, "HTML 콘텐츠 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일이 비디오인지 확인
     *
     * @param path 파일 경로
     * @return 비디오 여부
     */
    private boolean isVideoFile(String path) {
        if (path == null) {
            return false;
        }

        String lower = path.toLowerCase();
        return ALLOWED_VIDEO_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    /**
     * 파일 확장자 유효성 검사
     *
     * @param fileName 파일명
     */
    private void validateFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_UNSUPPORTED_FILE_TYPE, "파일 확장자를 확인할 수 없습니다.");
        }

        String extension = "";
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex != -1) {
            extension = fileName.substring(dotIndex).toLowerCase();
        } else {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_UNSUPPORTED_FILE_TYPE, "파일 확장자가 없습니다.");
        }

        // 허용된 파일 확장자 검사
        boolean isAllowed = ALLOWED_IMAGE_EXTENSIONS.contains(extension) || ALLOWED_VIDEO_EXTENSIONS.contains(extension);
        if (!isAllowed) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_UNSUPPORTED_FILE_TYPE,
                    "지원하지 않는 파일 형식입니다. 이미지(jpg, jpeg, png, gif) 또는 비디오(mp4, avi, mov)만 허용됩니다.");
        }
    }

    /**
     * 공지사항 엔티티에서 응답 DTO 생성
     *
     * @param notice 공지사항 엔티티
     * @return 응답 DTO
     */
    private NoticeResponseDto createResponseDto(Notice notice) {
        List<String> imageUrls = noticeImageRepository.findAllByNoticeId(notice.getId())
                .stream()
                .map(NoticeImage::getImageUrl)
                .collect(Collectors.toList());

        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory().name())
                .createdDate(notice.getCreatedDate())
                .modifiedDate(notice.getModifiedDate())
                .imageUrls(imageUrls)
                .build();
    }

    /**
     * 모든 사용자에게 알림 발송
     *
     * @param notice 공지사항 엔티티
     */
    private void sendNotificationToAllUsers(Notice notice) {
        log.debug("공지사항 알림 발송 시작: notice_id={}, title={}", notice.getId(), notice.getTitle());

        try {
            NotificationRequestDTO requestDTO = NotificationRequestDTO.builder()
                    .category(NotificationCategory.SHOPPING)  // 쇼핑 카테고리
                    .type(NotificationType.ANNOUNCEMENT)     // 공지사항 타입
                    .message("새로운 공지사항: " + notice.getTitle())
                    .build();

            notificationCommandService.createNotificationForAll(requestDTO);
            log.debug("공지사항 알림 발송 완료: notice_id={}", notice.getId());
        } catch (Exception e) {
            log.error("공지사항 알림 발송 중 오류 발생: notice_id={}", notice.getId(), e);
            throw new NoticeException(NoticeErrorCode.NOTICE_NOTIFICATION_ERROR, "공지사항 알림 발송 중 오류가 발생했습니다.", e);
        }
    }
}