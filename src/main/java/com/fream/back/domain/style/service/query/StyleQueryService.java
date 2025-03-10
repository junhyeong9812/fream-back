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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleQueryService {

    private final StyleRepository styleRepository;
    private final StyleLikeQueryService styleLikeQueryService;
    private final StyleInterestQueryService styleInterestQueryService;
    private final StyleHashtagQueryService styleHashtagQueryService;
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

    // 필터링된 스타일 목록 조회 (해시태그 추가) - 최적화 버전
    public Page<StyleResponseDto> getFilteredStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable, String email) {
        // 스타일 목록 조회
        Page<StyleResponseDto> styles = styleRepository.filterStyles(filterRequestDto, pageable);

        // 결과가 없으면 빈 페이지 반환
        if (styles.isEmpty()) {
            return styles;
        }

        // 스타일 ID 목록 추출
        List<Long> styleIds = styles.getContent().stream()
                .map(StyleResponseDto::getId)
                .collect(Collectors.toList());

        // 해시태그 맵 한 번에 조회
        Map<Long, List<com.fream.back.domain.style.dto.HashtagResponseDto>> styleToHashtagsMap =
                styleHashtagQueryService.getHashtagMapByStyleIds(styleIds);

        // 로그인한 사용자인 경우 - 좋아요 및 관심 상태 확인
        if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
            try {
                // 사용자 프로필 조회 (한 번만 실행)
                Profile profile = profileQueryService.getProfileByEmail(email);
                Long profileId = profile.getId();

                // 좋아요 상태 한 번에 조회
                Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

                // 관심 상태 한 번에 조회
                Set<Long> interestedStyleIds = styleInterestQueryService.getInterestedStyleIds(profileId, styleIds);

                // 각 스타일에 좋아요, 관심 상태, 해시태그 설정
                for (StyleResponseDto dto : styles.getContent()) {
                    dto.setLiked(likedStyleIds.contains(dto.getId()));
                    dto.setInterested(interestedStyleIds.contains(dto.getId()));
                    dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                }
            } catch (Exception e) {
                // 프로필 조회 실패 시 각 DTO는 이미 기본값이 설정되어 있으므로
                // 해시태그만 설정
                for (StyleResponseDto dto : styles.getContent()) {
                    dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                }
            }
        } else {
            // 로그인하지 않은 경우 - 해시태그만 설정 (liked, interested는 기본값 false)
            for (StyleResponseDto dto : styles.getContent()) {
                dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
            }
        }

        return styles;
    }

    // 프로필별 스타일 목록 조회 (해시태그 추가) - 최적화 버전
    public Page<ProfileStyleResponseDto> getStylesByProfile(Long profileId, Pageable pageable, String email) {
        // 스타일 목록 조회
        Page<ProfileStyleResponseDto> styles = styleRepository.getStylesByProfile(profileId, pageable);

        // 결과가 없으면 빈 페이지 반환
        if (styles.isEmpty()) {
            return styles;
        }

        // 스타일 ID 목록 추출
        List<Long> styleIds = styles.getContent().stream()
                .map(ProfileStyleResponseDto::getId)
                .collect(Collectors.toList());

        // 해시태그 맵 한 번에 조회
        Map<Long, List<com.fream.back.domain.style.dto.HashtagResponseDto>> styleToHashtagsMap =
                styleHashtagQueryService.getHashtagMapByStyleIds(styleIds);

        // 로그인한 사용자인 경우 - 좋아요 상태 확인
        if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
            try {
                // 좋아요 상태 한 번에 조회
                Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

                // 각 스타일에 좋아요 상태와 해시태그 설정
                for (ProfileStyleResponseDto dto : styles.getContent()) {
                    dto.setLiked(likedStyleIds.contains(dto.getId()));
                    dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                }
            } catch (Exception e) {
                // 프로필 조회 실패 시 해시태그만 설정
                for (ProfileStyleResponseDto dto : styles.getContent()) {
                    dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                }
            }
        } else {
            // 로그인하지 않은 경우 - 해시태그만 설정
            for (ProfileStyleResponseDto dto : styles.getContent()) {
                dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
            }
        }

        return styles;
    }

    /**
     * 스타일 상세 정보를 조회합니다. (해시태그 추가)
     * 사용자가 로그인한 경우, 좋아요 상태와 관심 등록 상태를 확인하여 반환합니다.
     *
     * @param styleId 조회할 스타일의 ID
     * @param email 현재 로그인한 사용자의 이메일 (미로그인 시 null 또는 "anonymousUser")
     * @return 스타일 상세 정보를 담은 DTO 객체
     */
    public StyleDetailResponseDto getStyleDetail(Long styleId, String email) {
        // 스타일 상세 정보 조회
        StyleDetailResponseDto detailDto = styleRepository.getStyleDetail(styleId);

        // 해시태그 조회 및 설정
        detailDto.setHashtags(styleHashtagQueryService.getHashtagsByStyleId(styleId));

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

            }
        }


        return detailDto;
    }
}