package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.exception.UserErrorCode;
import com.fream.back.domain.user.exception.UserException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    @Transactional
    public void createDefaultProfile(User user) {
        log.info("기본 프로필 생성 시작: userId={}, email={}", user.getId(), user.getEmail());

        try {
            String defaultProfileName = generateRandomProfileName(); // 랜덤 프로필 이름 생성
            String defaultImagePath = "user.jpg"; // 기본 프로필 이미지 경로
            String emailPrefix = user.getEmail().split("@")[0]; // 이메일의 '@' 앞부분을 이름으로 사용

            Profile profile = Profile.builder()
                    .user(user)
                    .profileName(defaultProfileName)
                    .profileImageUrl(defaultImagePath)
                    .bio("") // 기본 소개는 빈 문자열
                    .isPublic(true) // 기본값으로 프로필을 공개
                    .build();

            user.addProfile(profile); // 연관관계 설정

            log.info("기본 프로필 생성 완료: userId={}, profileName={}", user.getId(), defaultProfileName);

        } catch (Exception e) {
            log.error("기본 프로필 생성 실패: userId={}, email={}", user.getId(), user.getEmail(), e);
            throw new ProfileNotFoundException("기본 프로필 생성에 실패했습니다.");
        }
    }

    private String generateRandomProfileName() {
        try {
            SecureRandom random = new SecureRandom();
            StringBuilder sb = new StringBuilder(10);
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            for (int i = 0; i < 10; i++) {
                int index = random.nextInt(characters.length());
                sb.append(characters.charAt(index));
            }

            String profileName = sb.toString();
            log.debug("랜덤 프로필 이름 생성: profileName={}", profileName);
            return profileName;

        } catch (Exception e) {
            log.error("랜덤 프로필 이름 생성 실패", e);
            throw new UserException(UserErrorCode.PROFILE_NOT_FOUND, "프로필 이름 생성에 실패했습니다.", e);
        }
    }
}