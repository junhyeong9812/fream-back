package com.fream.back.domain.notice.service.command;

import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NoticeCommandService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final NoticeFileStorageUtil fileStorageUtil;
    private final NotificationCommandService notificationCommandService;

    /**
     * 공지사항 생성
     */
    public NoticeResponseDto createNotice(String title, String content, NoticeCategory category, List<MultipartFile> files)
            throws IOException {
        // 입력값 검증
        if (title == null || title.trim().isEmpty()) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "제목은 필수입니다.");
        }

        if (content == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "내용은 필수입니다.");
        }

        if (category == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_CATEGORY, "유효한 카테고리를 선택해주세요.");
        }

        try {
            log.debug("공지사항 생성 시작: title={}, category={}, files={}",
                    title, category, files != null ? files.size() : 0);

            Notice notice = Notice.builder()
                    .title(title)
                    .content(content)
                    .category(category)
                    .build();

            try {
                noticeRepository.save(notice);
                log.debug("공지사항 DB 저장 완료: id={}", notice.getId());
            } catch (DataAccessException e) {
                log.error("공지사항 DB 저장 중 오류 발생: ", e);
                throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR,
                        "공지사항 저장 중 데이터베이스 오류가 발생했습니다.", e);
            }

            // 알림 발송 시도
            try {
                sendNotificationToAllUsers(notice);
                log.debug("공지사항 알림 발송 완료: id={}", notice.getId());
            } catch (Exception e) {
                // 알림 실패는 공지 등록 자체를 실패시키지 않음 - 에러만 로깅
                log.error("공지사항 알림 발송 중 오류 발생: id={}, error={}",
                        notice.getId(), e.getMessage(), e);
            }

            // 파일 처리
            if (files != null && !files.isEmpty()) {
                try {
                    Long noticeId = notice.getId();
                    // 1) 상대경로 리스트 (예: [ "notice_10/abc.png", "notice_10/def.png", ... ])
                    List<String> relativePaths = fileStorageUtil.saveFiles(files, noticeId);
                    log.debug("공지사항 파일 저장 완료: id={}, count={}", noticeId, relativePaths.size());

                    // 2) HTML 치환 → 절대 URL
                    String updatedContent = fileStorageUtil.updateImagePaths(content, relativePaths, noticeId);
                    notice.updateContent(updatedContent);
                    log.debug("공지사항 내용 경로 업데이트 완료: id={}", noticeId);

                    // 3) DB에 이미지(NoticeImage) 엔티티 저장
                    saveNoticeImages(relativePaths, notice);
                    log.debug("공지사항 이미지 정보 DB 저장 완료: id={}", noticeId);
                } catch (NoticeFileException e) {
                    log.error("공지사항 파일 처리 중 오류 발생: id={}, error={}",
                            notice.getId(), e.getMessage(), e);
                    throw e;
                } catch (IOException e) {
                    log.error("공지사항 파일 저장 중 IO 오류 발생: id={}", notice.getId(), e);
                    throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                            "공지사항 파일 저장 중 오류가 발생했습니다.", e);
                } catch (Exception e) {
                    log.error("공지사항 파일 처리 중 예상치 못한 오류 발생: id={}", notice.getId(), e);
                    throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                            "공지사항 파일 처리 중 예상치 못한 오류가 발생했습니다.", e);
                }
            }

            log.info("공지사항 생성 완료: id={}, title={}", notice.getId(), notice.getTitle());
            return toResponseDto(notice);
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            throw e;
        } catch (NoticeException e) {
            // 공지사항 관련 예외
            throw e;
        } catch (Exception e) {
            log.error("공지사항 생성 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR,
                    "공지사항 저장 중 시스템 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 수정
     */
    public NoticeResponseDto updateNotice(Long noticeId, String title, String content, NoticeCategory category,
                                          List<String> existingImageUrls, List<MultipartFile> newFiles) throws IOException {
        // 입력값 검증
        if (noticeId == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "공지사항 ID는 필수입니다.");
        }

        if (title == null || title.trim().isEmpty()) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "제목은 필수입니다.");
        }

        if (content == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "내용은 필수입니다.");
        }

        if (category == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_CATEGORY, "유효한 카테고리를 선택해주세요.");
        }

        try {
            log.debug("공지사항 수정 시작: id={}, title={}, category={}", noticeId, title, category);

            // 기존 공지사항 조회
            Notice notice;
            try {
                notice = noticeRepository.findById(noticeId)
                        .orElseThrow(() -> new NoticeNotFoundException("ID가 " + noticeId + "인 공지사항을 찾을 수 없습니다."));
                log.debug("수정할 공지사항 조회 완료: id={}", noticeId);
            } catch (NoticeNotFoundException e) {
                log.warn("수정할 공지사항을 찾을 수 없음: id={}", noticeId);
                throw e;
            } catch (DataAccessException e) {
                log.error("공지사항 조회 중 데이터베이스 오류 발생: id={}", noticeId, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                        "공지사항 조회 중 오류가 발생했습니다.", e);
            }

            // 기존 이미지 처리
            List<NoticeImage> existingImages;
            try {
                existingImages = noticeImageRepository.findAllByNoticeId(noticeId);
                log.debug("기존 이미지 조회 완료: count={}", existingImages.size());
            } catch (DataAccessException e) {
                log.error("기존 이미지 목록 조회 중 오류 발생: id={}", noticeId, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                        "공지사항 이미지 정보 조회 중 오류가 발생했습니다.", e);
            }

            // 사용 중인 이미지 경로 추출
            List<String> currentContentPaths;
            try {
                currentContentPaths = fileStorageUtil.extractImagePaths(content);
                log.debug("현재 사용 중인 이미지 경로 추출 완료: count={}", currentContentPaths.size());
            } catch (Exception e) {
                log.error("이미지 경로 추출 중 오류 발생: id={}", noticeId, e);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                        "이미지 경로 추출 중 오류가 발생했습니다.", e);
            }

            // 삭제할 이미지 필터링 및 삭제
            try {
                List<NoticeImage> imagesToDelete = existingImages.stream()
                        .filter(img -> !currentContentPaths.contains(img.getImageUrl()))
                        .collect(Collectors.toList());

                if (!imagesToDelete.isEmpty()) {
                    log.debug("삭제할 이미지 필터링 완료: count={}", imagesToDelete.size());

                    // 파일 시스템에서 삭제
                    fileStorageUtil.deleteFiles(imagesToDelete.stream()
                            .map(NoticeImage::getImageUrl)
                            .collect(Collectors.toList()));
                    log.debug("사용하지 않는 이미지 파일 삭제 완료: count={}", imagesToDelete.size());

                    // DB에서 삭제
                    noticeImageRepository.deleteAll(imagesToDelete);
                    log.debug("사용하지 않는 이미지 DB 정보 삭제 완료: count={}", imagesToDelete.size());
                }
            } catch (NoticeFileException e) {
                log.error("이미지 파일 삭제 중 오류 발생: id={}", noticeId, e);
                throw e;
            } catch (IOException e) {
                log.error("이미지 파일 삭제 중 IO 오류 발생: id={}", noticeId, e);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_DELETE_ERROR,
                        "이미지 파일 삭제 중 오류가 발생했습니다.", e);
            } catch (DataAccessException e) {
                log.error("이미지 DB 정보 삭제 중 오류 발생: id={}", noticeId, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_UPDATE_ERROR,
                        "이미지 정보 삭제 중 오류가 발생했습니다.", e);
            }

            // 새 파일 처리
            if (newFiles != null && !newFiles.isEmpty()) {
                try {
                    // 파일 저장
                    List<String> newPaths = fileStorageUtil.saveFiles(newFiles, noticeId);
                    log.debug("새 파일 저장 완료: count={}", newPaths.size());

                    // 내용 업데이트
                    String updatedContent = fileStorageUtil.updateImagePaths(content, newPaths, noticeId);
                    notice.update(title, updatedContent, category);
                    log.debug("이미지 경로가 업데이트된 내용으로 공지사항 수정");

                    // 이미지 정보 저장
                    saveNoticeImages(newPaths, notice);
                    log.debug("새 이미지 정보 DB 저장 완료: count={}", newPaths.size());
                } catch (NoticeFileException e) {
                    log.error("새 파일 처리 중 오류 발생: id={}", noticeId, e);
                    throw e;
                } catch (IOException e) {
                    log.error("새 파일 저장 중 IO 오류 발생: id={}", noticeId, e);
                    throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                            "파일 저장 중 오류가 발생했습니다.", e);
                } catch (DataAccessException e) {
                    log.error("이미지 정보 저장 중 DB 오류 발생: id={}", noticeId, e);
                    throw new NoticeException(NoticeErrorCode.NOTICE_UPDATE_ERROR,
                            "이미지 정보 저장 중 오류가 발생했습니다.", e);
                }
            } else {
                // 파일 없이 공지사항 정보만 업데이트
                notice.update(title, content, category);
                log.debug("파일 변경 없이 공지사항 정보 수정");
            }

            log.info("공지사항 수정 완료: id={}, title={}", notice.getId(), notice.getTitle());
            return toResponseDto(notice);
        } catch (NoticeNotFoundException e) {
            // 공지사항을 찾을 수 없는 경우
            throw e;
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외
            throw e;
        } catch (Exception e) {
            log.error("공지사항 수정 중 예상치 못한 오류 발생: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_UPDATE_ERROR,
                    "공지사항 수정 중 시스템 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 삭제
     */
    public void deleteNotice(Long noticeId) throws IOException {
        if (noticeId == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "공지사항 ID는 필수입니다.");
        }

        try {
            log.debug("공지사항 삭제 시작: id={}", noticeId);

            // 공지사항 조회
            Notice notice;
            try {
                notice = noticeRepository.findById(noticeId)
                        .orElseThrow(() -> new NoticeNotFoundException("ID가 " + noticeId + "인 공지사항을 찾을 수 없습니다."));
                log.debug("삭제할 공지사항 조회 완료: id={}", noticeId);
            } catch (NoticeNotFoundException e) {
                log.warn("삭제할 공지사항을 찾을 수 없음: id={}", noticeId);
                throw e;
            } catch (DataAccessException e) {
                log.error("공지사항 조회 중 데이터베이스 오류 발생: id={}", noticeId, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                        "공지사항 조회 중 오류가 발생했습니다.", e);
            }

            // 이미지 처리
            try {
                List<NoticeImage> images = noticeImageRepository.findAllByNoticeId(noticeId);
                log.debug("공지사항 이미지 조회 완료: count={}", images.size());

                if (!images.isEmpty()) {
                    // 파일 시스템에서 삭제
                    List<String> imageUrls = images.stream().map(NoticeImage::getImageUrl).toList();
                    fileStorageUtil.deleteFiles(imageUrls);
                    log.debug("공지사항 파일 삭제 완료: count={}", imageUrls.size());

                    // DB에서 삭제
                    noticeImageRepository.deleteAll(images);
                    log.debug("공지사항 이미지 DB 정보 삭제 완료: count={}", images.size());
                }
            } catch (NoticeFileException e) {
                log.error("이미지 파일 삭제 중 오류 발생: id={}", noticeId, e);
                throw e;
            } catch (IOException e) {
                log.error("이미지 파일 삭제 중 IO 오류 발생: id={}", noticeId, e);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_DELETE_ERROR,
                        "이미지 파일 삭제 중 오류가 발생했습니다.", e);
            } catch (DataAccessException e) {
                log.error("이미지 DB 정보 삭제 중 오류 발생: id={}", noticeId, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_DELETE_ERROR,
                        "이미지 정보 삭제 중 오류가 발생했습니다.", e);
            }

            // 공지사항 삭제
            try {
                noticeRepository.delete(notice);
                log.debug("공지사항 DB 정보 삭제 완료: id={}", noticeId);
            } catch (DataAccessException e) {
                log.error("공지사항 삭제 중 데이터베이스 오류 발생: id={}", noticeId, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_DELETE_ERROR,
                        "공지사항 삭제 중 오류가 발생했습니다.", e);
            }

            log.info("공지사항 삭제 완료: id={}", noticeId);
        } catch (NoticeNotFoundException e) {
            // 공지사항을 찾을 수 없는 경우
            throw e;
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외
            throw e;
        } catch (Exception e) {
            log.error("공지사항 삭제 중 예상치 못한 오류 발생: id={}", noticeId, e);
            throw new NoticeException(NoticeErrorCode.NOTICE_DELETE_ERROR,
                    "공지사항 삭제 중 시스템 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이미지 정보 저장
     */
    private void saveNoticeImages(List<String> relativePaths, Notice notice) {
        for (String path : relativePaths) {
            try {
                NoticeImage image = NoticeImage.builder()
                        .imageUrl(path) // "notice_{id}/파일명"
                        .isVideo(fileStorageUtil.isVideo(path))
                        .notice(notice)
                        .build();
                noticeImageRepository.save(image);
                log.debug("이미지 정보 저장 완료: notice_id={}, path={}", notice.getId(), path);
            } catch (DataAccessException e) {
                log.error("이미지 정보 저장 중 DB 오류 발생: path={}", path, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR,
                        "이미지 정보 저장 중 오류가 발생했습니다.", e);
            } catch (Exception e) {
                log.error("이미지 정보 저장 중 예상치 못한 오류 발생: path={}", path, e);
                throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR,
                        "이미지 정보 저장 중 예상치 못한 오류가 발생했습니다.", e);
            }
        }
    }

    /**
     * 공지사항 엔티티를 DTO로 변환
     */
    private NoticeResponseDto toResponseDto(Notice notice) {
        try {
            List<String> imageUrls = noticeImageRepository.findAllByNoticeId(notice.getId())
                    .stream()
                    .map(image -> image.getImageUrl())
                    .collect(Collectors.toList());

            return NoticeResponseDto.builder()
                    .id(notice.getId())
                    .title(notice.getTitle())
                    .content(notice.getContent())
                    .category(notice.getCategory().name())
                    .createdDate(notice.getCreatedDate())
                    .imageUrls(imageUrls)
                    .build();
        } catch (DataAccessException e) {
            log.error("공지사항 DTO 변환 중 이미지 조회 오류: notice_id={}", notice.getId(), e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 이미지 정보를 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("공지사항 DTO 변환 중 예상치 못한 오류: notice_id={}", notice.getId(), e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 정보를 변환하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 사용자에게 알림 생성
     */
    private void sendNotificationToAllUsers(Notice notice) {
        try {
            log.debug("공지사항 알림 발송 시작: notice_id={}, title={}", notice.getId(), notice.getTitle());

            NotificationRequestDTO requestDTO = NotificationRequestDTO.builder()
                    .category(NotificationCategory.SHOPPING) // 쇼핑 카테고리
                    .type(NotificationType.ANNOUNCEMENT)    // 공지사항 타입
                    .message("새로운 공지사항: " + notice.getTitle())
                    .build();

            notificationCommandService.createNotificationForAll(requestDTO);
            log.debug("공지사항 알림 발송 완료: notice_id={}", notice.getId());
        } catch (Exception e) {
            log.error("공지사항 알림 발송 중 오류 발생: notice_id={}", notice.getId(), e);
            throw new NoticeException(NoticeErrorCode.NOTICE_NOTIFICATION_ERROR,
                    "공지사항 알림 발송 중 오류가 발생했습니다.", e);
        }
    }
}