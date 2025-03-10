package com.fream.back.domain.style.entity;

import com.fream.back.domain.user.entity.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StyleViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime viewedAt;  // 언제 조회했는지
    private String userEmail;        // 사용자 이메일 (익명 시 "anonymous")
    private Integer userAge;         // 사용자 나이 (익명 시 0)

    @Enumerated(EnumType.STRING)
    private Gender userGender;       // MALE, FEMALE, OTHER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id")
    private Style style;

    public static StyleViewLog create(Style style,
                                      String userEmail,
                                      Integer userAge,
                                      Gender userGender) {
        return StyleViewLog.builder()
                .viewedAt(LocalDateTime.now())
                .userEmail(userEmail)
                .userAge(userAge)
                .userGender(userGender)
                .style(style)
                .build();
    }

    public void addViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}