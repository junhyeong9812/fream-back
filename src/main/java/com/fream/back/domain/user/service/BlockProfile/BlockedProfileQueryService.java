package com.fream.back.domain.user.service.BlockProfile;

import com.fream.back.domain.user.dto.BlockedProfileDto;
import com.fream.back.domain.user.entity.BlockedProfile;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.repository.BlockedProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedProfileQueryService {

    private final BlockedProfileRepository blockedProfileRepository;

    @Transactional(readOnly = true)
    public List<BlockedProfileDto> getBlockedProfiles(Profile profile) {
        log.debug("차단된 프로필 목록 조회 - 프로필 ID: {}", profile.getId());

        try {
            List<BlockedProfile> blockedProfiles = blockedProfileRepository.findAllByProfileWithBlocked(profile);

            List<BlockedProfileDto> result = blockedProfiles.stream()
                    .map(bp -> new BlockedProfileDto(
                            bp.getBlockedProfile().getId(),
                            bp.getBlockedProfile().getProfileName(),
                            bp.getBlockedProfile().getProfileImageUrl()))
                    .toList();

            log.debug("차단된 프로필 목록 조회 완료 - 프로필 ID: {}, 차단된 수: {}",
                    profile.getId(), result.size());
            return result;
        } catch (Exception e) {
            log.error("차단된 프로필 목록 조회 중 오류 발생 - 프로필 ID: {}", profile.getId(), e);
            throw new ProfileNotFoundException("차단된 프로필 목록을 조회할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<BlockedProfileDto> getBlockedProfiles(String email) {
        log.info("차단된 프로필 목록 조회 - 사용자: {}", email);

        try {
            List<BlockedProfile> blockedProfiles = blockedProfileRepository.findAllByProfileEmailWithBlocked(email);

            List<BlockedProfileDto> result = blockedProfiles.stream()
                    .map(bp -> new BlockedProfileDto(
                            bp.getBlockedProfile().getId(),
                            bp.getBlockedProfile().getProfileName(),
                            bp.getBlockedProfile().getProfileImageUrl()))
                    .toList();

            log.info("차단된 프로필 목록 조회 완료 - 사용자: {}, 차단된 수: {}", email, result.size());
            return result;
        } catch (Exception e) {
            log.error("차단된 프로필 목록 조회 중 오류 발생 - 사용자: {}", email, e);
            throw new ProfileNotFoundException("차단된 프로필 목록을 조회할 수 없습니다.");
        }
    }
}