package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.service.command.StyleLikeCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/styles/likes/commands")
@RequiredArgsConstructor
public class StyleLikeCommandController {

    private final StyleLikeCommandService styleLikeCommandService;

    // 스타일 좋아요 토글
    @PostMapping("/{styleId}/toggle")
    public ResponseEntity<Void> toggleLike(
            @PathVariable("styleId") Long styleId
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 컨텍스트에서 이메일 추출
        styleLikeCommandService.toggleLike(email, styleId);
        return ResponseEntity.ok().build();
    }
}
