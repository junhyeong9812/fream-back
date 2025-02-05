package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ProfileService {

    @Transactional
    public void createDefaultProfile(User user) {
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
    }

    private String generateRandomProfileName() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }

        return sb.toString();
    }
}
