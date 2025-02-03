package com.fream.back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateDto {
    private String profileName;       // 프로필 이름
    private String Name;          // 이름
    private String bio;               // 소개글
    private Boolean isPublic;         // 공개 여부
}
