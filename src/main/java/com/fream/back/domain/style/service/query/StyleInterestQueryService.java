package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.style.repository.StyleInterestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleInterestQueryService {

    private final StyleInterestRepository styleInterestRepository;

    // 특정 스타일 ID와 프로필 ID로 관심 등록 여부 확인
    public boolean isStyleInterestedByProfile(Long styleId, Long profileId) {
        return styleInterestRepository.existsByStyleIdAndProfileId(styleId, profileId);
    }

    // 특정 스타일 ID로 연결된 관심 목록 조회
    public List<StyleInterest> findByStyleId(Long styleId) {
        return styleInterestRepository.findByStyleId(styleId);
    }
}


