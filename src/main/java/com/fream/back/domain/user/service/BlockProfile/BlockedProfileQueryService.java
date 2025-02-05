package com.fream.back.domain.user.service.BlockProfile;

import com.fream.back.domain.user.dto.BlockedProfileDto;
import com.fream.back.domain.user.entity.BlockedProfile;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.repository.BlockedProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockedProfileQueryService {

    private final BlockedProfileRepository blockedProfileRepository;

    @Transactional(readOnly = true)
    public List<BlockedProfileDto> getBlockedProfiles(Profile profile) {
        List<BlockedProfile> blockedProfiles = blockedProfileRepository.findAllByProfileWithBlocked(profile);

        return blockedProfiles.stream()
                .map(bp -> new BlockedProfileDto(
                        bp.getBlockedProfile().getId(),
                        bp.getBlockedProfile().getProfileName(),
                        bp.getBlockedProfile().getProfileImageUrl()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BlockedProfileDto> getBlockedProfiles(String email) {
        List<BlockedProfile> blockedProfiles = blockedProfileRepository.findAllByProfileEmailWithBlocked(email);
        return blockedProfiles.stream()
                .map(bp -> new BlockedProfileDto(
                        bp.getBlockedProfile().getId(),
                        bp.getBlockedProfile().getProfileName(),
                        bp.getBlockedProfile().getProfileImageUrl()))
                .toList();
    }
}