package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.repository.MediaUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaUrlQueryService {

    private final MediaUrlRepository mediaUrlRepository;

    public List<MediaUrl> findMediaUrlsByStyleId(Long styleId) {
        return mediaUrlRepository.findByStyleId(styleId);
    }
}
