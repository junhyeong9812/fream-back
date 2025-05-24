package com.fream.back.domain.user.service.follow;

import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.user.entity.Follow;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.exception.AlreadyFollowingException;
import com.fream.back.domain.user.exception.CannotFollowSelfException;
import com.fream.back.domain.user.exception.NotFollowingException;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.repository.FollowRepository;
import com.fream.back.domain.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowCommandService {

    private final FollowRepository followRepository;
    private final ProfileRepository profileRepository;
    private final NotificationCommandService notificationCommandService;

    @Transactional
    public void createFollow(String email, Long followingProfileId) {
        log.info("팔로우 생성 시작 - 요청자: {}, 대상: {}", email, followingProfileId);

        try {
            // 현재 사용자의 프로필 가져오기
            Profile followerProfile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("사용자의 프로필이 존재하지 않습니다."));

            // 팔로우 대상 프로필 가져오기
            Profile followingProfile = profileRepository.findById(followingProfileId)
                    .orElseThrow(() -> new ProfileNotFoundException(followingProfileId));

            // 자기 자신을 팔로우하려는 경우 체크
            if (followerProfile.getId().equals(followingProfileId)) {
                log.warn("자기 자신 팔로우 시도 - 사용자: {}", email);
                throw new CannotFollowSelfException();
            }

            // 이미 팔로우 중인지 확인
            if (followRepository.findByFollowerAndFollowing(followerProfile, followingProfile).isPresent()) {
                log.warn("이미 팔로우 중 - 요청자: {}, 대상: {}", email, followingProfileId);
                throw new AlreadyFollowingException(followingProfileId);
            }

            // 팔로우 관계 생성
            Follow follow = Follow.builder()
                    .follower(followerProfile)
                    .following(followingProfile)
                    .build();

            followRepository.save(follow);

            // 팔로우 알림 생성
            try {
                String message = followerProfile.getName() + "님이 회원님을 팔로우 했습니다.";
                notificationCommandService.createNotification(
                        followingProfile.getUser().getId(),
                        NotificationCategory.STYLE,
                        NotificationType.FOLLOW,
                        message
                );
                log.debug("팔로우 알림 생성 완료 - 대상: {}", followingProfileId);
            } catch (Exception e) {
                log.warn("팔로우 알림 생성 실패 - 대상: {}, 오류: {}", followingProfileId, e.getMessage());
                // 알림 실패는 팔로우 자체를 실패시키지 않음
            }

            log.info("팔로우 생성 완료 - 요청자: {}, 대상: {}", email, followingProfileId);
        } catch (ProfileNotFoundException | AlreadyFollowingException | CannotFollowSelfException e) {
            throw e;
        } catch (Exception e) {
            log.error("팔로우 생성 중 예상치 못한 오류 발생 - 요청자: {}, 대상: {}", email, followingProfileId, e);
            throw new AlreadyFollowingException("팔로우 처리 중 오류가 발생했습니다.");
        }
    }

    @Transactional
    public void deleteFollow(String email, Long followingProfileId) {
        log.info("팔로우 삭제 시작 - 요청자: {}, 대상: {}", email, followingProfileId);

        try {
            // 팔로우를 한 사용자 (follower) 프로필 조회
            Profile followerProfile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("사용자의 프로필이 존재하지 않습니다."));

            // 팔로우된 사용자 (following) 프로필 조회
            Profile followingProfile = profileRepository.findById(followingProfileId)
                    .orElseThrow(() -> new ProfileNotFoundException(followingProfileId));

            // 팔로우 관계 조회
            Follow follow = followRepository.findByFollowerAndFollowing(followerProfile, followingProfile)
                    .orElseThrow(() -> new NotFollowingException(followingProfileId));

            // 팔로우 관계 삭제
            followRepository.delete(follow);
            log.info("팔로우 삭제 완료 - 요청자: {}, 대상: {}", email, followingProfileId);
        } catch (ProfileNotFoundException | NotFollowingException e) {
            throw e;
        } catch (Exception e) {
            log.error("팔로우 삭제 중 예상치 못한 오류 발생 - 요청자: {}, 대상: {}", email, followingProfileId, e);
            throw new NotFollowingException("팔로우 삭제 처리 중 오류가 발생했습니다.");
        }
    }
}