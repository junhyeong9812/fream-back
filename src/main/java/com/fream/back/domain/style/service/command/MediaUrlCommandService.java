package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.MediaUrlRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MediaUrlCommandService {

    private final MediaUrlRepository mediaUrlRepository;
    private final FileUtils fileUtils; // 파일 저장 유틸리티

    // style/{styleId} 폴더
    private static final String STYLE_FOLDER_PREFIX = "styles";
    // => 최종 "/home/ubuntu/fream/styles/{styleId}"
    private static final String API_URL_PREFIX = "/styles/queries";

    // 허용되는 이미지 타입 목록
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/bmp", "image/webp"
    );

    // 허용되는 비디오 타입 목록
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
            "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo", "video/webm"
    );

    // 최대 파일 크기 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 미디어 파일 저장 및 MediaUrl 생성
     *
     * @param style 스타일 엔티티
     * @param mediaFile 미디어 파일
     * @return 생성된 MediaUrl 엔티티
     * @throws StyleException 파일 저장 실패 시
     */
    public MediaUrl saveMediaFile(Style style, MultipartFile mediaFile) {
        log.debug("미디어 파일 저장 시작: styleId={}, 파일명={}, 파일크기={}, 타입={}",
                style.getId(), mediaFile.getOriginalFilename(),
                mediaFile.getSize(), mediaFile.getContentType());

        try {
            // 유효성 검사
            validateMediaFile(mediaFile);

            // 1. 파일 시스템에 저장할 경로 설정
            String directory = STYLE_FOLDER_PREFIX + "/" + style.getId();

            // 2. 파일 저장 및 유니크 파일명 생성
            String savedFileName = fileUtils.saveFile(directory, "media_", mediaFile);
            log.debug("파일 저장 완료: styleId={}, 원본파일명={}, 저장파일명={}",
                    style.getId(), mediaFile.getOriginalFilename(), savedFileName);

            // 3. API 접근을 위한 URL 생성
            String url = String.format("%s/%d/media/%s", API_URL_PREFIX, style.getId(), savedFileName);

            // 4. MediaUrl 엔티티 생성 및 저장
            MediaUrl mediaUrl = MediaUrl.builder()
                    .style(style)
                    .url(url)  // 전체 URL 경로 저장
                    .build();

            // 5. 양방향 연관관계 설정
            mediaUrl.assignStyle(style);

            MediaUrl savedMediaUrl = mediaUrlRepository.save(mediaUrl);
            log.info("미디어 URL 저장 완료: mediaUrlId={}, styleId={}, url={}",
                    savedMediaUrl.getId(), style.getId(), savedMediaUrl.getUrl());

            return savedMediaUrl;

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("미디어 파일 저장 중 오류 발생: styleId={}, 파일명={}",
                    style.getId(), mediaFile.getOriginalFilename(), e);
            throw new StyleException(StyleErrorCode.MEDIA_FILE_UPLOAD_FAILED,
                    "미디어 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 미디어 URL 및 관련 파일 삭제
     *
     * @param mediaUrl 삭제할 미디어 URL 엔티티
     * @throws StyleException 파일 삭제 실패 시
     */
    public void deleteMediaUrl(MediaUrl mediaUrl) {
        log.debug("미디어 URL 삭제 시작: mediaUrlId={}, styleId={}, url={}",
                mediaUrl.getId(), mediaUrl.getStyle().getId(), mediaUrl.getUrl());

        try {
            // 1. URL에서 파일명 추출
            String url = mediaUrl.getUrl();
            String fileName = url.substring(url.lastIndexOf('/') + 1);

            // 2. 파일 시스템 경로 설정
            String directory = STYLE_FOLDER_PREFIX + "/" + mediaUrl.getStyle().getId();

            // 3. 파일 삭제
            boolean fileDeleted = fileUtils.deleteFile(directory, fileName);
            if (fileDeleted) {
                log.debug("파일 삭제 성공: directory={}, fileName={}", directory, fileName);
            } else {
                log.warn("파일 삭제 실패 또는 파일이 존재하지 않음: directory={}, fileName={}",
                        directory, fileName);
            }

            // 4. 엔티티 삭제
            mediaUrl.getStyle().removeMediaUrl(mediaUrl);
            mediaUrlRepository.delete(mediaUrl);
            log.info("미디어 URL 삭제 완료: mediaUrlId={}, styleId={}",
                    mediaUrl.getId(), mediaUrl.getStyle().getId());

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("미디어 URL 삭제 중 오류 발생: mediaUrlId={}, styleId={}",
                    mediaUrl.getId(), mediaUrl.getStyle().getId(), e);
            throw new StyleException(StyleErrorCode.MEDIA_FILE_NOT_FOUND,
                    "미디어 파일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 미디어 파일 유효성 검사
     *
     * @param mediaFile 검사할 미디어 파일
     * @throws StyleException 유효성 검사 실패 시
     */
    private void validateMediaFile(MultipartFile mediaFile) {
        // 파일 존재 여부 및 빈 파일 확인
        if (mediaFile == null || mediaFile.isEmpty()) {
            throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID, "파일이 비어 있습니다.");
        }

        // 파일 크기 제한 확인
        if (mediaFile.getSize() > MAX_FILE_SIZE) {
            throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID,
                    String.format("파일 크기가 제한을 초과했습니다 (최대 %d MB).", MAX_FILE_SIZE / (1024 * 1024)));
        }

        // 파일 타입 확인
        String contentType = mediaFile.getContentType();
        if (contentType == null) {
            throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID, "파일 타입을 확인할 수 없습니다.");
        }

        boolean isAllowedType = ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase()) ||
                ALLOWED_VIDEO_TYPES.contains(contentType.toLowerCase());
        if (!isAllowedType) {
            throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID,
                    "지원하지 않는 파일 형식입니다. 이미지 또는 비디오 파일만 업로드 가능합니다.");
        }

        // 파일명 유효성 검사
        String originalFilename = mediaFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID, "파일명이 유효하지 않습니다.");
        }
    }
}