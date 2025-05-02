package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.entity.StyleHashtag;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleHashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleHashtagQueryService {

    private final StyleHashtagRepository styleHashtagRepository;

    /**
     * 특정 스타일의 해시태그 목록 조회
     *
     * @param styleId 스타일 ID
     * @return 해시태그 응답 DTO 목록
     */
    public List<HashtagResponseDto> getHashtagsByStyleId(Long styleId) {
        log.debug("스타일의 해시태그 목록 조회 시작: styleId={}", styleId);

        try {
            if (styleId == null) {
                log.warn("스타일 ID가 null이므로 빈 목록 반환");
                return new ArrayList<>();
            }

            List<StyleHashtag> styleHashtags = styleHashtagRepository.findByStyleId(styleId);

            List<HashtagResponseDto> result = styleHashtags.stream()
                    .map(styleHashtag -> HashtagResponseDto.builder()
                            .id(styleHashtag.getHashtag().getId())
                            .name(styleHashtag.getHashtag().getName())
                            .count(styleHashtag.getHashtag().getCount())
                            .build())
                    .collect(Collectors.toList());

            log.debug("스타일의 해시태그 목록 조회 완료: styleId={}, 해시태그 수={}", styleId, result.size());

            return result;
        } catch (Exception e) {
            log.error("스타일의 해시태그 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 여러 스타일의 해시태그 목록 일괄 조회
     *
     * @param styleIds 스타일 ID 목록
     * @return 해시태그 응답 DTO 목록
     */
    public List<HashtagResponseDto> getHashtagsByStyleIds(List<Long> styleIds) {
        log.debug("여러 스타일의 해시태그 목록 일괄 조회 시작: 스타일 수={}",
                (styleIds != null ? styleIds.size() : 0));

        try {
            if (styleIds == null || styleIds.isEmpty()) {
                log.warn("스타일 ID 목록이 비어있어 빈 목록 반환");
                return new ArrayList<>();
            }

            // 여러 스타일의 해시태그 한 번에 조회
            List<StyleHashtag> styleHashtags = styleHashtagRepository.findByStyleIdIn(styleIds);
            log.debug("여러 스타일의 해시태그 조회 완료: 스타일 수={}, 스타일-해시태그 연결 수={}",
                    styleIds.size(), styleHashtags.size());

            List<HashtagResponseDto> result = styleHashtags.stream()
                    .map(styleHashtag -> HashtagResponseDto.builder()
                            .id(styleHashtag.getHashtag().getId())
                            .name(styleHashtag.getHashtag().getName())
                            .count(styleHashtag.getHashtag().getCount())
                            .build())
                    .collect(Collectors.toList());

            log.debug("여러 스타일의 해시태그 변환 완료: 해시태그 수={}", result.size());

            return result;
        } catch (Exception e) {
            log.error("여러 스타일의 해시태그 목록 일괄 조회 중 예상치 못한 오류 발생: 스타일 수={}",
                    (styleIds != null ? styleIds.size() : 0), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "여러 스타일의 해시태그 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 여러 스타일의 해시태그를 한 번에 조회하여 스타일 ID를 키로 하는 맵으로 반환
     * 성능 최적화를 위한 메서드
     *
     * @param styleIds 스타일 ID 목록
     * @return 스타일 ID를 키로 하는 해시태그 응답 DTO 목록 맵
     */
    public Map<Long, List<HashtagResponseDto>> getHashtagMapByStyleIds(List<Long> styleIds) {
        log.debug("스타일 ID별 해시태그 맵 조회 시작: 스타일 수={}",
                (styleIds != null ? styleIds.size() : 0));

        try {
            if (styleIds == null || styleIds.isEmpty()) {
                log.warn("스타일 ID 목록이 비어있어 빈 맵 반환");
                return Collections.emptyMap();
            }

            // 여러 스타일의 해시태그 한 번에 조회
            List<StyleHashtag> allStyleHashtags = styleHashtagRepository.findByStyleIdIn(styleIds);
            log.debug("스타일-해시태그 연결 조회 완료: 스타일 수={}, 연결 수={}",
                    styleIds.size(), allStyleHashtags.size());

            // 스타일 ID별로 해시태그 그룹화
            Map<Long, List<HashtagResponseDto>> result = allStyleHashtags.stream()
                    .collect(Collectors.groupingBy(
                            styleHashtag -> styleHashtag.getStyle().getId(),
                            Collectors.mapping(
                                    styleHashtag -> HashtagResponseDto.builder()
                                            .id(styleHashtag.getHashtag().getId())
                                            .name(styleHashtag.getHashtag().getName())
                                            .count(styleHashtag.getHashtag().getCount())
                                            .build(),
                                    Collectors.toList()
                            )
                    ));

            log.debug("스타일 ID별 해시태그 맵 생성 완료: 스타일 수={}, 맵 크기={}",
                    styleIds.size(), result.size());

            return result;
        } catch (Exception e) {
            log.error("스타일 ID별 해시태그 맵 조회 중 예상치 못한 오류 발생: 스타일 수={}",
                    (styleIds != null ? styleIds.size() : 0), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 ID별 해시태그 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}