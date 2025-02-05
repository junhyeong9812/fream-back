package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleInterest;
import com.fream.back.domain.style.repository.StyleInterestRepository;
import com.fream.back.domain.style.service.query.StyleQueryService;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StyleInterestCommandService {

    private final StyleInterestRepository styleInterestRepository;
    private final ProfileQueryService profileQueryService;
    private final StyleQueryService styleQueryService;

    // 스타일 관심 상태 토글
    public void toggleStyleInterest(String email, Long styleId) {
        Profile profile = profileQueryService.getProfileByEmail(email);
        Style style = styleQueryService.findStyleById(styleId);

        StyleInterest existingInterest = styleInterestRepository.findByStyleAndProfile(style, profile).orElse(null);

        if (existingInterest != null) {
            styleInterestRepository.delete(existingInterest);
            style.removeInterest(existingInterest); // Style -> Interest 관계 제거
        } else {
            StyleInterest styleInterest = StyleInterest.builder()
                    .style(style)
                    .profile(profile)
                    .build();

            style.addInterest(styleInterest); // Style -> Interest 관계 설정
            styleInterestRepository.save(styleInterest);
        }
    }
}
