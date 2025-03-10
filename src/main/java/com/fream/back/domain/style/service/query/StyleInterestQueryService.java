package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.style.repository.StyleInterestRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 여러 스타일에 대한 사용자의 관심 상태를 한 번에 확인합니다.
     *
     * @param profileId 프로필 ID
     * @param styleIds 스타일 ID 목록
     * @return 사용자가 관심 등록한 스타일 ID 집합
     */
    public Set<Long> getInterestedStyleIds(Long profileId, List<Long> styleIds) {
        if (profileId == null || styleIds == null || styleIds.isEmpty()) {
            return Set.of();
        }

        List<StyleInterest> interests = styleInterestRepository.findByProfileIdAndStyleIdIn(profileId, styleIds);

        return interests.stream()
                .map(interest -> interest.getStyle().getId())
                .collect(Collectors.toSet());
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