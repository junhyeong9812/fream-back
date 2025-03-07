package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.StyleCommentsResponseDto;
import com.fream.back.domain.style.service.query.StyleCommentQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/styles/comments/queries")
@RequiredArgsConstructor
public class StyleCommentQueryController {

    private final StyleCommentQueryService styleCommentQueryService;

    /**
     * 특정 스타일의 댓글 목록 조회
     */
    @GetMapping("/{styleId}")
    public ResponseEntity<StyleCommentsResponseDto> getCommentsByStyleId(
            @PathVariable("styleId") Long styleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        StyleCommentsResponseDto comments = styleCommentQueryService.getCommentsByStyleId(styleId, email, page, size);
        return ResponseEntity.ok(comments);
    }
}