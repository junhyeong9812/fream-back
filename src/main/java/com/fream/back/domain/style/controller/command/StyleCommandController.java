package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.command.StyleCommandService;
import com.fream.back.global.utils.NginxCachePurgeUtil;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/styles/commands")
@RequiredArgsConstructor
public class StyleCommandController {

    private final StyleCommandService styleCommandService;
    private final NginxCachePurgeUtil nginxCachePurgeUtil;

    /**
     * 스타일 생성 API (해시태그 처리 추가)
     *
     * @param orderItemIds 주문 아이템 ID 목록
     * @param content 스타일 내용
     * @param mediaFiles 미디어 파일 목록 (선택)
     * @param hashtags 해시태그 목록 (선택)
     * @return 생성된 스타일 ID
     */
    @PostMapping
    public ResponseEntity<Long> createStyle(
            @RequestParam("orderItemIds") List<Long> orderItemIds,
            @RequestParam("content") String content,
            @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestParam(value = "hashtags", required = false) List<String> hashtags
    ) {
        log.info("스타일 생성 요청: 주문 아이템 수={}, 미디어 파일 수={}, 해시태그 수={}",
                orderItemIds.size(),
                (mediaFiles != null ? mediaFiles.size() : 0),
                (hashtags != null ? hashtags.size() : 0));

        // 요청 유효성 검사
        if (orderItemIds == null || orderItemIds.isEmpty()) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "최소 하나 이상의 주문 아이템이 필요합니다.");
        }

        if (content == null || content.trim().isEmpty()) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 내용이 필요합니다.");
        }

        try {
            // SecurityContext에서 이메일 추출
            String email = SecurityUtils.extractEmailFromSecurityContext();
            if (email == null || email.isEmpty() || "anonymousUser".equals(email)) {
                log.error("인증되지 않은 사용자의 스타일 생성 시도");
                throw new StyleException(StyleErrorCode.STYLE_ACCESS_DENIED,
                        "로그인이 필요한 기능입니다.");
            }
            log.debug("사용자 이메일 추출: {}", email);

            Style createdStyle = styleCommandService.createStyle(email, orderItemIds, content, mediaFiles, hashtags);
            log.debug("스타일 생성 성공: styleId={}", createdStyle.getId());

            // 캐시 퍼지
            try {
                nginxCachePurgeUtil.purgeStyleCache();
                log.debug("Nginx 캐시 퍼지 성공");
            } catch (Exception e) {
                log.warn("Nginx 캐시 퍼지 실패: {}", e.getMessage());
                // 캐시 퍼지 실패는 전체 프로세스를 중단하지 않음
            }

            log.info("스타일 생성 완료: styleId={}", createdStyle.getId());
            return ResponseEntity.ok(createdStyle.getId());

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 생성 중 예상치 못한 오류 발생", e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 뷰 카운트 증가 API (카프카로 대체되어 사용하지 않음)
     * 하위 호환성을 위해 유지
     *
     * @param styleId 스타일 ID
     * @return 빈 응답
     */
    @PostMapping("/{styleId}/view")
    public ResponseEntity<Void> incrementViewCount(
            @PathVariable("styleId") Long styleId) {
        log.warn("레거시 뷰 카운트 API 호출: styleId={} - 이 엔드포인트는 향후 제거될 예정입니다", styleId);

        try {
            styleCommandService.incrementViewCount(styleId);
            log.debug("스타일 뷰 카운트 증가 완료: styleId={}", styleId);
            return ResponseEntity.ok().build();
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 뷰 카운트 증가 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_VIEW_EVENT_FAILED,
                    "스타일 뷰 카운트 증가 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스타일 업데이트 API (해시태그 처리 추가)
     *
     * @param styleId 업데이트할 스타일 ID
     * @param content 업데이트할 내용 (선택)
     * @param newMediaFiles 새로 추가할 미디어 파일 (선택)
     * @param existingUrlsFromFrontend 유지할 기존 미디어 URL (선택)
     * @param hashtags 업데이트할 해시태그 목록 (선택)
     * @return 빈 응답
     */
    @PutMapping("/{styleId}")
    public ResponseEntity<Void> updateStyle(
            @PathVariable("styleId") Long styleId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "newMediaFiles", required = false) List<MultipartFile> newMediaFiles,
            @RequestParam(value = "existingUrlsFromFrontend", required = false) List<String> existingUrlsFromFrontend,
            @RequestParam(value = "hashtags", required = false) List<String> hashtags
    ) {
        log.info("스타일 업데이트 요청: styleId={}, 유지할 URL 수={}, 새 미디어 파일 수={}, 해시태그 수={}",
                styleId,
                (existingUrlsFromFrontend != null ? existingUrlsFromFrontend.size() : 0),
                (newMediaFiles != null ? newMediaFiles.size() : 0),
                (hashtags != null ? hashtags.size() : 0));

        try {
            // 업데이트 실행
            styleCommandService.updateStyle(styleId, content, newMediaFiles, existingUrlsFromFrontend, hashtags);
            log.debug("스타일 업데이트 성공: styleId={}", styleId);

            // 캐시 퍼지
            try {
                nginxCachePurgeUtil.purgeStyleCache();
                log.debug("Nginx 캐시 퍼지 성공");
            } catch (Exception e) {
                log.warn("Nginx 캐시 퍼지 실패: {}", e.getMessage());
                // 캐시 퍼지 실패는 전체 프로세스를 중단하지 않음
            }

            log.info("스타일 업데이트 완료: styleId={}", styleId);
            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 업데이트 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스타일 삭제 API
     *
     * @param styleId 삭제할 스타일 ID
     * @return 빈 응답
     */
    @DeleteMapping("/{styleId}")
    public ResponseEntity<Void> deleteStyle(
            @PathVariable("styleId") Long styleId) {
        log.info("스타일 삭제 요청: styleId={}", styleId);

        try {
            styleCommandService.deleteStyle(styleId);
            log.debug("스타일 삭제 성공: styleId={}", styleId);

            // 캐시 퍼지
            try {
                nginxCachePurgeUtil.purgeStyleCache();
                log.debug("Nginx 캐시 퍼지 성공");
            } catch (Exception e) {
                log.warn("Nginx 캐시 퍼지 실패: {}", e.getMessage());
                // 캐시 퍼지 실패는 전체 프로세스를 중단하지 않음
            }

            log.info("스타일 삭제 완료: styleId={}", styleId);
            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 삭제 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 삭제 중 오류가 발생했습니다.", e);
        }
    }
}