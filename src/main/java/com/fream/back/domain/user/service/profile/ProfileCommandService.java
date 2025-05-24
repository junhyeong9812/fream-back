package com.fream.back.domain.user.service.profile;

import com.fream.back.domain.user.dto.ProfileUpdateDto;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.InvalidProfileImageException;
import com.fream.back.domain.user.exception.ProfileImageTooLargeException;
import com.fream.back.domain.user.exception.ProfileNotFoundException;
import com.fream.back.domain.user.repository.ProfileRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileCommandService {

    private final ProfileRepository profileRepository;
    private final FileUtils fileUtils;

    @Transactional
    public void createDefaultProfile(User user) {
        log.info("기본 프로필 생성 시작 - 사용자: {}", user.getEmail());

        try {
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

            log.info("기본 프로필 생성 완료 - 사용자: {}, 프로필명: {}", user.getEmail(), defaultProfileName);
        } catch (Exception e) {
            log.error("기본 프로필 생성 중 오류 발생 - 사용자: {}", user.getEmail(), e);
            throw new ProfileNotFoundException("기본 프로필 생성에 실패했습니다.");
        }
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
        log.info("프로필 업데이트 시작 - 사용자: {}", email);

        try {
            Profile profile = profileRepository.findByUserEmailWithFetchJoin(email)
                    .orElseThrow(() -> new ProfileNotFoundException("프로필을 찾을 수 없습니다."));

            // 이미지 변경 처리
            if (profileImage != null && !profileImage.isEmpty()) {
                log.debug("프로필 이미지 업데이트 - 사용자: {}, 파일명: {}",
                        email, profileImage.getOriginalFilename());

                // 프로필 이미지 검증
                validateProfileImage(profileImage);

                // 기존 이미지가 user.jpg가 아니라면 삭제
                if (!"user.jpg".equals(profile.getProfileImageUrl())) {
                    if (profile.getProfileImageUrl() != null) {
                        try {
                            // 기존 파일 삭제
                            fileUtils.deleteFile("profile_images", profile.getProfileImageUrl());
                            log.debug("기존 프로필 이미지 삭제 완료 - {}", profile.getProfileImageUrl());
                        } catch (Exception e) {
                            log.warn("기존 프로필 이미지 삭제 실패 - {}: {}", profile.getProfileImageUrl(), e.getMessage());
                            // 기존 파일 삭제 실패는 업데이트를 중단시키지 않음
                        }
                    }
                }

                // 새 이미지 저장
                String uniqueFilename = fileUtils.saveFile(
                        "profile_images",
                        "profile_" + profile.getId() + "_",
                        profileImage
                );

                // DB에 저장할 값: uniqueFilename (ex: "profile_3_bc123.jpg")
                profile.updateProfile(null, null, null, null, uniqueFilename);
                log.debug("새 프로필 이미지 저장 완료 - {}", uniqueFilename);
            }

            // 프로필 이름, 소개글, 공개 여부 업데이트
            profile.updateProfile(dto.getProfileName(), dto.getName(), dto.getBio(), dto.getIsPublic(), null);

            log.info("프로필 업데이트 완료 - 사용자: {}", email);
        } catch (ProfileNotFoundException | InvalidProfileImageException | ProfileImageTooLargeException e) {
            throw e;
        } catch (Exception e) {
            log.error("프로필 업데이트 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new ProfileNotFoundException("프로필 업데이트에 실패했습니다.");
        }
    }

    /**
     * 프로필 이미지 검증
     */
    private void validateProfileImage(MultipartFile profileImage) {
        // 파일 크기 검증 (5MB 제한)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (profileImage.getSize() > maxSize) {
            log.warn("프로필 이미지 크기 초과 - 파일크기: {} bytes, 제한: {} bytes",
                    profileImage.getSize(), maxSize);
            throw new ProfileImageTooLargeException(profileImage.getSize(), maxSize);
        }

        // 파일 형식 검증
        String contentType = profileImage.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("잘못된 프로필 이미지 형식 - contentType: {}", contentType);
            throw new InvalidProfileImageException(profileImage.getOriginalFilename(), contentType);
        }

        // 허용된 이미지 형식인지 확인
        if (!contentType.equals("image/jpeg") &&
                !contentType.equals("image/png") &&
                !contentType.equals("image/gif")) {
            log.warn("지원하지 않는 이미지 형식 - contentType: {}", contentType);
            throw new InvalidProfileImageException(profileImage.getOriginalFilename(), contentType);
        }
    }
}