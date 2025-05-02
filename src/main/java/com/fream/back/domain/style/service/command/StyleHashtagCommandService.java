package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleHashtag;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.HashtagRepository;
import com.fream.back.domain.style.repository.StyleHashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StyleHashtagCommandService {

    private final StyleHashtagRepository styleHashtagRepository;
    private final HashtagRepository hashtagRepository;

    /**
     * 스타일에 해시태그 추가 (기존 해시태그 사용)
     *
     * @param style 스타일 엔티티
     * @param hashtag 해시태그 엔티티
     * @return 생성된 스타일-해시태그 연결 엔티티
     * @throws StyleException 해시태그 연결 실패 시
     */
    public StyleHashtag addHashtagToStyle(Style style, Hashtag hashtag) {
        log.debug("스타일에 해시태그 추가 시작: styleId={}, hashtagId={}, hashtagName={}",
                style.getId(), hashtag.getId(), hashtag.getName());

        try {
            // 이미 연결되어 있는지 확인
            Optional<StyleHashtag> existingLink = styleHashtagRepository
                    .findByStyleIdAndHashtagId(style.getId(), hashtag.getId());

            if (existingLink.isPresent()) {
                log.debug("이미 연결된 해시태그: styleId={}, hashtagId={}",
                        style.getId(), hashtag.getId());
                return existingLink.get();
            }

            // 해시태그 사용 횟수 증가
            hashtag.incrementCount();
            hashtagRepository.save(hashtag);
            log.debug("해시태그 사용 횟수 증가: hashtagId={}, 새 카운트={}",
                    hashtag.getId(), hashtag.getCount());

            // 스타일-해시태그 연결 생성
            StyleHashtag styleHashtag = StyleHashtag.builder()
                    .style(style)
                    .hashtag(hashtag)
                    .build();

            // 양방향 연관관계 설정
            styleHashtag.assignStyle(style);
            styleHashtag.assignHashtag(hashtag);
            style.addStyleHashtag(styleHashtag);

            StyleHashtag savedLink = styleHashtagRepository.save(styleHashtag);
            log.info("스타일-해시태그 연결 완료: linkId={}, styleId={}, hashtagId={}",
                    savedLink.getId(), style.getId(), hashtag.getId());

            return savedLink;

        } catch (Exception e) {
            log.error("스타일에 해시태그 추가 중 오류 발생: styleId={}, hashtagId={}",
                    style.getId(), hashtag.getId(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일에 해시태그 추가 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스타일에서 모든 해시태그 제거
     *
     * @param styleId 스타일 ID
     * @throws StyleException 해시태그 제거 실패 시
     */
    public void removeAllHashtagsFromStyle(Long styleId) {
        log.debug("스타일에서 모든 해시태그 제거 시작: styleId={}", styleId);

        try {
            List<StyleHashtag> styleHashtags = styleHashtagRepository.findByStyleId(styleId);
            log.debug("스타일에 연결된 해시태그 수: styleId={}, 해시태그 수={}",
                    styleId, styleHashtags.size());

            // 각 해시태그의 카운트 감소
            for (StyleHashtag styleHashtag : styleHashtags) {
                try {
                    Hashtag hashtag = styleHashtag.getHashtag();
                    hashtag.decrementCount();
                    hashtagRepository.save(hashtag);
                    log.debug("해시태그 사용 횟수 감소: hashtagId={}, 새 카운트={}",
                            hashtag.getId(), hashtag.getCount());
                } catch (Exception e) {
                    log.warn("해시태그 카운트 감소 중 오류 발생: hashtagId={}, 원인={}",
                            styleHashtag.getHashtag().getId(), e.getMessage());
                    // 계속 진행
                }
            }

            // 연결 삭제
            styleHashtagRepository.deleteByStyleId(styleId);
            log.info("스타일에서 모든 해시태그 제거 완료: styleId={}, 제거된 연결 수={}",
                    styleId, styleHashtags.size());

        } catch (Exception e) {
            log.error("스타일에서 해시태그 제거 중 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일에서 해시태그 제거 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스타일에서 특정 해시태그 제거
     *
     * @param styleId 스타일 ID
     * @param hashtagId 해시태그 ID
     * @throws StyleException 해시태그 제거 실패 시
     */
    public void removeHashtagFromStyle(Long styleId, Long hashtagId) {
        log.debug("스타일에서 특정 해시태그 제거 시작: styleId={}, hashtagId={}", styleId, hashtagId);

        try {
            Optional<StyleHashtag> styleHashtag = styleHashtagRepository
                    .findByStyleIdAndHashtagId(styleId, hashtagId);

            if (styleHashtag.isPresent()) {
                // 해시태그 카운트 감소
                Hashtag hashtag = styleHashtag.get().getHashtag();
                hashtag.decrementCount();
                hashtagRepository.save(hashtag);
                log.debug("해시태그 사용 횟수 감소: hashtagId={}, 새 카운트={}",
                        hashtag.getId(), hashtag.getCount());

                // 연결 삭제
                styleHashtagRepository.delete(styleHashtag.get());
                log.info("스타일에서 특정 해시태그 제거 완료: styleId={}, hashtagId={}",
                        styleId, hashtagId);
            } else {
                log.warn("제거할 스타일-해시태그 연결을 찾을 수 없음: styleId={}, hashtagId={}",
                        styleId, hashtagId);
            }
        } catch (Exception e) {
            log.error("스타일에서 특정 해시태그 제거 중 오류 발생: styleId={}, hashtagId={}",
                    styleId, hashtagId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일에서 해시태그 제거 중 오류가 발생했습니다.", e);
        }
    }
}