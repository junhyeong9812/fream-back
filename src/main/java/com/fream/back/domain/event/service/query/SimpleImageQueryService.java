package com.fream.back.domain.event.service.query;

import com.fream.back.domain.event.entity.SimpleImage;
import com.fream.back.domain.event.exception.ImageNotFoundException;
import com.fream.back.domain.event.repository.SimpleImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SimpleImage 조회 전용 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class SimpleImageQueryService {

    private final SimpleImageRepository simpleImageRepository;

    /**
     * 이벤트 ID로 심플 이미지 목록 조회
     */
    public List<SimpleImage> findByEventId(Long eventId) {
        log.debug("이벤트 ID로 심플이미지 목록 조회: eventId={}", eventId);
        List<SimpleImage> images = simpleImageRepository.findByEventId(eventId);
        log.debug("이벤트 관련 심플이미지 조회 완료: eventId={}, 이미지 개수={}", eventId, images.size());
        return images;
    }

    /**
     * 심플 이미지 ID로 조회
     */
    public SimpleImage findById(Long imageId) {
        log.debug("심플이미지 ID로 조회: imageId={}", imageId);
        return simpleImageRepository.findById(imageId)
                .orElseThrow(() -> {
                    log.error("심플이미지를 찾을 수 없음: imageId={}", imageId);
                    return new ImageNotFoundException("이미지를 찾을 수 없습니다. ID: " + imageId);
                });
    }
}