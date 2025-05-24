package com.fream.back.domain.user.service.profile;

import com.fream.back.domain.user.dto.BlockedProfileDto;
import com.fream.back.domain.user.dto.ProfileInfoDto;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.repository.ProfileRepository;
import com.fream.back.domain.user.service.BlockProfile.BlockedProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileQueryService {

    private final ProfileRepository profileRepository;
    private final BlockedProfileQueryService blockedProfileQueryService;

    @Transactional(readOnly = true)
    public ProfileInfoDto getProfileInfo(String email) {
        log.info("프로필 정보 조회 시작 - 사용자: {}", email);

        try {
            Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("프로필을 찾을 수 없습니다."));

            List<BlockedProfileDto> blockedProfiles = blockedProfileQueryService.getBlockedProfiles(profile);

            ProfileInfoDto result = new ProfileInfoDto(
                    profile.getId(),
                    profile.getProfileImageUrl(),
                    profile.getProfileName(),
                    profile.getName(),
                    email,
                    profile.getBio(),
                    profile.isPublic(),
                    blockedProfiles
            );

            log.info("프로필 정보 조회 완료 - 사용자: {}, 프로필 ID: {}", email, profile.getId());
            return result;
        } catch (ProfileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로필 정보 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new ProfileNotFoundException("프로필 정보를 조회할 수 없습니다.");
        }
    }

    /**
     * 이메일 기반 프로필 조회
     */
    @Transactional(readOnly = true)
    public Profile getProfileByEmail(String email) {
        log.debug("이메일 기반 프로필 조회 - 사용자: {}", email);

        try {
            Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("프로필을 찾을 수 없습니다."));

            log.debug("이메일 기반 프로필 조회 완료 - 사용자: {}, 프로필 ID: {}", email, profile.getId());
            return profile;
        } catch (ProfileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("이메일 기반 프로필 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new ProfileNotFoundException("프로필을 조회할 수 없습니다.");
        }
    }

    /**
     * 프로필 이미지 파일명 조회
     */
    @Transactional(readOnly = true)
    public String getProfileImageFileName(Long profileId) {
        log.debug("프로필 이미지 파일명 조회 - 프로필 ID: {}", profileId);

        try {
            String fileName = profileRepository.findById(profileId)
                    .map(Profile::getProfileImageUrl)
                    .orElseThrow(() -> new ProfileNotFoundException(profileId));

            log.debug("프로필 이미지 파일명 조회 완료 - 프로필 ID: {}, 파일명: {}", profileId, fileName);
            return fileName;
        } catch (ProfileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로필 이미지 파일명 조회 중 예상치 못한 오류 발생 - 프로필 ID: {}", profileId, e);
            throw new ProfileNotFoundException("프로필 이미지 정보를 조회할 수 없습니다.");
        }
    }
}