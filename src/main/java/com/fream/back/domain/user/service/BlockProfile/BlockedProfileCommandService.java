package com.fream.back.domain.user.service.BlockProfile;

import com.fream.back.domain.user.entity.BlockedProfile;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.exception.AlreadyBlockedException;
import com.fream.back.domain.user.exception.CannotBlockSelfException;
import com.fream.back.domain.user.exception.NotBlockedException;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.repository.BlockedProfileRepository;
import com.fream.back.domain.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedProfileCommandService {

    private final ProfileRepository profileRepository;
    private final BlockedProfileRepository blockedProfileRepository;

    @Transactional
    public void blockProfile(String email, Long blockedProfileId) {
        log.info("프로필 차단 시작 - 요청자: {}, 차단 대상: {}", email, blockedProfileId);

        try {
            Profile myProfile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("내 프로필을 찾을 수 없습니다."));

            Profile blockedProfile = profileRepository.findById(blockedProfileId)
                    .orElseThrow(() -> new ProfileNotFoundException(blockedProfileId));

            // 자기 자신을 차단하려는 경우 체크
            if (myProfile.getId().equals(blockedProfileId)) {
                log.warn("자기 자신 차단 시도 - 사용자: {}", email);
                throw new CannotBlockSelfException();
            }

            // 이미 차단된 프로필인지 확인
            if (blockedProfileRepository.findByProfileAndBlockedProfile(myProfile, blockedProfile).isPresent()) {
                log.warn("이미 차단된 프로필 - 요청자: {}, 대상: {}", email, blockedProfileId);
                throw new AlreadyBlockedException(blockedProfileId);
            }

            BlockedProfile blockedProfileEntity = BlockedProfile.builder()
                    .profile(myProfile)
                    .blockedProfile(blockedProfile)
                    .build();

            blockedProfileRepository.save(blockedProfileEntity);
            log.info("프로필 차단 완료 - 요청자: {}, 대상: {}", email, blockedProfileId);
        } catch (ProfileNotFoundException | AlreadyBlockedException | CannotBlockSelfException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로필 차단 중 예상치 못한 오류 발생 - 요청자: {}, 대상: {}", email, blockedProfileId, e);
            throw new AlreadyBlockedException("프로필 차단 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public void unblockProfile(String email, Long blockedProfileId) {
        log.info("프로필 차단 해제 시작 - 요청자: {}, 해제 대상: {}", email, blockedProfileId);

        try {
            Profile myProfile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("내 프로필을 찾을 수 없습니다."));

            Profile blockedProfile = profileRepository.findById(blockedProfileId)
                    .orElseThrow(() -> new ProfileNotFoundException(blockedProfileId));

            BlockedProfile blockedProfileEntity = blockedProfileRepository
                    .findByProfileAndBlockedProfile(myProfile, blockedProfile)
                    .orElseThrow(() -> new NotBlockedException(blockedProfileId));

            blockedProfileRepository.delete(blockedProfileEntity);
            log.info("프로필 차단 해제 완료 - 요청자: {}, 대상: {}", email, blockedProfileId);
        } catch (ProfileNotFoundException | NotBlockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로필 차단 해제 중 예상치 못한 오류 발생 - 요청자: {}, 대상: {}", email, blockedProfileId, e);
            throw new NotBlockedException("프로필 차단 해제 처리 중 오류가 발생했습니다.");
        }
    }
}