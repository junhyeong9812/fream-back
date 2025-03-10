package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.service.query.StyleHashtagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/styles/queries")
@RequiredArgsConstructor
public class StyleHashtagQueryController {

    private final StyleHashtagQueryService styleHashtagQueryService;

    /**
     * 특정 스타일의 해시태그 목록 조회
     */
    @GetMapping("/{styleId}/hashtags")
    public ResponseEntity<List<HashtagResponseDto>> getStyleHashtags(@PathVariable("styleId") Long styleId) {
        List<HashtagResponseDto> hashtags = styleHashtagQueryService.getHashtagsByStyleId(styleId);
        return ResponseEntity.ok(hashtags);
    }
}