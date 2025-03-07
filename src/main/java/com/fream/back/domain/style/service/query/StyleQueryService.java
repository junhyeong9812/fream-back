package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.repository.StyleRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleQueryService {

    private final StyleRepository styleRepository;
    private final StyleLikeQueryService styleLikeQueryService;
    private final StyleInterestQueryService styleInterestQueryService;
    private final ProfileQueryService profileQueryService;


    // 스타일 ID로 조회
    public Style findStyleById(Long styleId) {
        return styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 스타일을 찾을 수 없습니다: " + styleId));
    }

    // 특정 프로필 ID로 스타일 목록 조회
    public List<Style> findStylesByProfileId(Long profileId) {
        return styleRepository.findByProfileId(profileId);
    }



    // 필터링된 스타일 목록 조회
    public Page<StyleResponseDto> getFilteredStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable, String email) {
        Page<StyleResponseDto> styles = styleRepository.filterStyles(filterRequestDto, pageable);

        // 로그인한 사용자이고, anonymousUser가 아닌 경우에만 좋아요 상태 확인
        if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
            List<Long> styleIds = styles.getContent().stream()
                    .map(StyleResponseDto::getId)
                    .collect(Collectors.toList());

            Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

            // 좋아요 상태 설정
            styles.getContent().forEach(style -> {
                style.setLiked(likedStyleIds.contains(style.getId()));
            });
        } else {
            // 로그인하지 않은 경우 또는 anonymousUser인 경우 모두 false로 설정
            styles.getContent().forEach(style -> style.setLiked(false));
        }

        return styles;
    }


    // 프로필별 스타일 목록 조회
    public Page<ProfileStyleResponseDto> getStylesByProfile(Long profileId, Pageable pageable, String email) {
        Page<ProfileStyleResponseDto> styles = styleRepository.getStylesByProfile(profileId, pageable);

        // 로그인한 사용자이고, anonymousUser가 아닌 경우에만 좋아요 상태 확인
        if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
            List<Long> styleIds = styles.getContent().stream()
                    .map(ProfileStyleResponseDto::getId)
                    .collect(Collectors.toList());

            Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

            // 좋아요 상태 설정
            styles.getContent().forEach(style -> {
                style.setLiked(likedStyleIds.contains(style.getId()));
            });
        } else {
            // 로그인하지 않은 경우 또는 anonymousUser인 경우 모두 false로 설정
            styles.getContent().forEach(style -> style.setLiked(false));
        }

        return styles;
    }

    /**
     * 스타일 상세 정보를 조회합니다.
     * 사용자가 로그인한 경우, 좋아요 상태와 관심 등록 상태를 확인하여 반환합니다.
     *
     * @param styleId 조회할 스타일의 ID
     * @param email 현재 로그인한 사용자의 이메일 (미로그인 시 null 또는 "anonymousUser")
     * @return 스타일 상세 정보를 담은 DTO 객체
     */
    public StyleDetailResponseDto getStyleDetail(Long styleId, String email) {
        // 스타일 상세 정보 조회
        StyleDetailResponseDto detailDto = styleRepository.getStyleDetail(styleId);

        // 로그인한 사용자이고, anonymousUser가 아닌 경우에만 좋아요 상태와 관심 상태 확인
        if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
            try {
                // 이메일을 통해 프로필 정보 조회 - 한 번의 조회로 재사용
                Profile profile = profileQueryService.getProfileByEmail(email);
                Long profileId = profile.getId();

                // 좋아요 상태 확인 및 설정 - 프로필 ID 사용
                boolean isLiked = styleLikeQueryService.isLikedByProfile(styleId, profileId);
                detailDto.setLiked(isLiked);

                // 관심 상태 확인 및 설정 - 프로필 ID 사용
                boolean isInterested = styleInterestQueryService.isStyleInterestedByProfile(styleId, profileId);
                detailDto.setInterested(isInterested);
            } catch (Exception e) {
                // 프로필 조회 또는 상태 확인 중 예외 발생 시, 기본값 유지
                detailDto.setLiked(false);
                detailDto.setInterested(false);
            }
        } else {
            // 로그인하지 않은 경우 또는 anonymousUser인 경우 모두 false로 설정
            detailDto.setLiked(false);
            detailDto.setInterested(false);
        }

        return detailDto;
    }
}

