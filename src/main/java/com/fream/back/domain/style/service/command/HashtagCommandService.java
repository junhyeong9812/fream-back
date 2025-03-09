package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.dto.HashtagCreateRequestDto;
import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.dto.HashtagUpdateRequestDto;
import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.repository.HashtagRepository;
import com.fream.back.domain.style.repository.StyleHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HashtagCommandService {

    private final HashtagRepository hashtagRepository;
    private final StyleHashtagRepository styleHashtagRepository;

    /**
     * 해시태그 생성
     */
    public HashtagResponseDto create(HashtagCreateRequestDto requestDto) {
        // 중복 확인
        if (hashtagRepository.existsByName(requestDto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 해시태그입니다: " + requestDto.getName());
        }

        // 해시태그 생성
        Hashtag hashtag = Hashtag.builder()
                .name(requestDto.getName())
                .count(0L)
                .build();

        // 저장 및 DTO 반환
        Hashtag savedHashtag = hashtagRepository.save(hashtag);
        return convertToDto(savedHashtag);
    }

    /**
     * 해시태그 수정
     */
    public HashtagResponseDto update(Long id, HashtagUpdateRequestDto requestDto) {
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해시태그를 찾을 수 없습니다: " + id));

        // 새 이름이 기존 다른 해시태그와 중복되는지 확인
        if (!hashtag.getName().equals(requestDto.getName()) &&
                hashtagRepository.existsByName(requestDto.getName())) {
            throw new IllegalArgumentException("이미 존재하는 해시태그입니다: " + requestDto.getName());
        }

        // 해시태그 이름 변경을 위한 새 인스턴스 생성 (불변성 유지)
        Hashtag updatedHashtag = Hashtag.builder()
                .id(hashtag.getId())
                .name(requestDto.getName())
                .count(hashtag.getCount())
                .build();

        // 저장 및 DTO 반환
        Hashtag savedHashtag = hashtagRepository.save(updatedHashtag);
        return convertToDto(savedHashtag);
    }

    /**
     * 해시태그 삭제
     */
    public void delete(Long id) {
        // 해시태그 존재 확인
        Hashtag hashtag = hashtagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해시태그를 찾을 수 없습니다: " + id));

        // 스타일과의 연결 확인
        long connectionCount = styleHashtagRepository.countByHashtagId(id);
        if (connectionCount > 0) {
            throw new IllegalStateException(
                    String.format("이 해시태그는 %d개의 스타일에서 사용 중이므로 삭제할 수 없습니다.", connectionCount));
        }

        // 삭제
        hashtagRepository.delete(hashtag);
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