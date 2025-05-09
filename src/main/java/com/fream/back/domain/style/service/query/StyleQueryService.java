package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleQueryService {

    private final StyleRepository styleRepository;
    private final StyleLikeQueryService styleLikeQueryService;
    private final StyleInterestQueryService styleInterestQueryService;
    private final StyleHashtagQueryService styleHashtagQueryService;
    private final ProfileQueryService profileQueryService;

    /**
     * 스타일 ID로 스타일 조회
     *
     * @param styleId 조회할 스타일의 ID
     * @return 조회된 스타일 엔티티
     * @throws StyleException 스타일을 찾을 수 없는 경우
     */
    public Style findStyleById(Long styleId) {
        log.debug("스타일 조회 시작: styleId={}", styleId);
        try {
            Style style = styleRepository.findById(styleId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.STYLE_NOT_FOUND,
                            "해당 스타일을 찾을 수 없습니다: " + styleId));
            log.debug("스타일 조회 성공: styleId={}", styleId);
            return style;
        } catch (StyleException e) {
            log.error("스타일 조회 실패: styleId={}, 원인={}", styleId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("스타일 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_NOT_FOUND, "스타일 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 프로필 ID로 스타일 목록 조회
     *
     * @param profileId 프로필 ID
     * @return 스타일 목록
     */
    public List<Style> findStylesByProfileId(Long profileId) {
        log.debug("프로필별 스타일 목록 조회 시작: profileId={}", profileId);
        try {
            List<Style> styles = styleRepository.findByProfileId(profileId);
            log.debug("프로필별 스타일 목록 조회 완료: profileId={}, 조회된 스타일 수={}", profileId, styles.size());
            return styles;
        } catch (Exception e) {
            log.error("프로필별 스타일 목록 조회 중 오류 발생: profileId={}", profileId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "프로필별 스타일 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 필터링된 스타일 목록 조회 (해시태그 추가) - 최적화 버전
     *
     * @param filterRequestDto 필터 요청 DTO
     * @param pageable 페이징 정보
     * @param email 사용자 이메일
     * @return 필터링된 스타일 목록
     */
    public Page<StyleResponseDto> getFilteredStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable, String email) {
        log.debug("필터링된 스타일 목록 조회 시작: 필터={}, 페이지={}, 이메일={}", filterRequestDto, pageable, email);

        try {
            // 스타일 목록 조회
            Page<StyleResponseDto> styles = styleRepository.filterStyles(filterRequestDto, pageable);
            log.debug("스타일 기본 정보 조회 완료: 조회된 스타일 수={}", styles.getTotalElements());

            // 결과가 없으면 빈 페이지 반환
            if (styles.isEmpty()) {
                log.debug("조회된 스타일이 없습니다.");
                return styles;
            }

            // 스타일 ID 목록 추출
            List<Long> styleIds = styles.getContent().stream()
                    .map(StyleResponseDto::getId)
                    .collect(Collectors.toList());

            // 해시태그 맵 한 번에 조회
            Map<Long, List<com.fream.back.domain.style.dto.HashtagResponseDto>> styleToHashtagsMap =
                    styleHashtagQueryService.getHashtagMapByStyleIds(styleIds);
            log.debug("스타일별 해시태그 정보 조회 완료");

            // 로그인한 사용자인 경우 - 좋아요 및 관심 상태 확인
            if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
                log.debug("로그인 사용자의 좋아요/관심 상태 조회 시작: email={}", email);

                try {
                    // 사용자 프로필 조회 (한 번만 실행)
                    Profile profile = profileQueryService.getProfileByEmail(email);
                    Long profileId = profile.getId();

                    // 좋아요 상태 한 번에 조회
                    Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);

                    // 관심 상태 한 번에 조회
                    Set<Long> interestedStyleIds = styleInterestQueryService.getInterestedStyleIds(profileId, styleIds);
                    log.debug("좋아요/관심 상태 조회 완료: 좋아요 개수={}, 관심 개수={}",
                            likedStyleIds.size(), interestedStyleIds.size());

                    // 각 스타일에 좋아요, 관심 상태, 해시태그 설정
                    for (StyleResponseDto dto : styles.getContent()) {
                        dto.setLiked(likedStyleIds.contains(dto.getId()));
                        dto.setInterested(interestedStyleIds.contains(dto.getId()));
                        dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                    }
                } catch (Exception e) {
                    log.warn("사용자 정보 연동 중 오류 발생 - 기본값으로 진행: email={}, 원인={}", email, e.getMessage());
                    // 프로필 조회 실패 시 각 DTO는 이미 기본값이 설정되어 있으므로
                    // 해시태그만 설정
                    for (StyleResponseDto dto : styles.getContent()) {
                        dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                    }
                }
            } else {
                log.debug("비로그인 사용자용 스타일 정보 구성");
                // 로그인하지 않은 경우 - 해시태그만 설정 (liked, interested는 기본값 false)
                for (StyleResponseDto dto : styles.getContent()) {
                    dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                }
            }

            log.debug("필터링된 스타일 목록 조회 완료: 총 {}개", styles.getTotalElements());
            return styles;

        } catch (Exception e) {
            log.error("필터링된 스타일 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 프로필별 스타일 목록 조회 (해시태그 추가) - 최적화 버전
     *
     * @param profileId 프로필 ID
     * @param pageable 페이징 정보
     * @param email 사용자 이메일
     * @return 프로필별 스타일 목록
     */
    public Page<ProfileStyleResponseDto> getStylesByProfile(Long profileId, Pageable pageable, String email) {
        log.debug("프로필별 스타일 목록 조회 시작: profileId={}, 페이지={}, 이메일={}", profileId, pageable, email);

        try {
            // 스타일 목록 조회
            Page<ProfileStyleResponseDto> styles = styleRepository.getStylesByProfile(profileId, pageable);
            log.debug("프로필별 스타일 기본 정보 조회 완료: 조회된 스타일 수={}", styles.getTotalElements());

            // 결과가 없으면 빈 페이지 반환
            if (styles.isEmpty()) {
                log.debug("조회된 스타일이 없습니다.");
                return styles;
            }

            // 스타일 ID 목록 추출
            List<Long> styleIds = styles.getContent().stream()
                    .map(ProfileStyleResponseDto::getId)
                    .collect(Collectors.toList());

            // 해시태그 맵 한 번에 조회
            Map<Long, List<com.fream.back.domain.style.dto.HashtagResponseDto>> styleToHashtagsMap =
                    styleHashtagQueryService.getHashtagMapByStyleIds(styleIds);
            log.debug("스타일별 해시태그 정보 조회 완료");

            // 로그인한 사용자인 경우 - 좋아요 상태 확인
            if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
                log.debug("로그인 사용자의 좋아요 상태 조회 시작: email={}", email);

                try {
                    // 좋아요 상태 한 번에 조회
                    Set<Long> likedStyleIds = styleLikeQueryService.getLikedStyleIds(email, styleIds);
                    log.debug("좋아요 상태 조회 완료: 좋아요 개수={}", likedStyleIds.size());

                    // 각 스타일에 좋아요 상태와 해시태그 설정
                    for (ProfileStyleResponseDto dto : styles.getContent()) {
                        dto.setLiked(likedStyleIds.contains(dto.getId()));
                        dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                    }
                } catch (Exception e) {
                    log.warn("사용자 정보 연동 중 오류 발생 - 기본값으로 진행: email={}, 원인={}", email, e.getMessage());
                    // 프로필 조회 실패 시 해시태그만 설정
                    for (ProfileStyleResponseDto dto : styles.getContent()) {
                        dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                    }
                }
            } else {
                log.debug("비로그인 사용자용 스타일 정보 구성");
                // 로그인하지 않은 경우 - 해시태그만 설정
                for (ProfileStyleResponseDto dto : styles.getContent()) {
                    dto.setHashtags(styleToHashtagsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                }
            }

            log.debug("프로필별 스타일 목록 조회 완료: 총 {}개", styles.getTotalElements());
            return styles;

        } catch (Exception e) {
            log.error("프로필별 스타일 목록 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "프로필별 스타일 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스타일 상세 정보를 조회합니다. (해시태그 추가)
     * 사용자가 로그인한 경우, 좋아요 상태와 관심 등록 상태를 확인하여 반환합니다.
     *
     * @param styleId 조회할 스타일의 ID
     * @param email 현재 로그인한 사용자의 이메일 (미로그인 시 null 또는 "anonymousUser")
     * @return 스타일 상세 정보를 담은 DTO 객체
     * @throws StyleException 스타일을 찾을 수 없거나 조회 중 오류 발생 시
     */
    public StyleDetailResponseDto getStyleDetail(Long styleId, String email) {
        log.debug("스타일 상세 정보 조회 시작: styleId={}, email={}", styleId, email);

        try {
            // 스타일 상세 정보 조회
            StyleDetailResponseDto detailDto = styleRepository.getStyleDetail(styleId);
            if (detailDto == null) {
                log.error("스타일 상세 정보 조회 실패: styleId={}", styleId);
                throw new StyleException(StyleErrorCode.STYLE_NOT_FOUND,
                        "해당 스타일을 찾을 수 없습니다: " + styleId);
            }

            log.debug("스타일 기본 정보 조회 완료: styleId={}", styleId);

            // 해시태그 조회 및 설정
            detailDto.setHashtags(styleHashtagQueryService.getHashtagsByStyleId(styleId));
            log.debug("스타일 해시태그 정보 조회 완료: styleId={}, 해시태그 수={}",
                    styleId, detailDto.getHashtags().size());

            // 로그인한 사용자이고, anonymousUser가 아닌 경우에만 좋아요 상태와 관심 상태 확인
            if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
                log.debug("로그인 사용자의 좋아요/관심 상태 조회 시작: styleId={}, email={}", styleId, email);

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

                    log.debug("사용자 좋아요/관심 상태 조회 완료: styleId={}, 좋아요={}, 관심={}",
                            styleId, isLiked, isInterested);

                } catch (Exception e) {
                    log.warn("사용자 정보 연동 중 오류 발생 - 기본값으로 진행: styleId={}, email={}, 원인={}",
                            styleId, email, e.getMessage());
                }
            } else {
                log.debug("비로그인 사용자 요청: 기본값으로 진행 (좋아요=false, 관심=false)");
            }

            log.debug("스타일 상세 정보 조회 완료: styleId={}", styleId);
            return detailDto;

        } catch (StyleException e) {
            // 이미 로깅된 StyleException은 그대로 다시 throw
            throw e;
        } catch (Exception e) {
            log.error("스타일 상세 정보 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 상세 정보 조회 중 오류가 발생했습니다.", e);
        }
    }
}