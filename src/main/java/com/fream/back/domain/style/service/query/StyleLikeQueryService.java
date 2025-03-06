package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleLike;
import com.fream.back.domain.style.repository.StyleLikeRepository;
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
public class StyleLikeQueryService {

    private final StyleLikeRepository styleLikeRepository;
    private final ProfileQueryService profileQueryService;

    /**
     * 특정 스타일에 대한 사용자의 좋아요 여부를 확인합니다.
     *
     * @param email   사용자 이메일
     * @param styleId 스타일 ID
     * @return 좋아요 여부
     */
    public boolean checkUserLiked(String email, Long styleId) {
        if (email == null) {
            return false;
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            return styleLikeRepository.existsByProfileIdAndStyleId(profile.getId(), styleId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 여러 스타일에 대한 사용자의 좋아요 상태를 한 번에 확인합니다.
     *
     * @param email    사용자 이메일
     * @param styleIds 스타일 ID 목록
     * @return 사용자가 좋아요한 스타일 ID 집합
     */
    public Set<Long> getLikedStyleIds(String email, List<Long> styleIds) {
        if (email == null || styleIds.isEmpty()) {
            return Set.of();
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            List<StyleLike> likes = styleLikeRepository.findByProfileIdAndStyleIdIn(profile.getId(), styleIds);

            return likes.stream()
                    .map(like -> like.getStyle().getId())
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }
    // 특정 스타일 ID와 프로필 ID로 좋아요 여부 확인
    public boolean isLikedByProfile(Long styleId, Long profileId) {
        return styleLikeRepository.existsByStyleIdAndProfileId(styleId, profileId);
    }

    // 특정 스타일 ID로 좋아요 목록 조회
    public List<StyleLike> findLikesByStyleId(Long styleId) {
        return styleLikeRepository.findByStyleId(styleId);
    }
}
