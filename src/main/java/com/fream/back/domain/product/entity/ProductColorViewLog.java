package com.fream.back.domain.product.entity;

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
public class ProductColorViewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime viewedAt;  // 언제 클릭(뷰)했는지
    private String userEmail;       // 사용자 이메일 (익명 시 null)
    private Integer userAge;        // 사용자 나이 (익명 시 0)

    @Enumerated(EnumType.STRING)
    private Gender userGender;      // MALE, FEMALE, OTHER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_color_id")
    private ProductColor productColor;

    public static ProductColorViewLog create(ProductColor productColor,
                                             String userEmail,
                                             Integer userAge,
                                             Gender userGender) {
        return ProductColorViewLog.builder()
                .viewedAt(LocalDateTime.now())
                .userEmail(userEmail)
                .userAge(userAge)
                .userGender(userGender)
                .productColor(productColor)
                .build();
    }
    public void addViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
