package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.dto.HashtagCreateRequestDto;
import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.dto.HashtagUpdateRequestDto;
import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.HashtagRepository;
import com.fream.back.domain.style.repository.StyleHashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HashtagCommandService {

    private final HashtagRepository hashtagRepository;
    private final StyleHashtagRepository styleHashtagRepository;

    /**
     * 해시태그 생성
     *
     * @param requestDto 해시태그 생성 요청 DTO
     * @return 생성된 해시태그 응답 DTO
     * @throws StyleException 해시태그 생성 실패 시
     */
    public HashtagResponseDto create(HashtagCreateRequestDto requestDto) {
        log.debug("해시태그 생성 시작: name={}", requestDto.getName());

        try {
            // 입력값 검증
            if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
                throw new StyleException(StyleErrorCode.HASHTAG_ALREADY_EXISTS, "해시태그 이름이 필요합니다.");
            }

            // 이름 정제 (앞뒤 공백 제거 및 소문자 변환)
            String cleanName = requestDto.getName().trim();

            // 중복 확인
            if (hashtagRepository.existsByName(cleanName)) {
                log.warn("해시태그 중복 발생: name={}", cleanName);
                throw new StyleException(StyleErrorCode.HASHTAG_ALREADY_EXISTS,
                        "이미 존재하는 해시태그입니다: " + cleanName);
            }

            // 해시태그 생성
            Hashtag hashtag = Hashtag.builder()
                    .name(cleanName)
                    .count(0L)
                    .build();

            // 저장 및 DTO 반환
            Hashtag savedHashtag = hashtagRepository.save(hashtag);
            log.info("해시태그 생성 완료: hashtagId={}, name={}", savedHashtag.getId(), savedHashtag.getName());
            return convertToDto(savedHashtag);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 생성 중 예상치 못한 오류 발생: name={}", requestDto.getName(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 해시태그 수정
     *
     * @param id 수정할 해시태그 ID
     * @param requestDto 해시태그 수정 요청 DTO
     * @return 수정된 해시태그 응답 DTO
     * @throws StyleException 해시태그 수정 실패 시
     */
    public HashtagResponseDto update(Long id, HashtagUpdateRequestDto requestDto) {
        log.debug("해시태그 수정 시작: hashtagId={}, newName={}", id, requestDto.getName());

        try {
            // 입력값 검증
            if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "해시태그 이름이 필요합니다.");
            }

            // 이름 정제 (앞뒤 공백 제거)
            String cleanName = requestDto.getName().trim();

            Hashtag hashtag = hashtagRepository.findById(id)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND,
                            "해시태그를 찾을 수 없습니다: " + id));
            log.debug("해시태그 조회 성공: hashtagId={}, 현재 이름={}", id, hashtag.getName());

            // 새 이름이 기존 다른 해시태그와 중복되는지 확인
            if (!hashtag.getName().equals(cleanName) && hashtagRepository.existsByName(cleanName)) {
                log.warn("해시태그 이름 중복: hashtagId={}, 중복 이름={}", id, cleanName);
                throw new StyleException(StyleErrorCode.HASHTAG_ALREADY_EXISTS,
                        "이미 존재하는 해시태그입니다: " + cleanName);
            }

            // 해시태그 이름 변경을 위한 새 인스턴스 생성 (불변성 유지)
            Hashtag updatedHashtag = Hashtag.builder()
                    .id(hashtag.getId())
                    .name(cleanName)
                    .count(hashtag.getCount())
                    .build();

            // 저장 및 DTO 반환
            Hashtag savedHashtag = hashtagRepository.save(updatedHashtag);
            log.info("해시태그 수정 완료: hashtagId={}, 이전 이름={}, 새 이름={}",
                    id, hashtag.getName(), savedHashtag.getName());

            return convertToDto(savedHashtag);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 수정 중 예상치 못한 오류 발생: hashtagId={}, newName={}",
                    id, requestDto.getName(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 해시태그 삭제
     *
     * @param id 삭제할 해시태그 ID
     * @throws StyleException 해시태그 삭제 실패 시
     */
    public void delete(Long id) {
        log.debug("해시태그 삭제 시작: hashtagId={}", id);

        try {
            // 해시태그 존재 확인
            Hashtag hashtag = hashtagRepository.findById(id)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND,
                            "해시태그를 찾을 수 없습니다: " + id));
            log.debug("해시태그 조회 성공: hashtagId={}, name={}", id, hashtag.getName());

            // 스타일과의 연결 확인
            long connectionCount = styleHashtagRepository.countByHashtagId(id);
            if (connectionCount > 0) {
                log.warn("사용 중인 해시태그 삭제 시도: hashtagId={}, 연결된 스타일 수={}", id, connectionCount);
                throw new StyleException(StyleErrorCode.HASHTAG_IN_USE,
                        String.format("이 해시태그는 %d개의 스타일에서 사용 중이므로 삭제할 수 없습니다.", connectionCount));
            }

            // 삭제
            hashtagRepository.delete(hashtag);
            log.info("해시태그 삭제 완료: hashtagId={}, name={}", id, hashtag.getName());

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 삭제 중 예상치 못한 오류 발생: hashtagId={}", id, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 엔티티를 DTO로 변환
     *
     * @param hashtag 해시태그 엔티티
     * @return 해시태그 응답 DTO
     */
    private HashtagResponseDto convertToDto(Hashtag hashtag) {
        return HashtagResponseDto.builder()
                .id(hashtag.getId())
                .name(hashtag.getName())
                .count(hashtag.getCount())
                .build();
    }
}