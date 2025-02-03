package com.fream.back.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedProfileDto {
    private Long profileId;          // 차단된 프로필 ID
    private String profileName;      // 차단된 프로필 이름
    private String profileImageUrl;  // 차단된 프로필 이미지 경로
}
