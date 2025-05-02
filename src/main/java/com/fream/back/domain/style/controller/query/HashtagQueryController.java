package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.query.HashtagQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/hashtags/queries")
@RequiredArgsConstructor
public class HashtagQueryController {

    private final HashtagQueryService hashtagQueryService;

    /**
     * ID로 해시태그 조회 API
     *
     * @param id 해시태그 ID
     * @return 해시태그 응답 DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<HashtagResponseDto> getHashtagById(@PathVariable("id") Long id) {
        log.info("ID로 해시태그 조회 요청: hashtagId={}", id);

        try {
            HashtagResponseDto hashtag = hashtagQueryService.findById(id);
            log.info("해시태그 조회 완료: hashtagId={}, name={}", hashtag.getId(), hashtag.getName());

            return ResponseEntity.ok(hashtag);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 조회 중 예상치 못한 오류 발생: hashtagId={}", id, e);
            throw new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND,
                    "해시태그 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이름으로 해시태그 조회 API
     *
     * @param name 해시태그 이름
     * @return 해시태그 응답 DTO
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<HashtagResponseDto> getHashtagByName(@PathVariable("name") String name) {
        log.info("이름으로 해시태그 조회 요청: name={}", name);

        try {
            HashtagResponseDto hashtag = hashtagQueryService.findByName(name);
            log.info("해시태그 조회 완료: hashtagId={}, name={}", hashtag.getId(), hashtag.getName());

            return ResponseEntity.ok(hashtag);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("이름으로 해시태그 조회 중 예상치 못한 오류 발생: name={}", name, e);
            throw new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND,
                    "해시태그 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 해시태그 페이징 조회 API
     *
     * @param pageable 페이징 정보
     * @return 해시태그 페이징 목록
     */
    @GetMapping
    public ResponseEntity<Page<HashtagResponseDto>> getAllHashtags(Pageable pageable) {
        log.info("모든 해시태그 페이징 조회 요청: 페이지={}, 사이즈={}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<HashtagResponseDto> hashtags = hashtagQueryService.findAll(pageable);
            log.info("해시태그 목록 조회 완료: 결과 수={}, 총 페이지={}",
                    hashtags.getNumberOfElements(), hashtags.getTotalPages());

            return ResponseEntity.ok(hashtags);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 목록 조회 중 예상치 못한 오류 발생", e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 인기 해시태그 조회 API
     *
     * @param pageable 페이징 정보
     * @return 인기 해시태그 페이징 목록
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<HashtagResponseDto>> getPopularHashtags(Pageable pageable) {
        log.info("인기 해시태그 조회 요청: 페이지={}, 사이즈={}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<HashtagResponseDto> hashtags = hashtagQueryService.findPopular(pageable);
            log.info("인기 해시태그 조회 완료: 결과 수={}, 총 페이지={}",
                    hashtags.getNumberOfElements(), hashtags.getTotalPages());

            return ResponseEntity.ok(hashtags);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("인기 해시태그 조회 중 예상치 못한 오류 발생", e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "인기 해시태그 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 키워드로 해시태그 검색 API
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 해시태그 페이징 목록
     */
    @GetMapping("/search")
    public ResponseEntity<Page<HashtagResponseDto>> searchHashtags(
            @RequestParam("keyword") String keyword, Pageable pageable) {
        log.info("키워드로 해시태그 검색 요청: keyword={}, 페이지={}, 사이즈={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<HashtagResponseDto> hashtags = hashtagQueryService.search(keyword, pageable);
            log.info("해시태그 검색 완료: keyword={}, 결과 수={}, 총 페이지={}",
                    keyword, hashtags.getNumberOfElements(), hashtags.getTotalPages());

            return ResponseEntity.ok(hashtags);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 검색 중 예상치 못한 오류 발생: keyword={}", keyword, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 자동완성을 위한 접두사 검색 API
     *
     * @param keyword 검색 키워드
     * @param limit 결과 제한 수
     * @param isKorean 한글 키워드 여부 (선택)
     * @return 자동완성 해시태그 목록
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<HashtagResponseDto>> autocompleteHashtags(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "isKorean", required = false) Boolean isKorean) {
        log.info("해시태그 자동완성 요청: keyword={}, limit={}, isKorean={}",
                keyword, limit, isKorean);

        try {
            List<HashtagResponseDto> hashtags;

            if (isKorean != null) {
                // 클라이언트가 언어 정보를 제공한 경우
                hashtags = hashtagQueryService.autocomplete(keyword, limit, isKorean);
                log.debug("언어 정보 제공 기반 자동완성: isKorean={}", isKorean);
            } else {
                // 언어 자동 감지
                hashtags = hashtagQueryService.smartAutocomplete(keyword, limit);
                log.debug("언어 자동 감지 기반 자동완성");
            }

            log.info("해시태그 자동완성 완료: keyword={}, 결과 수={}", keyword, hashtags.size());
            return ResponseEntity.ok(hashtags);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 자동완성 중 예상치 못한 오류 발생: keyword={}", keyword, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 자동완성 중 오류가 발생했습니다.", e);
        }
    }
}