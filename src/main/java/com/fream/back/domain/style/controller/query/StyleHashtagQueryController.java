package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.query.StyleHashtagQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/styles/queries")
@RequiredArgsConstructor
public class StyleHashtagQueryController {

    private final StyleHashtagQueryService styleHashtagQueryService;

    /**
     * 특정 스타일의 해시태그 목록 조회 API
     *
     * @param styleId 스타일 ID
     * @return 해시태그 목록
     */
    @GetMapping("/{styleId}/hashtags")
    public ResponseEntity<List<HashtagResponseDto>> getStyleHashtags(@PathVariable("styleId") Long styleId) {
        log.info("스타일 해시태그 목록 조회 요청: styleId={}", styleId);

        try {
            // 입력값 검증
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            List<HashtagResponseDto> hashtags = styleHashtagQueryService.getHashtagsByStyleId(styleId);
            log.info("스타일 해시태그 목록 조회 완료: styleId={}, 해시태그 수={}", styleId, hashtags.size());

            return ResponseEntity.ok(hashtags);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 해시태그 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 해시태그 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}