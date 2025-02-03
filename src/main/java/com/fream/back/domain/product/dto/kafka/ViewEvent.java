package com.fream.back.domain.product.dto.kafka;

import com.fream.back.domain.user.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewEvent {
    private Long productColorId;       // 조회된 상품 색상 ID
    private String email;              // 사용자 이메일
    private Integer age;               // 사용자 나이
    private Gender gender;             // 사용자 성별
    private LocalDateTime viewedAt;    // 조회 시각
}

