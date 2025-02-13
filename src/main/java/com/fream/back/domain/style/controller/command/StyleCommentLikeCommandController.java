package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.service.command.StyleCommentLikeCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/styles/comments/likes/commands")
@RequiredArgsConstructor
public class StyleCommentLikeCommandController {

    private final StyleCommentLikeCommandService styleCommentLikeCommandService;

    // 댓글 좋아요 토글
    @PostMapping("/{commentId}/toggle")
    public ResponseEntity<Void> toggleCommentLike(
            @PathVariable("commentId") Long commentId
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 컨텍스트에서 이메일 추출
        styleCommentLikeCommandService.toggleCommentLike(email, commentId);
        return ResponseEntity.ok().build();
    }
}
