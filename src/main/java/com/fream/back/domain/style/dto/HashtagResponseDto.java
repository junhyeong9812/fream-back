package com.fream.back.domain.style.dto;

import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.entity.StyleHashtag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponseDto {
    private Long id;
    private String name;
    private Long count;

    /**
     * Hashtag 엔티티를 HashtagResponseDto로 변환
     */
    public static HashtagResponseDto from(Hashtag hashtag) {
        if (hashtag == null) {
            return null;
        }

        return HashtagResponseDto.builder()
                .id(hashtag.getId())
                .name(hashtag.getName())
                .count(hashtag.getCount())
                .build();
    }

    /**
     * StyleHashtag 엔티티를 HashtagResponseDto로 변환
     */
    public static HashtagResponseDto from(StyleHashtag styleHashtag) {
        if (styleHashtag == null || styleHashtag.getHashtag() == null) {
            return null;
        }

        return HashtagResponseDto.builder()
                .id(styleHashtag.getHashtag().getId())
                .name(styleHashtag.getHashtag().getName())
                .count(styleHashtag.getHashtag().getCount())
                .build();
    }
}