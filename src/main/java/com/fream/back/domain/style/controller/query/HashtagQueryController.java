package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.service.query.HashtagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hashtags/queries")
@RequiredArgsConstructor
public class HashtagQueryController {

    private final HashtagQueryService hashtagQueryService;

    /**
     * ID로 해시태그 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<HashtagResponseDto> getHashtagById(@PathVariable("id") Long id) {
        HashtagResponseDto hashtag = hashtagQueryService.findById(id);
        return ResponseEntity.ok(hashtag);
    }

    /**
     * 이름으로 해시태그 조회
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<HashtagResponseDto> getHashtagByName(@PathVariable("name") String name) {
        HashtagResponseDto hashtag = hashtagQueryService.findByName(name);
        return ResponseEntity.ok(hashtag);
    }

    /**
     * 모든 해시태그 페이징 조회
     */
    @GetMapping
    public ResponseEntity<Page<HashtagResponseDto>> getAllHashtags(Pageable pageable) {
        Page<HashtagResponseDto> hashtags = hashtagQueryService.findAll(pageable);
        return ResponseEntity.ok(hashtags);
    }

    /**
     * 인기 해시태그 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<HashtagResponseDto>> getPopularHashtags(Pageable pageable) {
        Page<HashtagResponseDto> hashtags = hashtagQueryService.findPopular(pageable);
        return ResponseEntity.ok(hashtags);
    }

    /**
     * 키워드로 해시태그 검색
     */
    @GetMapping("/search")
    public ResponseEntity<Page<HashtagResponseDto>> searchHashtags(
            @RequestParam("keyword") String keyword, Pageable pageable) {
        Page<HashtagResponseDto> hashtags = hashtagQueryService.search(keyword, pageable);
        return ResponseEntity.ok(hashtags);
    }

    /**
     * 자동완성을 위한 접두사 검색
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<HashtagResponseDto>> autocompleteHashtags(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "isKorean", required = false) Boolean isKorean) {

        List<HashtagResponseDto> hashtags;

        if (isKorean != null) {
            // 클라이언트가 언어 정보를 제공한 경우
            hashtags = hashtagQueryService.autocomplete(keyword, limit, isKorean);
        } else {
            // 언어 자동 감지
            hashtags = hashtagQueryService.smartAutocomplete(keyword, limit);
        }

        return ResponseEntity.ok(hashtags);
    }
}