package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleInterestRepository;
import com.fream.back.domain.style.service.query.StyleQueryService;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StyleInterestCommandService {

    private final StyleInterestRepository styleInterestRepository;
    private final ProfileQueryService profileQueryService;
    private final StyleQueryService styleQueryService;

    /**
     * 스타일 관심 상태 토글
     *
     * @param email 사용자 이메일
     * @param styleId 스타일 ID
     * @throws StyleException 관심 등록/취소 처리 실패 시
     */
    public void toggleStyleInterest(String email, Long styleId) {
        log.debug("스타일 관심 토글 시작: styleId={}, email={}", styleId, email);

        // 입력값 검증
        if (email == null || email.isEmpty()) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "사용자 이메일이 필요합니다.");
        }

        if (styleId == null) {
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            log.debug("프로필 조회 성공: profileId={}, profileName={}",
                    profile.getId(), profile.getProfileName());

            Style style = styleQueryService.findStyleById(styleId);
            log.debug("스타일 조회 성공: styleId={}", styleId);

            StyleInterest existingInterest = styleInterestRepository.findByStyleAndProfile(style, profile)
                    .orElse(null);

            if (existingInterest != null) {
                // 관심 취소
                log.debug("기존 관심 삭제: interestId={}, styleId={}, profileId={}",
                        existingInterest.getId(), styleId, profile.getId());
                styleInterestRepository.delete(existingInterest);
                style.removeInterest(existingInterest); // Style -> Interest 관계 제거
                log.info("스타일 관심 취소 완료: styleId={}, profileId={}", styleId, profile.getId());
            } else {
                // 새로운 관심 추가
                StyleInterest styleInterest = StyleInterest.builder()
                        .style(style)
                        .profile(profile)
                        .build();

                style.addInterest(styleInterest); // Style -> Interest 관계 설정
                StyleInterest savedInterest = styleInterestRepository.save(styleInterest);
                log.info("스타일 관심 추가 완료: interestId={}, styleId={}, profileId={}",
                        savedInterest.getId(), styleId, profile.getId());
            }
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 관심 토글 중 예상치 못한 오류 발생: styleId={}, email={}", styleId, email, e);
            throw new StyleException(StyleErrorCode.INTEREST_OPERATION_FAILED,
                    "스타일 관심 등록 처리 중 오류가 발생했습니다.", e);
        }
    }
}