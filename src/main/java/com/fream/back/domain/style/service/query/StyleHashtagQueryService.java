package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.entity.StyleHashtag;
import com.fream.back.domain.style.repository.StyleHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleHashtagQueryService {

    private final StyleHashtagRepository styleHashtagRepository;

    /**
     * 특정 스타일의 해시태그 목록 조회
     */
    public List<HashtagResponseDto> getHashtagsByStyleId(Long styleId) {
        if (styleId == null) {
            return new ArrayList<>();
        }

        List<StyleHashtag> styleHashtags = styleHashtagRepository.findByStyleId(styleId);

        return styleHashtags.stream()
                .map(styleHashtag -> HashtagResponseDto.builder()
                        .id(styleHashtag.getHashtag().getId())
                        .name(styleHashtag.getHashtag().getName())
                        .count(styleHashtag.getHashtag().getCount())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 여러 스타일의 해시태그 목록 일괄 조회
     */
    public List<HashtagResponseDto> getHashtagsByStyleIds(List<Long> styleIds) {
        if (styleIds == null || styleIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 여러 스타일의 해시태그 한 번에 조회
        List<StyleHashtag> styleHashtags = styleHashtagRepository.findByStyleIdIn(styleIds);

        return styleHashtags.stream()
                .map(styleHashtag -> HashtagResponseDto.builder()
                        .id(styleHashtag.getHashtag().getId())
                        .name(styleHashtag.getHashtag().getName())
                        .count(styleHashtag.getHashtag().getCount())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 여러 스타일의 해시태그를 한 번에 조회하여 스타일 ID를 키로 하는 맵으로 반환
     * 성능 최적화를 위한 메서드
     */
    public Map<Long, List<HashtagResponseDto>> getHashtagMapByStyleIds(List<Long> styleIds) {
        if (styleIds == null || styleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 여러 스타일의 해시태그 한 번에 조회
        List<StyleHashtag> allStyleHashtags = styleHashtagRepository.findByStyleIdIn(styleIds);

        // 스타일 ID별로 해시태그 그룹화
        return allStyleHashtags.stream()
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
    }
}