package com.fream.back.domain.notice.service.query;

import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.repository.NoticeImageRepository;
import com.fream.back.domain.notice.repository.NoticeRepository;
import com.fream.back.domain.notice.service.command.NoticeFileStorageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final NoticeFileStorageUtil fileStorageUtil;

    // 공지사항 목록 조회
    public Page<NoticeResponseDto> getNotices(Pageable pageable) {
        return noticeRepository.findAll(pageable).map(this::toResponseDto);
    }

    // 단일 공지사항 조회
    public NoticeResponseDto getNotice(Long noticeId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다."));
        return toResponseDto(notice);
    }

    // 카테고리별 조회
    public Page<NoticeResponseDto> getNoticesByCategory(NoticeCategory category, Pageable pageable) {
        return noticeRepository.findByCategory(category, pageable).map(this::toResponseDto);
    }

    // 검색 기능
    public Page<NoticeResponseDto> searchNotices(String keyword, Pageable pageable) {
        return noticeRepository.searchNotices(keyword, pageable).map(this::toResponseDto);
    }

    // 파일 미리보기
    public byte[] getFilePreview(String fileName) throws IOException {
        Path filePath = fileStorageUtil.getFilePath(fileName);
        return Files.readAllBytes(filePath);
    }

    private NoticeResponseDto toResponseDto(Notice notice) {
        return NoticeResponseDto.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory().name())
                .createdDate(notice.getCreatedDate())
                .imageUrls(noticeImageRepository.findAllByNoticeId(notice.getId())
                        .stream()
                        .map(image -> image.getImageUrl())
                        .collect(Collectors.toList()))
                .build();
    }
}
