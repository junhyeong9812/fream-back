package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.HashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HashtagQueryService {

    private final HashtagRepository hashtagRepository;

    /**
     * ID로 해시태그 조회
     *
     * @param id 해시태그 ID
     * @return 해시태그 응답 DTO
     * @throws StyleException 해시태그를 찾을 수 없는 경우
     */
    public HashtagResponseDto findById(Long id) {
        log.debug("ID로 해시태그 조회 시작: hashtagId={}", id);

        try {
            Hashtag hashtag = hashtagRepository.findById(id)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND,
                            "해시태그를 찾을 수 없습니다: " + id));

            HashtagResponseDto responseDto = convertToDto(hashtag);
            log.debug("ID로 해시태그 조회 완료: hashtagId={}, name={}", id, hashtag.getName());

            return responseDto;
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
     * 이름으로 해시태그 조회
     *
     * @param name 해시태그 이름
     * @return 해시태그 응답 DTO
     * @throws StyleException 해시태그를 찾을 수 없는 경우
     */
    public HashtagResponseDto findByName(String name) {
        log.debug("이름으로 해시태그 조회 시작: name={}", name);

        try {
            Hashtag hashtag = hashtagRepository.findByName(name)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND,
                            "해시태그를 찾을 수 없습니다: " + name));

            HashtagResponseDto responseDto = convertToDto(hashtag);
            log.debug("이름으로 해시태그 조회 완료: hashtagId={}, name={}", hashtag.getId(), name);

            return responseDto;
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
     * 모든 해시태그 페이징 조회
     *
     * @param pageable 페이징 정보
     * @return 해시태그 응답 DTO 페이지
     */
    public Page<HashtagResponseDto> findAll(Pageable pageable) {
        log.debug("모든 해시태그 페이징 조회 시작: 페이지={}, 크기={}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<HashtagResponseDto> result = hashtagRepository.findAll(pageable)
                    .map(this::convertToDto);

            log.debug("모든 해시태그 페이징 조회 완료: 결과 수={}, 총 페이지={}",
                    result.getNumberOfElements(), result.getTotalPages());

            return result;
        } catch (Exception e) {
            log.error("모든 해시태그 페이징 조회 중 예상치 못한 오류 발생: 페이지={}, 크기={}",
                    pageable.getPageNumber(), pageable.getPageSize(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 인기 해시태그 조회
     *
     * @param pageable 페이징 정보
     * @return 인기 해시태그 응답 DTO 페이지
     */
    public Page<HashtagResponseDto> findPopular(Pageable pageable) {
        log.debug("인기 해시태그 조회 시작: 페이지={}, 크기={}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<HashtagResponseDto> result = hashtagRepository.findAllByOrderByCountDesc(pageable)
                    .map(this::convertToDto);

            log.debug("인기 해시태그 조회 완료: 결과 수={}, 총 페이지={}",
                    result.getNumberOfElements(), result.getTotalPages());

            return result;
        } catch (Exception e) {
            log.error("인기 해시태그 조회 중 예상치 못한 오류 발생: 페이지={}, 크기={}",
                    pageable.getPageNumber(), pageable.getPageSize(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "인기 해시태그 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 키워드로 해시태그 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 해시태그 응답 DTO 페이지
     */
    public Page<HashtagResponseDto> search(String keyword, Pageable pageable) {
        log.debug("키워드로 해시태그 검색 시작: keyword={}, 페이지={}, 크기={}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<HashtagResponseDto> result = hashtagRepository.findByNameContainingOrderByCountDesc(keyword, pageable)
                    .map(this::convertToDto);

            log.debug("키워드로 해시태그 검색 완료: keyword={}, 결과 수={}, 총 페이지={}",
                    keyword, result.getNumberOfElements(), result.getTotalPages());

            return result;
        } catch (Exception e) {
            log.error("키워드로 해시태그 검색 중 예상치 못한 오류 발생: keyword={}, 페이지={}, 크기={}",
                    keyword, pageable.getPageNumber(), pageable.getPageSize(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 자동완성을 위한 검색 - 언어에 따라 적절한 메서드 선택
     * isKorean 파라미터를 통해 한글 검색 여부 결정
     *
     * @param keyword 검색 키워드
     * @param limit 결과 제한 수
     * @param isKorean 한글 키워드 여부
     * @return 자동완성 해시태그 응답 DTO 목록
     */
    public List<HashtagResponseDto> autocomplete(String keyword, int limit, boolean isKorean) {
        log.debug("해시태그 자동완성 검색 시작: keyword={}, limit={}, isKorean={}", keyword, limit, isKorean);

        try {
            List<HashtagResponseDto> result;

            if (isKoreanChosung(keyword)) {
                result = chosungAutocomplete(keyword, limit);
                log.debug("초성 기반 자동완성 실행");
            } else if (isKorean) {
                result = koreanAutocomplete(keyword, limit);
                log.debug("한글 기반 자동완성 실행");
            } else {
                result = englishAutocomplete(keyword, limit);
                log.debug("영문 기반 자동완성 실행");
            }

            log.debug("해시태그 자동완성 검색 완료: keyword={}, 결과 수={}", keyword, result.size());
            return result;

        } catch (Exception e) {
            log.error("해시태그 자동완성 검색 중 예상치 못한 오류 발생: keyword={}, limit={}", keyword, limit, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 자동완성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 영어 등 알파벳 기반 언어를 위한 자동완성 (접두사 검색)
     *
     * @param prefix 접두사
     * @param limit 결과 제한 수
     * @return 자동완성 해시태그 응답 DTO 목록
     */
    private List<HashtagResponseDto> englishAutocomplete(String prefix, int limit) {
        log.debug("영문 기반 자동완성 실행: prefix={}, limit={}", prefix, limit);

        try {
            List<HashtagResponseDto> result = hashtagRepository.findByNameStartingWithOrderByCountDesc(prefix, Pageable.ofSize(limit))
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.debug("영문 기반 자동완성 완료: prefix={}, 결과 수={}", prefix, result.size());
            return result;
        } catch (Exception e) {
            log.error("영문 기반 자동완성 중 오류 발생: prefix={}", prefix, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "영문 기반 자동완성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 한글 자동완성을 위한 검색 (접두사 또는 포함 검색)
     *
     * @param keyword 검색 키워드
     * @param limit 결과 제한 수
     * @return 자동완성 해시태그 응답 DTO 목록
     */
    private List<HashtagResponseDto> koreanAutocomplete(String keyword, int limit) {
        log.debug("한글 기반 자동완성 실행: keyword={}, limit={}", keyword, limit);

        try {
            List<HashtagResponseDto> result = hashtagRepository.findByNameContainingKeywordOrderByCountDesc(keyword, Pageable.ofSize(limit))
                    .stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.debug("한글 기반 자동완성 완료: keyword={}, 결과 수={}", keyword, result.size());
            return result;
        } catch (Exception e) {
            log.error("한글 기반 자동완성 중 오류 발생: keyword={}", keyword, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "한글 기반 자동완성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 초성 자동완성을 위한 메서드
     *
     * @param keyword 초성 키워드
     * @param limit 결과 제한 수
     * @return 자동완성 해시태그 응답 DTO 목록
     */
    private List<HashtagResponseDto> chosungAutocomplete(String keyword, int limit) {
        log.debug("초성 기반 자동완성 실행: keyword={}, limit={}", keyword, limit);

        try {
            // 전체 해시태그 조회
            List<Hashtag> allHashtags = hashtagRepository.findAll();
            log.debug("초성 검색을 위한 전체 해시태그 조회: 총 개수={}", allHashtags.size());

            List<HashtagResponseDto> result = allHashtags.stream()
                    .filter(tag -> {
                        String chosung = extractChosung(tag.getName());
                        return chosung.contains(keyword);
                    })
                    .sorted(Comparator.comparing(Hashtag::getCount).reversed())
                    .limit(limit)
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            log.debug("초성 기반 자동완성 완료: keyword={}, 결과 수={}", keyword, result.size());
            return result;
        } catch (Exception e) {
            log.error("초성 기반 자동완성 중 오류 발생: keyword={}", keyword, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "초성 기반 자동완성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 자동완성 통합 메서드 - 입력된 키워드를 분석하여 적절한 검색 방식 결정
     * (클라이언트에서 isKorean 정보를 전달하지 않는 경우 사용)
     *
     * @param keyword 검색 키워드
     * @param limit 결과 제한 수
     * @return 자동완성 해시태그 응답 DTO 목록
     */
    public List<HashtagResponseDto> smartAutocomplete(String keyword, int limit) {
        log.debug("스마트 자동완성 시작: keyword={}, limit={}", keyword, limit);

        try {
            boolean containsKorean = containsKoreanCharacters(keyword);
            log.debug("키워드 언어 감지 결과: keyword={}, 한글포함={}", keyword, containsKorean);

            List<HashtagResponseDto> result = autocomplete(keyword, limit, containsKorean);
            log.debug("스마트 자동완성 완료: keyword={}, 결과 수={}", keyword, result.size());

            return result;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스마트 자동완성 중 예상치 못한 오류 발생: keyword={}", keyword, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 자동완성 중 오류가 발생했습니다.", e);
        }
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
     * 입력이 한글 초성인지 확인
     */
    private boolean isKoreanChosung(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        boolean hasChosung = false;
        for (char c : text.toCharArray()) {
            // 초성이 아닌 문자가 있으면 false
            if (c >= 'ㄱ' && c <= 'ㅎ') {
                hasChosung = true;
            } else {
                return false;
            }
        }
        return hasChosung;
    }

    /**
     * 한글 문자열에서 초성만 추출
     */
    private String extractChosung(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (char c : text.toCharArray()) {
            // 한글인 경우에만 초성 추출
            if (c >= '가' && c <= '힣') {
                // 한글 유니코드 수식: (한글 - 가) / (21 * 28) + 'ㄱ'
                char chosung = (char) ((c - '가') / (21 * 28) + 'ㄱ');
                result.append(chosung);
            }
        }

        return result.toString();
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