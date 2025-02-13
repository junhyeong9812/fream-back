package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.service.command.StyleInterestCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/styles/interests/commands")
@RequiredArgsConstructor
public class StyleInterestCommandController {

    private final StyleInterestCommandService
            styleInterestCommandService;

    // 스타일 관심 토글
    @PostMapping("/{styleId}/toggle")
    public ResponseEntity<Void> toggleInterest(
            @PathVariable("styleId") Long styleId
    ) {
        String email = SecurityUtils.extractEmailFromSecurityContext(); // 컨텍스트에서 이메일 추출
        styleInterestCommandService.toggleStyleInterest(email, styleId);
        return ResponseEntity.ok().build();
    }
}
