package com.fream.back.domain.style.entity;

import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(indexes = {
        @Index(name = "idx_stylehashtag_style", columnList = "style_id"),
        @Index(name = "idx_stylehashtag_hashtag", columnList = "hashtag_id"),
        @Index(name = "idx_stylehashtag_unique", columnList = "style_id, hashtag_id", unique = true)
})
public class StyleHashtag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id", nullable = false)
    private Hashtag hashtag;

    // 연관관계 메서드
    public void assignStyle(Style style) {
        this.style = style;
    }

    public void assignHashtag(Hashtag hashtag) {
        this.hashtag = hashtag;
    }
}