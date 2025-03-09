package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagQueryService {

    private final HashtagRepository hashtagRepository;

    /**
     * ID로 해시태그 조회
     */
    public HashtagResponseDto findById(Long id) {
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해시태그를 찾을 수 없습니다: " + id));

        return convertToDto(hashtag);
    }

    /**
     * 이름으로 해시태그 조회
     */
    public HashtagResponseDto findByName(String name) {
        Hashtag hashtag = hashtagRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("해시태그를 찾을 수 없습니다: " + name));

        return convertToDto(hashtag);
    }

    /**
     * 모든 해시태그 페이징 조회
     */
    public Page<HashtagResponseDto> findAll(Pageable pageable) {
        return hashtagRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    /**
     * 인기 해시태그 조회
     */
    public Page<HashtagResponseDto> findPopular(Pageable pageable) {
        return hashtagRepository.findAllByOrderByCountDesc(pageable)
                .map(this::convertToDto);
    }

    /**
     * 키워드로 해시태그 검색
     */
    public Page<HashtagResponseDto> search(String keyword, Pageable pageable) {
        return hashtagRepository.findByNameContainingOrderByCountDesc(keyword, pageable)
                .map(this::convertToDto);
    }

    /**
     * 자동완성을 위한 검색 - 언어에 따라 적절한 메서드 선택
     * isKorean 파라미터를 통해 한글 검색 여부 결정
     */
    public List<HashtagResponseDto> autocomplete(String keyword, int limit, boolean isKorean) {
        if (isKorean) {
            return koreanAutocomplete(keyword, limit);
        } else {
            return englishAutocomplete(keyword, limit);
        }
    }

    /**
     * 영어 등 알파벳 기반 언어를 위한 자동완성 (접두사 검색)
     */
    private List<HashtagResponseDto> englishAutocomplete(String prefix, int limit) {
        return hashtagRepository.findByNameStartingWithOrderByCountDesc(prefix, Pageable.ofSize(limit))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 한글 자동완성을 위한 검색 (접두사 또는 포함 검색)
     */
    private List<HashtagResponseDto> koreanAutocomplete(String keyword, int limit) {
        return hashtagRepository.findByNameContainingKeywordOrderByCountDesc(keyword, Pageable.ofSize(limit))
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 자동완성 통합 메서드 - 입력된 키워드를 분석하여 적절한 검색 방식 결정
     * (클라이언트에서 isKorean 정보를 전달하지 않는 경우 사용)
     */
    public List<HashtagResponseDto> smartAutocomplete(String keyword, int limit) {
        boolean containsKorean = containsKoreanCharacters(keyword);
        return autocomplete(keyword, limit, containsKorean);
    }

    /**
     * 입력된 문자열에 한글이 포함되어 있는지 확인
     */
    private boolean containsKoreanCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (char c : text.toCharArray()) {
            // 한글 유니코드 범위 확인 (가~힣)
            if (c >= '가' && c <= '힣') {
                return true;
            }
            // 한글 자음/모음 확인 (ㄱ~ㅎ, ㅏ~ㅣ)
            if ((c >= 'ㄱ' && c <= 'ㅎ') || (c >= 'ㅏ' && c <= 'ㅣ')) {
                return true;
            }
        }
        return false;
    }


    /**
     * 엔티티를 DTO로 변환
     */
    private HashtagResponseDto convertToDto(Hashtag hashtag) {
        return HashtagResponseDto.builder()
                .id(hashtag.getId())
                .name(hashtag.getName())
                .count(hashtag.getCount())
                .build();
    }
}