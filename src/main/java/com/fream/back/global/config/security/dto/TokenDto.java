package com.fream.back.global.config.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AccessToken, RefreshToken을 묶어서 반환할 때 사용하는 DTO
 */
@Getter
@AllArgsConstructor
public class TokenDto {
    private String accessToken;
    private String refreshToken;
}
