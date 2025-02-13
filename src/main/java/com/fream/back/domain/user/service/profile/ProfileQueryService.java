package com.fream.back.domain.user.service.profile;

import com.fream.back.domain.user.dto.BlockedProfileDto;
import com.fream.back.domain.user.dto.ProfileInfoDto;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.repository.ProfileRepository;
import com.fream.back.domain.user.service.BlockProfile.BlockedProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final BlockedProfileQueryService blockedProfileQueryService;

    @Transactional(readOnly = true)
    public ProfileInfoDto getProfileInfo(String email) {
        Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));

        List<BlockedProfileDto> blockedProfiles = blockedProfileQueryService.getBlockedProfiles(profile);


        return new ProfileInfoDto(
                profile.getProfileImageUrl(),
                profile.getProfileName(),
                profile.getName(),
                email,
                profile.getBio(),
                profile.isPublic(),
                blockedProfiles
        );
    }
    //이메일 기반 조회
    @Transactional(readOnly = true)
    public Profile getProfileByEmail(String email) {
        return profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));
    }

    // 프로필 이미지 파일명 조회
    public String getProfileImageFileName(Long profileId) {
        return profileRepository.findById(profileId)
                .map(Profile::getProfileImageUrl)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));
    }
}
