package com.fream.back.domain.user.service.BlockProfile;


import com.fream.back.domain.user.entity.BlockedProfile;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.repository.BlockedProfileRepository;
import com.fream.back.domain.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockedProfileCommandService {

    private final ProfileRepository profileRepository;
    private final BlockedProfileRepository blockedProfileRepository;

    @Transactional
    public void blockProfile(String email, Long blockedProfileId) {
        Profile myProfile = profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("내 프로필을 찾을 수 없습니다."));
        Profile blockedProfile = profileRepository.findById(blockedProfileId)
                .orElseThrow(() -> new IllegalArgumentException("차단할 프로필을 찾을 수 없습니다."));

        if (blockedProfileRepository.findByProfileAndBlockedProfile(myProfile, blockedProfile).isPresent()) {
            throw new IllegalArgumentException("이미 차단된 프로필입니다.");
        }

        BlockedProfile blockedProfileEntity = BlockedProfile.builder()
                .profile(myProfile)
                .blockedProfile(blockedProfile)
                .build();

        blockedProfileRepository.save(blockedProfileEntity);
    }

    @Transactional
    public void unblockProfile(String email, Long blockedProfileId) {
        Profile myProfile = profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("내 프로필을 찾을 수 없습니다."));
        Profile blockedProfile = profileRepository.findById(blockedProfileId)
                .orElseThrow(() -> new IllegalArgumentException("차단된 프로필을 찾을 수 없습니다."));

        BlockedProfile blockedProfileEntity = blockedProfileRepository.findByProfileAndBlockedProfile(myProfile, blockedProfile)
                .orElseThrow(() -> new IllegalArgumentException("해당 프로필은 차단 목록에 없습니다."));

        blockedProfileRepository.delete(blockedProfileEntity);
    }
}
