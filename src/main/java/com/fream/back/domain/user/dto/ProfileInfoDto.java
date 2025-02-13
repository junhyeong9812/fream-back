package com.fream.back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileInfoDto {
    private String profileImage;      // 프로필 이미지 경로
    private String profileName;       // 프로필 이름
    private String realName;          // 이름
    private String email;             // 이메일
    private String bio;               // 소개글
    private Boolean isPublic;         // 공개 여부
    private List<BlockedProfileDto> blockedProfiles; // 차단된 프로필 목록
}
