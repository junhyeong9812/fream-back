package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleHashtag;
import com.fream.back.domain.style.repository.HashtagRepository;
import com.fream.back.domain.style.repository.StyleHashtagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class StyleHashtagCommandService {

    private final StyleHashtagRepository styleHashtagRepository;
    private final HashtagRepository hashtagRepository;

    /**
     * 스타일에 해시태그 추가 (기존 해시태그 사용)
     */
    public StyleHashtag addHashtagToStyle(Style style, Hashtag hashtag) {
        // 이미 연결되어 있는지 확인
        Optional<StyleHashtag> existingLink = styleHashtagRepository.findByStyleIdAndHashtagId(style.getId(), hashtag.getId());
        if (existingLink.isPresent()) {
            return existingLink.get();
        }

        // 해시태그 사용 횟수 증가
        hashtag.incrementCount();
        hashtagRepository.save(hashtag);

        // 스타일-해시태그 연결 생성
        StyleHashtag styleHashtag = StyleHashtag.builder()
                .style(style)
                .hashtag(hashtag)
                .build();

        // 양방향 연관관계 설정
        styleHashtag.assignStyle(style);
        styleHashtag.assignHashtag(hashtag);
        style.addStyleHashtag(styleHashtag);

        return styleHashtagRepository.save(styleHashtag);
    }

    /**
     * 스타일에서 모든 해시태그 제거
     */
    public void removeAllHashtagsFromStyle(Long styleId) {
        List<StyleHashtag> styleHashtags = styleHashtagRepository.findByStyleId(styleId);

        // 각 해시태그의 카운트 감소
        for (StyleHashtag styleHashtag : styleHashtags) {
            Hashtag hashtag = styleHashtag.getHashtag();
            hashtag.decrementCount();
            hashtagRepository.save(hashtag);
        }

        // 연결 삭제
        styleHashtagRepository.deleteByStyleId(styleId);
    }

    /**
     * 스타일에서 특정 해시태그 제거
     */
    public void removeHashtagFromStyle(Long styleId, Long hashtagId) {
        Optional<StyleHashtag> styleHashtag = styleHashtagRepository.findByStyleIdAndHashtagId(styleId, hashtagId);

        if (styleHashtag.isPresent()) {
            // 해시태그 카운트 감소
            Hashtag hashtag = styleHashtag.get().getHashtag();
            hashtag.decrementCount();
            hashtagRepository.save(hashtag);

            // 연결 삭제
            styleHashtagRepository.delete(styleHashtag.get());
        }
    }
}