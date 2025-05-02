package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleInterestRepository;
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
        log.debug("스타일 관심 등록 여부 확인 시작: styleId={}, profileId={}", styleId, profileId);

        try {
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            if (profileId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "프로필 ID가 필요합니다.");
            }

            boolean isInterested = styleInterestRepository.existsByStyleIdAndProfileId(styleId, profileId);
            log.debug("스타일 관심 등록 여부 확인 완료: styleId={}, profileId={}, 관심={}",
                    styleId, profileId, isInterested);

            return isInterested;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 관심 등록 여부 확인 중 예상치 못한 오류 발생: styleId={}, profileId={}",
                    styleId, profileId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 관심 등록 여부 확인 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 스타일 ID로 연결된 관심 목록을 조회합니다.
     *
     * @param styleId 스타일 ID
     * @return 관심 등록 목록
     */
    public List<StyleInterest> findByStyleId(Long styleId) {
        log.debug("스타일 ID로 관심 목록 조회 시작: styleId={}", styleId);

        try {
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            List<StyleInterest> interests = styleInterestRepository.findByStyleId(styleId);
            log.debug("스타일 ID로 관심 목록 조회 완료: styleId={}, 관심 등록 수={}",
                    styleId, interests.size());

            return interests;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 ID로 관심 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 관심 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 여러 스타일에 대한 사용자의 관심 상태를 한 번에 확인합니다.
     *
     * @param profileId 프로필 ID
     * @param styleIds 스타일 ID 목록
     * @return 사용자가 관심 등록한 스타일 ID 집합
     */
    public Set<Long> getInterestedStyleIds(Long profileId, List<Long> styleIds) {
        log.debug("여러 스타일에 대한 관심 상태 확인 시작: profileId={}, 스타일 수={}",
                profileId, (styleIds != null ? styleIds.size() : 0));

        try {
            if (profileId == null || styleIds == null || styleIds.isEmpty()) {
                log.debug("프로필 ID 또는 스타일 ID 목록이 비어있어 빈 결과 반환");
                return Collections.emptySet();
            }

            List<StyleInterest> interests = styleInterestRepository.findByProfileIdAndStyleIdIn(
                    profileId, styleIds);

            Set<Long> interestedStyleIds = interests.stream()
                    .map(interest -> interest.getStyle().getId())
                    .collect(Collectors.toSet());

            log.debug("여러 스타일에 대한 관심 상태 확인 완료: profileId={}, 관심 스타일 수={}/{}",
                    profileId, interestedStyleIds.size(), styleIds.size());

            return interestedStyleIds;
        } catch (Exception e) {
            log.error("여러 스타일에 대한 관심 상태 확인 중 예상치 못한 오류 발생: profileId={}, 스타일 수={}",
                    profileId, (styleIds != null ? styleIds.size() : 0), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 관심 상태 확인 중 오류가 발생했습니다.", e);
        }
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
        log.debug("사용자 이메일로 스타일 관심 여부 확인 시작: email={}, styleId={}", email, styleId);

        if (email == null) {
            log.debug("이메일이 null이므로 관심 등록 안함");
            return false;
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            boolean isInterested = styleInterestRepository.existsByStyleIdAndProfileId(
                    styleId, profile.getId());

            log.debug("사용자 이메일로 스타일 관심 여부 확인 완료: email={}, styleId={}, 관심={}",
                    email, styleId, isInterested);

            return isInterested;
        } catch (Exception e) {
            log.warn("사용자 이메일로 스타일 관심 여부 확인 중 오류 발생 - false 반환: email={}, styleId={}",
                    email, styleId, e);
            return false;
        }
    }
}