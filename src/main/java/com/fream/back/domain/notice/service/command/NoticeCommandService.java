package com.fream.back.domain.notice.service.command;

import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.entity.NoticeImage;
import com.fream.back.domain.notice.repository.NoticeImageRepository;
import com.fream.back.domain.notice.repository.NoticeRepository;
import com.fream.back.domain.notification.dto.NotificationRequestDTO;
import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NoticeCommandService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final NoticeFileStorageUtil fileStorageUtil;
    private final NotificationCommandService notificationCommandService;

    // 생성
    public NoticeResponseDto createNotice(String title, String content, NoticeCategory category, List<MultipartFile> files)
            throws IOException {
        Notice notice = Notice.builder()
                .title(title)
                .content(content)
                .category(category)
                .build();
        noticeRepository.save(notice);

        // 알림
        sendNotificationToAllUsers(notice);

        // 파일 저장
        if (files != null && !files.isEmpty()) {
            Long noticeId = notice.getId();  // DB 저장 후 ID
            // 1) 상대경로 리스트 (예: [ "notice_10/abc.png", "notice_10/def.png", ... ])
            List<String> relativePaths = fileStorageUtil.saveFiles(files, noticeId);

            // 2) HTML 치환 → 절대 URL
            String updatedContent = fileStorageUtil.updateImagePaths(content, relativePaths, noticeId);
            notice.updateContent(updatedContent);

            // 3) DB에 이미지(NoticeImage) 엔티티 저장
            saveNoticeImages(relativePaths, notice);
        }

        return toResponseDto(notice);
    }

    // 수정
    public NoticeResponseDto updateNotice(Long noticeId, String title, String content, NoticeCategory category,
                                          List<String> existingImageUrls, List<MultipartFile> newFiles) throws IOException {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));

        List<NoticeImage> existingImages = noticeImageRepository.findAllByNoticeId(noticeId);

        List<String> currentContentPaths = fileStorageUtil.extractImagePaths(content);

        // DB 이미지 중 content에 없는 것만 삭제
        List<NoticeImage> imagesToDelete = existingImages.stream()
                .filter(img -> !currentContentPaths.contains(img.getImageUrl()))
                .collect(Collectors.toList());
        fileStorageUtil.deleteFiles(imagesToDelete.stream()
                .map(NoticeImage::getImageUrl)
                .collect(Collectors.toList()));
        noticeImageRepository.deleteAll(imagesToDelete);

        // 새 파일
        if (newFiles != null && !newFiles.isEmpty()) {
            List<String> newPaths = fileStorageUtil.saveFiles(newFiles, noticeId);

            // 절대 URL로 치환
            String updatedContent = fileStorageUtil.updateImagePaths(content, newPaths, noticeId);
            notice.update(title, updatedContent, category);

            saveNoticeImages(newPaths, notice);
        } else {
            notice.update(title, content, category);
        }
        return toResponseDto(notice);
    }

    // 삭제
    public void deleteNotice(Long noticeId) throws IOException {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));

        // 이미지 파일 삭제
        List<NoticeImage> images = noticeImageRepository.findAllByNoticeId(noticeId);
        List<String> imageUrls = images.stream().map(NoticeImage::getImageUrl).toList();
        fileStorageUtil.deleteFiles(imageUrls);

        noticeImageRepository.deleteAll(images);
        noticeRepository.delete(notice);
    }

    private void saveNoticeImages(List<String> relativePaths, Notice notice) {
        for (String path : relativePaths) {
            NoticeImage image = NoticeImage.builder()
                    .imageUrl(path) // "notice_{id}/파일명"
                    .isVideo(fileStorageUtil.isVideo(path))
                    .notice(notice)
                    .build();
            noticeImageRepository.save(image);
        }
    }

    private NoticeResponseDto toResponseDto(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory().name())
                .createdDate(notice.getCreatedDate())
                .build();
    }


    // 모든 사용자에게 알림 생성
    private void sendNotificationToAllUsers(Notice notice) {
        NotificationRequestDTO requestDTO = NotificationRequestDTO.builder()
                .category(NotificationCategory.SHOPPING) // 쇼핑 카테고리
                .type(NotificationType.ANNOUNCEMENT)    // 공지사항 타입
                .message("새로운 공지사항: " + notice.getTitle())
                .build();

        notificationCommandService.createNotificationForAll(requestDTO);
    }
}
