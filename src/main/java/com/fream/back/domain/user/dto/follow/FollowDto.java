package com.fream.back.domain.user.dto.follow;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowDto {
    private Long profileId;        // 팔로우된(또는 팔로우하는) 프로필의 ID
    private String profileName;    // 프로필 이름
    private String profileImageUrl; // 프로필 이미지 URL
}
