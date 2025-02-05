package com.fream.back.domain.user.service.follow;


import com.fream.back.domain.user.dto.follow.FollowDto;
import com.fream.back.domain.user.entity.Follow;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.repository.FollowRepository;
import com.fream.back.domain.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowQueryService {

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;

    public Page<FollowDto> getFollowers(String email, Pageable pageable) {
        Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자의 프로필이 존재하지 않습니다."));

        Page<Follow> followers = followRepository.findFollowersByProfileId(profile.getId(), pageable);
        return followers.map(follow -> new FollowDto(
                follow.getFollower().getId(),
                follow.getFollower().getProfileName(),
                follow.getFollower().getProfileImageUrl()
        ));
    }

    public Page<FollowDto> getFollowings(String email, Pageable pageable) {
        Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자의 프로필이 존재하지 않습니다."));

        Page<Follow> followings = followRepository.findFollowingsByProfileId(profile.getId(), pageable);
        return followings.map(follow -> new FollowDto(
                follow.getFollowing().getId(),
                follow.getFollowing().getProfileName(),
                follow.getFollowing().getProfileImageUrl()
        ));
    }
}