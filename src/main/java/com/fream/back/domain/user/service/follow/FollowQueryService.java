package com.fream.back.domain.user.service.follow;

import com.fream.back.domain.user.dto.follow.FollowDto;
import com.fream.back.domain.user.entity.Follow;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.repository.FollowRepository;
import com.fream.back.domain.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowQueryService {

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;

    public Page<FollowDto> getFollowers(String email, Pageable pageable) {
        log.info("팔로워 목록 조회 시작 - 사용자: {}", email);

        try {
            Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("사용자의 프로필이 존재하지 않습니다."));

            Page<Follow> followers = followRepository.findFollowersByProfileId(profile.getId(), pageable);

            Page<FollowDto> result = followers.map(follow -> new FollowDto(
                    follow.getFollower().getId(),
                    follow.getFollower().getProfileName(),
                    follow.getFollower().getProfileImageUrl()
            ));

            log.info("팔로워 목록 조회 완료 - 사용자: {}, 팔로워 수: {}", email, result.getTotalElements());
            return result;
        } catch (ProfileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("팔로워 목록 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new ProfileNotFoundException("팔로워 목록을 조회할 수 없습니다.");
        }
    }

    public Page<FollowDto> getFollowings(String email, Pageable pageable) {
        log.info("팔로잉 목록 조회 시작 - 사용자: {}", email);

        try {
            Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("사용자의 프로필이 존재하지 않습니다."));

            Page<Follow> followings = followRepository.findFollowingsByProfileId(profile.getId(), pageable);

            Page<FollowDto> result = followings.map(follow -> new FollowDto(
                    follow.getFollowing().getId(),
                    follow.getFollowing().getProfileName(),
                    follow.getFollowing().getProfileImageUrl()
            ));

            log.info("팔로잉 목록 조회 완료 - 사용자: {}, 팔로잉 수: {}", email, result.getTotalElements());
            return result;
        } catch (ProfileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("팔로잉 목록 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new ProfileNotFoundException("팔로잉 목록을 조회할 수 없습니다.");
        }
    }
}