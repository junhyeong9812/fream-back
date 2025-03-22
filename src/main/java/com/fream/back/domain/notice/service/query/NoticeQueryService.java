package com.fream.back.domain.notice.service.query;

import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.entity.Notice;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.exception.NoticeErrorCode;
import com.fream.back.domain.notice.exception.NoticeException;
import com.fream.back.domain.notice.exception.NoticeFileException;
import com.fream.back.domain.notice.exception.NoticeNotFoundException;
import com.fream.back.domain.notice.repository.NoticeImageRepository;
import com.fream.back.domain.notice.repository.NoticeRepository;
import com.fream.back.domain.notice.service.command.NoticeFileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NoticeQueryService {

    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final NoticeFileStorageUtil fileStorageUtil;

    /**
     * 모든 공지사항 페이징 조회
     */
    public Page<NoticeResponseDto> getNotices(Pageable pageable) {
        try {
            log.debug("모든 공지사항 조회: page={}, size={}",
                    pageable.getPageNumber(), pageable.getPageSize());

            return noticeRepository.findAll(pageable).map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("공지사항 목록 조회 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 목록을 조회하는 중 오류가 발생했습니다.", e);
        } catch (NoticeFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("공지사항 목록 조회 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외는 그대로 전파
            log.error("공지사항 목록 조회 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 목록 조회 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 단일 공지사항 조회
     */
    public NoticeResponseDto getNotice(Long noticeId) {
        if (noticeId == null) {
            throw new NoticeNotFoundException("조회할 공지사항 ID가 필요합니다.");
        }

        try {
            log.debug("단일 공지사항 조회: ID={}", noticeId);

            Notice notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new NoticeNotFoundException("ID가 " + noticeId + "인 공지사항을 찾을 수 없습니다."));

            return toResponseDto(notice);
        } catch (NoticeNotFoundException e) {
            log.warn("공지사항 조회 중 공지를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (DataAccessException e) {
            log.error("공지사항 조회 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항을 조회하는 중 오류가 발생했습니다.", e);
        } catch (NoticeFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("공지사항 조회 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외는 그대로 전파
            log.error("공지사항 조회 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 조회 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리별 공지사항 조회
     */
    public Page<NoticeResponseDto> getNoticesByCategory(NoticeCategory category, Pageable pageable) {
        if (category == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_CATEGORY,
                    "공지사항 카테고리는 필수입니다.");
        }

        try {
            log.debug("카테고리별 공지사항 조회: category={}, page={}, size={}",
                    category, pageable.getPageNumber(), pageable.getPageSize());

            return noticeRepository.findByCategory(category, pageable).map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("카테고리별 공지사항 조회 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "카테고리별 공지사항을 조회하는 중 오류가 발생했습니다.", e);
        } catch (NoticeFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("카테고리별 공지사항 조회 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외는 그대로 전파
            log.error("카테고리별 공지사항 조회 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("카테고리별 공지사항 조회 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "카테고리별 공지사항을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 검색
     */
    public Page<NoticeResponseDto> searchNotices(String keyword, Pageable pageable) {
        try {
            log.debug("공지사항 검색: keyword={}, page={}, size={}",
                    keyword, pageable.getPageNumber(), pageable.getPageSize());

            return noticeRepository.searchNotices(keyword, pageable).map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("공지사항 검색 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 검색 중 오류가 발생했습니다.", e);
        } catch (NoticeFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.error("공지사항 검색 중 파일 관련 예외 발생: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외는 그대로 전파
            log.error("공지사항 검색 중 예외 발생: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("공지사항 검색 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 미리보기
     */
    public byte[] getFilePreview(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "조회할 파일명이 필요합니다.");
        }

        try {
            log.debug("파일 미리보기 요청: fileName={}", fileName);

            Path filePath = fileStorageUtil.getFilePath(fileName);
            if (!Files.exists(filePath)) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                        "파일이 존재하지 않습니다: " + fileName);
            }
            return Files.readAllBytes(filePath);
        } catch (NoticeFileException e) {
            // 파일 관련 예외는 그대로 전파
            log.warn("파일 미리보기 중 파일 예외 발생: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("파일 미리보기 중 IO 예외 발생: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 " + fileName + " 읽기 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("파일 미리보기 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "파일 " + fileName + " 조회 중 예상치 못한 오류가 발생했습니다.", e);
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
}