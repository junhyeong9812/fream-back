package com.fream.back.domain.event.service.query;

import com.fream.back.domain.event.entity.SimpleImage;
import com.fream.back.domain.event.repository.SimpleImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SimpleImage 조회 전용 서비스
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SimpleImageQueryService {

    private final SimpleImageRepository simpleImageRepository;

    public List<SimpleImage> findByEventId(Long eventId) {
        return simpleImageRepository.findByEventId(eventId);
    }
}