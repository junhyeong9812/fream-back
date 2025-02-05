package com.fream.back.domain.style.service.command;

import com.fream.back.domain.order.service.query.OrderItemQueryService;
import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.repository.StyleRepository;
import com.fream.back.domain.style.service.query.MediaUrlQueryService;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class StyleCommandService {

    private final StyleRepository styleRepository;
    private final ProfileQueryService profileQueryService;
    private final OrderItemQueryService orderItemQueryService;
    private final FileUtils fileUtils;
    private final StyleOrderItemCommandService styleOrderItemCommandService;
    private final MediaUrlCommandService mediaUrlCommandService;
    private final MediaUrlQueryService mediaUrlQueryService;

    private static final String STYLE_MEDIA_DIRECTORY = System.getProperty("user.dir") +  "/style/";

    // 스타일 생성
    public Style createStyle(String email, List<Long> orderItemIds, String content, List<MultipartFile> mediaFiles) {
        // 1. 프로필 조회
        Profile profile = profileQueryService.getProfileByEmail(email);

        // 2. Style 생성
        Style style = Style.builder()
                .profile(profile)
                .content(content)
                .viewCount(0L) // 초기 뷰 카운트
                .build();
        Style savedStyle = styleRepository.save(style);

        // 3. StyleOrderItem 생성
        for (Long orderItemId : orderItemIds) {
            styleOrderItemCommandService.createStyleOrderItem(orderItemId, savedStyle);
        }

        // 4. MediaUrl 저장
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            for (MultipartFile mediaFile : mediaFiles) {
                MediaUrl mediaUrl = mediaUrlCommandService.saveMediaFile(savedStyle, mediaFile);
                savedStyle.addMediaUrl(mediaUrl); // 연관관계 메서드 호출
            }
        }


        return savedStyle;
    }

    // 뷰 카운트 증가
    public void incrementViewCount(Long styleId) {
        Style style = styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Style을 찾을 수 없습니다: " + styleId));
        style.incrementViewCount();
    }

    // 스타일 업데이트
    public void updateStyle(Long styleId, String content, List<MultipartFile> newMediaFiles, List<String> existingUrlsFromFrontend) {
        // 1. 스타일 조회
        Style style = styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Style을 찾을 수 없습니다: " + styleId));

        // 2. 텍스트 콘텐츠 업데이트
        if (content != null) {
            style.updateContent(content);
        }

        // 3. 기존 MediaUrl 목록 조회
        List<MediaUrl> existingMediaUrls = mediaUrlQueryService.findMediaUrlsByStyleId(styleId);

        // 4. 기존 MediaUrl 중 삭제해야 할 URL 제거
        // 4. 기존 MediaUrl 중 삭제해야 할 URL 제거
        if (existingUrlsFromFrontend == null || existingUrlsFromFrontend.isEmpty()) {
            // 프론트엔드에서 아무 URL도 제공하지 않은 경우, 기존 모든 MediaUrl 삭제
            for (MediaUrl mediaUrl : existingMediaUrls) {
                mediaUrlCommandService.deleteMediaUrl(mediaUrl);
            }
        } else {
            // 프론트엔드에서 제공한 URL과 비교하여 삭제
            for (MediaUrl mediaUrl : existingMediaUrls) {
                if (!existingUrlsFromFrontend.contains(mediaUrl.getUrl())) {
                    mediaUrlCommandService.deleteMediaUrl(mediaUrl);
                }
            }
        }

        // 5. 새로운 MediaUrl 저장
        if (newMediaFiles != null && !newMediaFiles.isEmpty()) {
            for (MultipartFile mediaFile : newMediaFiles) {
                MediaUrl mediaUrl = mediaUrlCommandService.saveMediaFile(style, mediaFile);
                style.addMediaUrl(mediaUrl); // 스타일과의 연관 관계 설정
            }
        }
    }
    // 스타일 삭제
    public void deleteStyle(Long styleId) {
        Style style = styleRepository.findById(styleId)
                .orElseThrow(() -> new IllegalArgumentException("해당 Style을 찾을 수 없습니다: " + styleId));
        styleRepository.delete(style);
    }

}

