package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.repository.StyleRepository;
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

        // 로그인한 사용자인 경우 좋아요 상태 확인
        if (email != null && !email.isEmpty()) {
            List<Long> styleIds = styles.getContent().stream()
                    .map(StyleResponseDto::getId)
                    .collect(Collectors.toList());

            Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

            // 좋아요 상태 설정
            styles.getContent().forEach(style -> {
                style.setLiked(likedStyleIds.contains(style.getId()));
            });
        } else {
            // 로그인하지 않은 경우 모두 false로 설정
            styles.getContent().forEach(style -> style.setLiked(false));
        }

        return styles;
    }

    // 프로필별 스타일 목록 조회
    public Page<ProfileStyleResponseDto> getStylesByProfile(Long profileId, Pageable pageable, String email) {
        Page<ProfileStyleResponseDto> styles = styleRepository.getStylesByProfile(profileId, pageable);

        // 로그인한 사용자인 경우 좋아요 상태 확인
        if (email != null && !email.isEmpty()) {
            List<Long> styleIds = styles.getContent().stream()
                    .map(ProfileStyleResponseDto::getId)
                    .collect(Collectors.toList());

            Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

            // 좋아요 상태 설정
            styles.getContent().forEach(style -> {
                style.setLiked(likedStyleIds.contains(style.getId()));
            });
        } else {
            // 로그인하지 않은 경우 모두 false로 설정
            styles.getContent().forEach(style -> style.setLiked(false));
        }

        return styles;
    }

    // 스타일 상세 정보 조회
    public StyleDetailResponseDto getStyleDetail(Long styleId, String email) {
        StyleDetailResponseDto detailDto = styleRepository.getStyleDetail(styleId);

        // 로그인한 사용자인 경우 좋아요 상태 확인
        if (email != null && !email.isEmpty()) {
            boolean isLiked = styleLikeQueryService.checkUserLiked(email, styleId);
            detailDto.setLiked(isLiked);
        } else {
            detailDto.setLiked(false);
        }

        return detailDto;
    }
}

