package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.MediaUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaUrlQueryService {

    private final MediaUrlRepository mediaUrlRepository;

    /**
     * 특정 스타일의 모든 미디어 URL 목록을 조회합니다.
     *
     * @param styleId 스타일 ID
     * @return 미디어 URL 엔티티 목록
     * @throws StyleException 조회 실패 시
     */
    public List<MediaUrl> findMediaUrlsByStyleId(Long styleId) {
        log.debug("스타일의 미디어 URL 목록 조회 시작: styleId={}", styleId);

        try {
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                        "스타일 ID가 필요합니다.");
            }

            List<MediaUrl> mediaUrls = mediaUrlRepository.findByStyleId(styleId);
            log.debug("스타일의 미디어 URL 목록 조회 완료: styleId={}, 미디어 URL 수={}",
                    styleId, mediaUrls.size());

            return mediaUrls;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일의 미디어 URL 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "미디어 URL 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 미디어 URL ID로 미디어 URL 엔티티를 조회합니다.
     *
     * @param mediaUrlId 미디어 URL ID
     * @return 미디어 URL 엔티티
     * @throws StyleException 미디어 URL을 찾을 수 없는 경우
     */
    public MediaUrl findMediaUrlById(Long mediaUrlId) {
        log.debug("미디어 URL ID로 조회 시작: mediaUrlId={}", mediaUrlId);

        try {
            if (mediaUrlId == null) {
                throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID,
                        "미디어 URL ID가 필요합니다.");
            }

            MediaUrl mediaUrl = mediaUrlRepository.findById(mediaUrlId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.MEDIA_FILE_NOT_FOUND,
                            "미디어 URL을 찾을 수 없습니다: " + mediaUrlId));

            log.debug("미디어 URL ID로 조회 완료: mediaUrlId={}, styleId={}",
                    mediaUrlId, mediaUrl.getStyle().getId());

            return mediaUrl;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("미디어 URL ID로 조회 중 예상치 못한 오류 발생: mediaUrlId={}", mediaUrlId, e);
            throw new StyleException(StyleErrorCode.MEDIA_FILE_NOT_FOUND,
                    "미디어 URL 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * URL 값으로 미디어 URL 엔티티를 조회합니다.
     *
     * @param url URL 문자열
     * @return 미디어 URL 엔티티 목록
     * @throws StyleException 조회 실패 시
     */
    public List<MediaUrl> findMediaUrlsByUrl(String url) {
        log.debug("URL 값으로 미디어 URL 조회 시작: url={}", url);

        try {
            if (url == null || url.trim().isEmpty()) {
                throw new StyleException(StyleErrorCode.MEDIA_FILE_INVALID,
                        "미디어 URL 값이 필요합니다.");
            }

            List<MediaUrl> mediaUrls = mediaUrlRepository.findByUrl(url);
            log.debug("URL 값으로 미디어 URL 조회 완료: url={}, 결과 수={}", url, mediaUrls.size());

            return mediaUrls;

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("URL 값으로 미디어 URL 조회 중 예상치 못한 오류 발생: url={}", url, e);
            throw new StyleException(StyleErrorCode.MEDIA_FILE_NOT_FOUND,
                    "미디어 URL 조회 중 오류가 발생했습니다.", e);
        }
    }
}
