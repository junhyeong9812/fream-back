package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.dto.HashtagCreateRequestDto;
import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleOrderItem;
import com.fream.back.domain.style.repository.HashtagRepository;
import com.fream.back.domain.style.repository.StyleRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StyleCommandService {

    private final StyleRepository styleRepository;
    private final ProfileQueryService profileQueryService; // 프로필 쿼리 서비스 사용
    private final StyleOrderItemCommandService styleOrderItemCommandService;
    private final MediaUrlCommandService mediaUrlCommandService;
    private final HashtagRepository hashtagRepository;
    private final HashtagCommandService hashtagCommandService;
    private final StyleHashtagCommandService styleHashtagCommandService;

    /**
     * 스타일 생성 (해시태그 처리 추가)
     */
    public Style createStyle(String email, List<Long> orderItemIds, String content,
                             List<MultipartFile> mediaFiles, List<String> hashtagNames) {
        // 프로필 조회 - ProfileQueryService 사용
        Profile profile = profileQueryService.getProfileByEmail(email);

        // 스타일 생성
        Style style = Style.builder()
                .profile(profile)
                .content(content)
                .viewCount(0L)
                .build();

        // 프로필에 스타일 할당
        style.assignProfile(profile);
        Style savedStyle = styleRepository.save(style);

        // 주문 아이템 처리
        if (orderItemIds != null && !orderItemIds.isEmpty()) {
            for (Long orderItemId : orderItemIds) {
                StyleOrderItem styleOrderItem = styleOrderItemCommandService.createStyleOrderItem(orderItemId,savedStyle);
                savedStyle.addStyleOrderItem(styleOrderItem);
            }
        }

        // 미디어 파일 처리 - MediaUrlCommandService 사용
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile mediaFile : mediaFiles) {
                // MediaUrl 엔티티 생성 및 저장
                mediaUrlCommandService.saveMediaFile(savedStyle, mediaFile);
            }
        }

        // 해시태그 처리
        if (hashtagNames != null && !hashtagNames.isEmpty()) {
            processHashtags(savedStyle, hashtagNames);
        }

        return savedStyle;
    }

    /**
     * 해시태그 처리 로직 (생성 또는 기존 태그 사용)
     */
    private void processHashtags(Style style, List<String> hashtagNames) {
        for (String name : hashtagNames) {
            // 해시태그 이름 정제 (앞에 # 제거)
            String cleanName = name.startsWith("#") ? name.substring(1) : name;

            if (cleanName.isEmpty()) {
                continue;
            }

            // 기존 해시태그 검색
            Optional<Hashtag> existingHashtag = hashtagRepository.findByName(cleanName);
            Hashtag hashtag;

            if (existingHashtag.isPresent()) {
                // 기존 해시태그 사용
                hashtag = existingHashtag.get();
            } else {
                // 새 해시태그 생성
                HashtagCreateRequestDto createDto = new HashtagCreateRequestDto();
                createDto.setName(cleanName);
                hashtag = hashtagRepository.findById(hashtagCommandService.create(createDto).getId())
                        .orElseThrow(() -> new IllegalStateException("해시태그 생성 실패"));
            }

            // 스타일에 해시태그 연결
            styleHashtagCommandService.addHashtagToStyle(style, hashtag);
        }
    }

    /**
     * 스타일 업데이트 (해시태그 처리 추가)
     */
    public void updateStyle(Long styleId, String content, List<MultipartFile> newMediaFiles,
                            List<String> existingUrls, List<String> hashtagNames) {
        Style style = styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("스타일을 찾을 수 없습니다: " + styleId));

        // 텍스트 콘텐츠 업데이트
        if (content != null && !content.equals(style.getContent())) {
            style.updateContent(content);
        }

        // 기존 미디어 URL 처리
        if (existingUrls != null) {
            // 현재 스타일에 있는 모든 미디어 URL 가져오기
            List<MediaUrl> currentMediaUrls = new ArrayList<>(style.getMediaUrls());

            // 프론트엔드에서 전달된 URL 목록에 없는 것은 삭제
            for (MediaUrl mediaUrl : currentMediaUrls) {
                if (!existingUrls.contains(mediaUrl.getUrl())) {
                    mediaUrlCommandService.deleteMediaUrl(mediaUrl);
                }
            }
        }

        // 새 미디어 파일 추가
        if (newMediaFiles != null && !newMediaFiles.isEmpty()) {
            for (MultipartFile mediaFile : newMediaFiles) {
                mediaUrlCommandService.saveMediaFile(style, mediaFile);
            }
        }

        // 해시태그 처리 (기존 해시태그 제거 후 새로 추가)
        if (hashtagNames != null) {
            // 기존 해시태그 연결 모두 제거
            styleHashtagCommandService.removeAllHashtagsFromStyle(styleId);

            // 새 해시태그 추가
            processHashtags(style, hashtagNames);
        }

        styleRepository.save(style);
    }

    /**
     * 스타일 삭제 (해시태그 연결 제거 추가)
     */
    public void deleteStyle(Long styleId) {
        Style style = styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("스타일을 찾을 수 없습니다: " + styleId));

        // 해시태그 연결 제거
        styleHashtagCommandService.removeAllHashtagsFromStyle(styleId);

        // 미디어 URL을 통한 파일 삭제
        List<MediaUrl> mediaUrls = new ArrayList<>(style.getMediaUrls());
        for (MediaUrl mediaUrl : mediaUrls) {
            mediaUrlCommandService.deleteMediaUrl(mediaUrl);
        }

        // 스타일 삭제
        styleRepository.delete(style);
    }

    /**
     * 뷰 카운트 증가
     */
    public void incrementViewCount(Long styleId) {
        Style style = styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("스타일을 찾을 수 없습니다: " + styleId));
        style.incrementViewCount();
        styleRepository.save(style);
    }
}