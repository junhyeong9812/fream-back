package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.style.repository.StyleInterestRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleInterestQueryService {

    private final StyleInterestRepository styleInterestRepository;
    private final ProfileQueryService profileQueryService;

    /**
     * 특정 스타일 ID와 프로필 ID로 관심 등록 여부를 확인합니다.
     *
     * @param styleId 스타일 ID
     * @param profileId 프로필 ID
     * @return 관심 등록 여부
     */
    public boolean isStyleInterestedByProfile(Long styleId, Long profileId) {
        return styleInterestRepository.existsByStyleIdAndProfileId(styleId, profileId);
    }

    /**
     * 특정 스타일 ID로 연결된 관심 목록을 조회합니다.
     *
     * @param styleId 스타일 ID
     * @return 관심 등록 목록
     */
    public List<StyleInterest> findByStyleId(Long styleId) {
        return styleInterestRepository.findByStyleId(styleId);
    }

    /**
     * 이메일로 사용자를 식별하여 특정 스타일에 대한 관심 등록 여부를 확인합니다.
     * 주로 StyleQueryService에서 직접 프로필을 조회하는 경우에는 사용하지 않습니다.
     *
     * @param email 사용자 이메일
     * @param styleId 스타일 ID
     * @return 관심 등록 여부
     */
    public boolean checkUserInterested(String email, Long styleId) {
        if (email == null) {
            return false;
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            return styleInterestRepository.existsByStyleIdAndProfileId(styleId, profile.getId());
        } catch (Exception e) {
            return false;
        }
    }
}