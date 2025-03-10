package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.entity.Style;
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
     * 스타일 생성 (해시태그 처리 추가)
     */
    @PostMapping
    public ResponseEntity<Long> createStyle(
            @RequestParam("orderItemIds") List<Long> orderItemIds,
            @RequestParam("content") String content,
            @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestParam(value = "hashtags", required = false) List<String> hashtags
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 컨텍스트에서 이메일 추출
        Style createdStyle = styleCommandService.createStyle(email, orderItemIds, content, mediaFiles, hashtags);
        nginxCachePurgeUtil.purgeStyleCache();
        return ResponseEntity.ok(createdStyle.getId());
    }

    /**
     * 뷰 카운트 증가 (카프카로 대체되어 사용하지 않음)
     * 하위 호환성을 위해 유지
     */
    @PostMapping("/{styleId}/view")
    public ResponseEntity<Void> incrementViewCount(
            @PathVariable("styleId") Long styleId) {
        log.warn("레거시 뷰 카운트 API 호출: styleId={} - 이 엔드포인트는 향후 제거될 예정입니다", styleId);
        styleCommandService.incrementViewCount(styleId);
        return ResponseEntity.ok().build();
    }

    /**
     * 스타일 업데이트 (해시태그 처리 추가)
     */
    @PutMapping("/{styleId}")
    public ResponseEntity<Void> updateStyle(
            @PathVariable("styleId") Long styleId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "newMediaFiles", required = false) List<MultipartFile> newMediaFiles,
            @RequestParam(value = "existingUrlsFromFrontend", required = false) List<String> existingUrlsFromFrontend,
            @RequestParam(value = "hashtags", required = false) List<String> hashtags
    ) {
        styleCommandService.updateStyle(styleId, content, newMediaFiles, existingUrlsFromFrontend, hashtags);
        nginxCachePurgeUtil.purgeStyleCache();
        return ResponseEntity.ok().build();
    }

    /**
     * 스타일 삭제
     */
    @DeleteMapping("/{styleId}")
    public ResponseEntity<Void> deleteStyle(
            @PathVariable("styleId") Long styleId) {
        styleCommandService.deleteStyle(styleId);
        nginxCachePurgeUtil.purgeStyleCache();
        return ResponseEntity.ok().build();
    }
}