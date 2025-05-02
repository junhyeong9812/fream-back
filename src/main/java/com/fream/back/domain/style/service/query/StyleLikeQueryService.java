package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleLike;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleLikeRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
        log.debug("사용자 이메일로 좋아요 여부 확인 시작: email={}, styleId={}", email, styleId);

        if (email == null) {
            log.debug("이메일이 null이므로 좋아요 안함");
            return false;
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            boolean isLiked = styleLikeRepository.existsByProfileIdAndStyleId(profile.getId(), styleId);

            log.debug("사용자 이메일로 좋아요 여부 확인 완료: email={}, styleId={}, 좋아요={}",
                    email, styleId, isLiked);

            return isLiked;
        } catch (Exception e) {
            log.warn("사용자 이메일로 좋아요 여부 확인 중 오류 발생 - false 반환: email={}, styleId={}",
                    email, styleId, e);
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
        log.debug("여러 스타일에 대한 좋아요 상태 확인 시작: email={}, 스타일 수={}",
                email, (styleIds != null ? styleIds.size() : 0));

        try {
            if (email == null || styleIds == null || styleIds.isEmpty()) {
                log.debug("이메일 또는 스타일 ID 목록이 비어있어 빈 결과 반환");
                return Collections.emptySet();
            }

            try {
                Profile profile = profileQueryService.getProfileByEmail(email);
                List<StyleLike> likes = styleLikeRepository.findByProfileIdAndStyleIdIn(
                        profile.getId(), styleIds);

                Set<Long> likedStyleIds = likes.stream()
                        .map(like -> like.getStyle().getId())
                        .collect(Collectors.toSet());

                log.debug("여러 스타일에 대한 좋아요 상태 확인 완료: email={}, 좋아요 스타일 수={}/{}",
                        email, likedStyleIds.size(), styleIds.size());

                return likedStyleIds;
            } catch (Exception e) {
                log.warn("프로필 조회 중 오류 발생 - 빈 결과 반환: email={}, 원인={}", email, e.getMessage());
                return Collections.emptySet();
            }
        } catch (Exception e) {
            log.error("여러 스타일에 대한 좋아요 상태 확인 중 예상치 못한 오류 발생: email={}", email, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 좋아요 상태 확인 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 스타일 ID와 프로필 ID로 좋아요 여부 확인
     *
     * @param styleId 스타일 ID
     * @param profileId 프로필 ID
     * @return 좋아요 여부
     */
    public boolean isLikedByProfile(Long styleId, Long profileId) {
        log.debug("스타일 좋아요 여부 확인 시작: styleId={}, profileId={}", styleId, profileId);

        try {
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            if (profileId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "프로필 ID가 필요합니다.");
            }

            boolean isLiked = styleLikeRepository.existsByStyleIdAndProfileId(styleId, profileId);
            log.debug("스타일 좋아요 여부 확인 완료: styleId={}, profileId={}, 좋아요={}",
                    styleId, profileId, isLiked);

            return isLiked;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 좋아요 여부 확인 중 예상치 못한 오류 발생: styleId={}, profileId={}",
                    styleId, profileId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 좋아요 여부 확인 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 스타일 ID로 좋아요 목록 조회
     *
     * @param styleId 스타일 ID
     * @return 좋아요 엔티티 목록
     */
    public List<StyleLike> findLikesByStyleId(Long styleId) {
        log.debug("스타일 ID로 좋아요 목록 조회 시작: styleId={}", styleId);

        try {
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            List<StyleLike> likes = styleLikeRepository.findByStyleId(styleId);
            log.debug("스타일 ID로 좋아요 목록 조회 완료: styleId={}, 좋아요 수={}",
                    styleId, likes.size());

            return likes;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 ID로 좋아요 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 좋아요 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}