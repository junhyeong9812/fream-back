package com.fream.back.domain.notice.service.query;

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
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 공지사항 조회 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NoticeQueryService {

    // 상수 정의
//    private static final String NOTICE_BASE_DIR = "/home/ubuntu/fream/notice";
    private static final String NOTICE_BASE_DIR = "C:\\Users\\pickj\\webserver\\dockerVolums\\fream\\notice";
    // 의존성 주입
    private final NoticeRepository noticeRepository;
    private final NoticeImageRepository noticeImageRepository;
    private final FileUtils fileUtils;

    /**
     * 모든 공지사항 페이징 조회
     *
     * @param pageable 페이징 정보
     * @return 공지사항 목록
     */
    public Page<NoticeResponseDto> getNotices(Pageable pageable) {
        log.debug("모든 공지사항 조회: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        try {
            return noticeRepository.findAllOrderByCreatedDateDesc(pageable)
                    .map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("공지사항 목록 조회 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 목록을 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("공지사항 목록 조회 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 목록을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 단일 공지사항 조회
     *
     * @param noticeId 공지사항 ID
     * @return 공지사항 정보
     */
    public NoticeResponseDto getNotice(Long noticeId) {
        log.debug("단일 공지사항 조회: ID={}", noticeId);

        try {
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
        } catch (Exception e) {
            log.error("공지사항 조회 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 카테고리별 공지사항 조회
     *
     * @param category 공지사항 카테고리
     * @param pageable 페이징 정보
     * @return 공지사항 목록
     */
    public Page<NoticeResponseDto> getNoticesByCategory(NoticeCategory category, Pageable pageable) {
        log.debug("카테고리별 공지사항 조회: category={}, page={}, size={}",
                category, pageable.getPageNumber(), pageable.getPageSize());

        try {
            return noticeRepository.findByCategoryOrderByCreatedDateDesc(category, pageable)
                    .map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("카테고리별 공지사항 조회 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "카테고리별 공지사항을 조회하는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("카테고리별 공지사항 조회 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "카테고리별 공지사항을 조회하는 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과 목록
     */
    public Page<NoticeResponseDto> searchNotices(String keyword, Pageable pageable) {
        log.debug("공지사항 검색: keyword={}, page={}, size={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            return noticeRepository.searchNotices(keyword, pageable)
                    .map(this::toResponseDto);
        } catch (DataAccessException e) {
            log.error("공지사항 검색 중 데이터베이스 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 검색 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("공지사항 검색 중 예상치 못한 오류 발생: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_QUERY_ERROR,
                    "공지사항 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 공지사항 파일 리소스 조회
     *
     * @param noticeId 공지사항 ID
     * @param fileName 파일명
     * @return 파일 리소스
     */
    public Resource getNoticeFileResource(Long noticeId, String fileName) {
        log.debug("공지사항 파일 리소스 조회: noticeId={}, fileName={}", noticeId, fileName);

        try {
            // 파일 경로 구성
            String directory = "notice_" + noticeId;
            Path filePath = Paths.get(NOTICE_BASE_DIR, directory, fileName).normalize();

            // 디렉토리 탐색 방지 (Path Traversal 취약점 방지)
            if (!filePath.startsWith(Paths.get(NOTICE_BASE_DIR))) {
                log.warn("잘못된 파일 경로 요청: {}", filePath);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "잘못된 파일 경로 요청");
            }

            // 파일 존재 확인
            File file = filePath.toFile();
            if (!file.exists() || !file.isFile()) {
                log.warn("요청한 파일을 찾을 수 없음: {}", filePath);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                        "요청한 파일을 찾을 수 없습니다: " + fileName);
            }

            // 리소스 생성
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("파일을 읽을 수 없음: {}", filePath);
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                        "파일을 읽을 수 없습니다: " + fileName);
            }

            return resource;
        } catch (MalformedURLException e) {
            log.error("파일 URL 생성 중 오류: fileName={}", fileName, e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 접근 중 오류가 발생했습니다: " + fileName, e);
        } catch (NoticeFileException e) {
            throw e;
        } catch (Exception e) {
            log.error("파일 리소스 조회 중 예상치 못한 오류: fileName={}", fileName, e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND,
                    "파일 리소스 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 미리보기 (바이트 배열 반환)
     *
     * @param fileName 파일명
     * @return 파일 데이터
     * @throws IOException 파일 읽기 오류 발생 시
     */
    public byte[] getFilePreview(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND, "조회할 파일명이 필요합니다.");
        }

        try {
            log.debug("파일 미리보기 요청: fileName={}", fileName);

            // 전체 경로 구성
            int lastSlashIndex = fileName.lastIndexOf('/');
            String directory = "";
            String name = fileName;

            if (lastSlashIndex > 0) {
                directory = fileName.substring(0, lastSlashIndex);
                name = fileName.substring(lastSlashIndex + 1);
            }

            // 파일 존재 확인
            if (!fileUtils.existsFile(directory, name)) {
                throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_NOT_FOUND, "파일이 존재하지 않습니다: " + fileName);
            }

            // 파일 경로 및 데이터 읽기
            File file = new File(NOTICE_BASE_DIR + File.separator + directory + File.separator + name);
            return java.nio.file.Files.readAllBytes(file.toPath());
        } catch (NoticeFileException e) {
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
     *
     * @param notice 공지사항 엔티티
     * @return 공지사항 응답 DTO
     */
    private NoticeResponseDto toResponseDto(Notice notice) {
        try {
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