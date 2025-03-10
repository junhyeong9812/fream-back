package com.fream.back.domain.style.dto.kafka;

import com.fream.back.domain.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleViewEvent {
    private Long styleId;           // 조회된 스타일 ID
    private String email;           // 사용자 이메일 (익명 시 "anonymous")
    private Integer age;            // 나이 (익명 시 0)
    private Gender gender;          // 성별 (익명 시 Gender.OTHER)
    private LocalDateTime viewedAt; // 조회 시점
}