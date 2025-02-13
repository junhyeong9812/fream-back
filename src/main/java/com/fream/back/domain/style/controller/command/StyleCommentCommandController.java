package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.dto.AddCommentRequestDto;
import com.fream.back.domain.style.dto.UpdateCommentRequestDto;
import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.service.command.StyleCommentCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/styles/comments/commands")
@RequiredArgsConstructor
public class StyleCommentCommandController {

    private final StyleCommentCommandService styleCommentCommandService;

    // 댓글 생성
    @PostMapping
    public ResponseEntity<StyleComment> addComment(
            @RequestBody AddCommentRequestDto addCommentRequestDto
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 컨텍스트에서 이메일 추출
        StyleComment comment = styleCommentCommandService.addComment(
                email,
                addCommentRequestDto.getStyleId(),
                addCommentRequestDto.getContent(),
                addCommentRequestDto.getParentCommentId()
        );
        return ResponseEntity.ok(comment);
    }

    // 댓글 수정
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable("commentId") Long commentId,
            @RequestBody UpdateCommentRequestDto updateCommentRequestDto
    ) {
        styleCommentCommandService.updateComment(commentId,  updateCommentRequestDto.getUpdatedContent());
        return ResponseEntity.ok().build();
    }

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable("commentId") Long commentId) {
        styleCommentCommandService.deleteComment(commentId);
        return ResponseEntity.ok().build();
    }
}
