package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.service.command.StyleCommandService;
import com.fream.back.global.utils.NginxCachePurgeUtil;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/styles/commands")
@RequiredArgsConstructor
public class StyleCommandController {

    private final StyleCommandService styleCommandService;
    private final NginxCachePurgeUtil nginxCachePurgeUtil;
    // 스타일 생성
    @PostMapping
    public ResponseEntity<Long> createStyle(
            @RequestParam("orderItemIds") List<Long> orderItemIds,
            @RequestParam("content") String content,
            @RequestParam(value = "mediaFiles", required = false) List<MultipartFile> mediaFiles
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 컨텍스트에서 이메일 추출
        Style createdStyle = styleCommandService.createStyle(email, orderItemIds, content, mediaFiles);
        nginxCachePurgeUtil.purgeStyleCache();
        return ResponseEntity.ok(createdStyle.getId());
    }

    // 뷰 카운트 증가
    @PostMapping("/{styleId}/view")
    public ResponseEntity<Void> incrementViewCount(
            @PathVariable("styleId") Long styleId) {
        styleCommandService.incrementViewCount(styleId);
        return ResponseEntity.ok().build();
    }

    // 스타일 업데이트
    @PutMapping("/{styleId}")
    public ResponseEntity<Void> updateStyle(
            @PathVariable("styleId") Long styleId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "newMediaFiles", required = false) List<MultipartFile> newMediaFiles,
            @RequestParam(value = "existingUrlsFromFrontend", required = false) List<String> existingUrlsFromFrontend
    ) {
        styleCommandService.updateStyle(styleId, content, newMediaFiles, existingUrlsFromFrontend);
        return ResponseEntity.ok().build();
    }


    // 스타일 삭제
    @DeleteMapping("/{styleId}")
    public ResponseEntity<Void> deleteStyle(
            @PathVariable("styleId") Long styleId) {
        styleCommandService.deleteStyle(styleId);
        return ResponseEntity.ok().build();
    }
}
