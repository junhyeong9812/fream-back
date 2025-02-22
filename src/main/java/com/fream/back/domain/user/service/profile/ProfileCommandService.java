package com.fream.back.domain.user.service.profile;

import com.fream.back.domain.user.dto.ProfileUpdateDto;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.ProfileRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final FileUtils fileUtils;

    @Transactional
    public void createDefaultProfile(User user) {
        String defaultProfileName = generateRandomProfileName(); // 랜덤 프로필 이름 생성
        String defaultImagePath = "user.jpg"; // 기본 프로필 이미지 경로
        String emailPrefix = user.getEmail().split("@")[0]; // 이메일의 '@' 앞부분을 이름으로 사용

        Profile profile = Profile.builder()
                .user(user)
                .profileName(defaultProfileName)
                .profileImageUrl(defaultImagePath)
                .Name(emailPrefix)
                .bio("") // 기본 소개는 빈 문자열
                .isPublic(true) // 기본값으로 프로필을 공개
                .build();

        user.addProfile(profile); // 연관관계 설정
        profileRepository.save(profile); // 프로필 저장

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

    @Transactional
    public void updateProfile(String email, ProfileUpdateDto dto, MultipartFile profileImage) {
        Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                .orElseThrow(() -> new IllegalArgumentException("프로필을 찾을 수 없습니다."));

        // 이미지 변경
        if (profileImage != null && !profileImage.isEmpty()) {
            // 기존 이미지가 user.jpg가 아니라면 삭제
            if (!"user.jpg".equals(profile.getProfileImageUrl())) {
                if (profile.getProfileImageUrl() != null) {
                    // 기존 파일 삭제
                    // directory = "profile_images", fileName = profileImageUrl
                    fileUtils.deleteFile("profile_images", profile.getProfileImageUrl());
                }
            }

            // 새 이미지 저장
            // directory = "profile_images", prefix = "profile_{profile.id}_"
            String uniqueFilename = fileUtils.saveFile(
                    "profile_images",
                    "profile_" + profile.getId() + "_",
                    profileImage
            );

            // DB에 저장할 값: uniqueFilename (ex: "profile_3_bc123.jpg")
            profile.updateProfile(null, null, null, null, uniqueFilename);
        }

        // 프로필 이름, 소개글, 공개 여부 업데이트
        profile.updateProfile(dto.getProfileName(), dto.getName(), dto.getBio(), dto.getIsPublic(), null);
    }
}
