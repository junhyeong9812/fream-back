package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Hashtag;
import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleOrderItem;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
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
    private final ProfileQueryService profileQueryService;
    private final StyleOrderItemCommandService styleOrderItemCommandService;
    private final MediaUrlCommandService mediaUrlCommandService;
    private final HashtagRepository hashtagRepository;
    private final HashtagCommandService hashtagCommandService;
    private final StyleHashtagCommandService styleHashtagCommandService;

    /**
     * 스타일 생성 (해시태그 처리 추가)
     *
     * @param email 사용자 이메일
     * @param orderItemIds 주문 아이템 ID 목록
     * @param content 스타일 내용
     * @param mediaFiles 미디어 파일 목록
     * @param hashtagNames 해시태그 이름 목록
     * @return 생성된 스타일 객체
     * @throws StyleException 스타일 생성 실패 시
     */
    public Style createStyle(String email, List<Long> orderItemIds, String content,
                             List<MultipartFile> mediaFiles, List<String> hashtagNames) {
        log.info("스타일 생성 시작: email={}, orderItemIds 수={}, 해시태그 수={}",
                email, (orderItemIds != null ? orderItemIds.size() : 0),
                (hashtagNames != null ? hashtagNames.size() : 0));

        try {
            // 유효성 검사
            if (email == null || email.isEmpty()) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "사용자 이메일이 필요합니다.");
            }

            if (content == null || content.trim().isEmpty()) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 내용이 필요합니다.");
            }

            // 프로필 조회 - ProfileQueryService 사용
            Profile profile = profileQueryService.getProfileByEmail(email);
            log.debug("프로필 조회 성공: profileId={}, profileName={}", profile.getId(), profile.getProfileName());

            // 스타일 생성
            Style style = Style.builder()
                    .profile(profile)
                    .content(content)
                    .viewCount(0L)
                    .build();

            // 프로필에 스타일 할당
            style.assignProfile(profile);
            Style savedStyle = styleRepository.save(style);
            log.debug("스타일 기본 정보 저장 완료: styleId={}", savedStyle.getId());

            // 주문 아이템 처리
            if (orderItemIds != null && !orderItemIds.isEmpty()) {
                log.debug("스타일 주문 아이템 처리 시작: styleId={}, 주문 아이템 수={}",
                        savedStyle.getId(), orderItemIds.size());

                for (Long orderItemId : orderItemIds) {
                    try {
                        StyleOrderItem styleOrderItem = styleOrderItemCommandService.createStyleOrderItem(orderItemId, savedStyle);
                        savedStyle.addStyleOrderItem(styleOrderItem);
                        log.debug("스타일 주문 아이템 처리 완료: styleId={}, orderItemId={}",
                                savedStyle.getId(), orderItemId);
                    } catch (Exception e) {
                        log.error("주문 아이템 처리 중 오류 발생: styleId={}, orderItemId={}, 원인={}",
                                savedStyle.getId(), orderItemId, e.getMessage());
                        // 개별 주문 아이템 오류는 전체 프로세스를 중단하지 않음 (나머지 처리 계속)
                    }
                }
            }

            // 미디어 파일 처리 - MediaUrlCommandService 사용
            if (mediaFiles != null && !mediaFiles.isEmpty()) {
                log.debug("스타일 미디어 파일 처리 시작: styleId={}, 미디어 파일 수={}",
                        savedStyle.getId(), mediaFiles.size());

                for (MultipartFile mediaFile : mediaFiles) {
                    if (mediaFile == null || mediaFile.isEmpty()) {
                        log.warn("빈 미디어 파일 스킵: styleId={}", savedStyle.getId());
                        continue;
                    }

                    try {
                        // MediaUrl 엔티티 생성 및 저장
                        MediaUrl mediaUrl = mediaUrlCommandService.saveMediaFile(savedStyle, mediaFile);
                        log.debug("미디어 파일 저장 완료: styleId={}, mediaUrlId={}",
                                savedStyle.getId(), mediaUrl.getId());
                    } catch (Exception e) {
                        log.error("미디어 파일 처리 중 오류 발생: styleId={}, 파일명={}, 원인={}",
                                savedStyle.getId(), mediaFile.getOriginalFilename(), e.getMessage());
                        // 개별 미디어 파일 오류는 전체 프로세스를 중단하지 않음 (나머지 처리 계속)
                    }
                }
            }

            // 해시태그 처리
            if (hashtagNames != null && !hashtagNames.isEmpty()) {
                log.debug("스타일 해시태그 처리 시작: styleId={}, 해시태그 수={}",
                        savedStyle.getId(), hashtagNames.size());
                processHashtags(savedStyle, hashtagNames);
            }

            log.info("스타일 생성 완료: styleId={}", savedStyle.getId());
            return savedStyle;

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 생성 중 예상치 못한 오류 발생: email={}", email, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 해시태그 처리 로직 (생성 또는 기존 태그 사용)
     *
     * @param style 스타일 객체
     * @param hashtagNames 해시태그 이름 목록
     */
    private void processHashtags(Style style, List<String> hashtagNames) {
        for (String name : hashtagNames) {
            try {
                // 해시태그 이름 정제 (앞에 # 제거)
                String cleanName = name.startsWith("#") ? name.substring(1) : name;

                if (cleanName.isEmpty()) {
                    log.warn("빈 해시태그 스킵: styleId={}", style.getId());
                    continue;
                }

                // 기존 해시태그 검색
                Optional<Hashtag> existingHashtag = hashtagRepository.findByName(cleanName);
                Hashtag hashtag;

                if (existingHashtag.isPresent()) {
                    // 기존 해시태그 사용
                    hashtag = existingHashtag.get();
                    log.debug("기존 해시태그 사용: styleId={}, hashtagId={}, name={}",
                            style.getId(), hashtag.getId(), hashtag.getName());
                } else {
                    // 새 해시태그 생성
                    log.debug("새 해시태그 생성: styleId={}, name={}", style.getId(), cleanName);
                    com.fream.back.domain.style.dto.HashtagCreateRequestDto createDto = new com.fream.back.domain.style.dto.HashtagCreateRequestDto();
                    createDto.setName(cleanName);
                    hashtag = hashtagRepository.findById(hashtagCommandService.create(createDto).getId())
                            .orElseThrow(() -> new StyleException(StyleErrorCode.HASHTAG_NOT_FOUND, "해시태그 생성 실패"));
                }

                // 스타일에 해시태그 연결
                styleHashtagCommandService.addHashtagToStyle(style, hashtag);
                log.debug("해시태그 스타일 연결 완료: styleId={}, hashtagId={}", style.getId(), hashtag.getId());
            } catch (Exception e) {
                log.error("해시태그 처리 중 오류 발생: styleId={}, 해시태그={}, 원인={}",
                        style.getId(), name, e.getMessage());
                // 개별 해시태그 오류는 전체 프로세스를 중단하지 않음 (나머지 처리 계속)
            }
        }
    }

    /**
     * 스타일 업데이트 (해시태그 처리 추가)
     *
     * @param styleId 업데이트할 스타일 ID
     * @param content 업데이트할 내용
     * @param newMediaFiles 새로 추가할 미디어 파일
     * @param existingUrls 유지할 기존 미디어 URL
     * @param hashtagNames 업데이트할 해시태그 이름 목록
     * @throws StyleException 스타일 업데이트 실패 시
     */
    public void updateStyle(Long styleId, String content, List<MultipartFile> newMediaFiles,
                            List<String> existingUrls, List<String> hashtagNames) {
        log.info("스타일 업데이트 시작: styleId={}", styleId);

        try {
            Style style = styleRepository.findById(styleId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.STYLE_NOT_FOUND,
                            "스타일을 찾을 수 없습니다: " + styleId));

            // 텍스트 콘텐츠 업데이트
            if (content != null && !content.equals(style.getContent())) {
                style.updateContent(content);
                log.debug("스타일 내용 업데이트 완료: styleId={}", styleId);
            }

            // 기존 미디어 URL 처리
            if (existingUrls != null) {
                log.debug("기존 미디어 URL, 처리 시작: styleId={}, 유지할 URL 수={}",
                        styleId, existingUrls.size());

                // 현재 스타일에 있는 모든 미디어 URL 가져오기
                List<MediaUrl> currentMediaUrls = new ArrayList<>(style.getMediaUrls());

                // 프론트엔드에서 전달된 URL 목록에 없는 것은 삭제
                for (MediaUrl mediaUrl : currentMediaUrls) {
                    if (!existingUrls.contains(mediaUrl.getUrl())) {
                        try {
                            log.debug("미디어 URL 삭제: styleId={}, mediaUrlId={}, url={}",
                                    styleId, mediaUrl.getId(), mediaUrl.getUrl());
                            mediaUrlCommandService.deleteMediaUrl(mediaUrl);
                        } catch (Exception e) {
                            log.error("미디어 URL 삭제 중 오류 발생: styleId={}, mediaUrlId={}, 원인={}",
                                    styleId, mediaUrl.getId(), e.getMessage());
                            // 개별 미디어 오류는 전체 프로세스를 중단하지 않음
                        }
                    }
                }
            }

            // 새 미디어 파일 추가
            if (newMediaFiles != null && !newMediaFiles.isEmpty()) {
                log.debug("새 미디어 파일 추가 시작: styleId={}, 추가할 파일 수={}",
                        styleId, newMediaFiles.size());

                for (MultipartFile mediaFile : newMediaFiles) {
                    if (mediaFile == null || mediaFile.isEmpty()) {
                        log.warn("빈 미디어 파일 스킵: styleId={}", styleId);
                        continue;
                    }

                    try {
                        mediaUrlCommandService.saveMediaFile(style, mediaFile);
                        log.debug("새 미디어 파일 추가 완료: styleId={}, 파일명={}",
                                styleId, mediaFile.getOriginalFilename());
                    } catch (Exception e) {
                        log.error("새 미디어 파일 추가 중 오류 발생: styleId={}, 파일명={}, 원인={}",
                                styleId, mediaFile.getOriginalFilename(), e.getMessage());
                        // 개별 미디어 파일 오류는 전체 프로세스를 중단하지 않음
                    }
                }
            }

            // 해시태그 처리 (기존 해시태그 제거 후 새로 추가)
            if (hashtagNames != null) {
                log.debug("해시태그 업데이트 시작: styleId={}, 새 해시태그 수={}",
                        styleId, hashtagNames.size());

                try {
                    // 기존 해시태그 연결 모두 제거
                    styleHashtagCommandService.removeAllHashtagsFromStyle(styleId);
                    log.debug("기존 해시태그 연결 제거 완료: styleId={}", styleId);

                    // 새 해시태그 추가
                    processHashtags(style, hashtagNames);
                    log.debug("새 해시태그 추가 완료: styleId={}", styleId);
                } catch (Exception e) {
                    log.error("해시태그 업데이트 중 오류 발생: styleId={}, 원인={}", styleId, e.getMessage());
                    // 해시태그 오류는 전체 프로세스를 중단하지 않음
                }
            }

            styleRepository.save(style);
            log.info("스타일 업데이트 완료: styleId={}", styleId);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 업데이트 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스타일 삭제 (해시태그 연결 제거 추가)
     *
     * @param styleId 삭제할 스타일 ID
     * @throws StyleException 스타일 삭제 실패 시
     */
    public void deleteStyle(Long styleId) {
        log.info("스타일 삭제 시작: styleId={}", styleId);

        try {
            Style style = styleRepository.findById(styleId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.STYLE_NOT_FOUND,
                            "스타일을 찾을 수 없습니다: " + styleId));

            // 해시태그 연결 제거
            try {
                styleHashtagCommandService.removeAllHashtagsFromStyle(styleId);
                log.debug("스타일 해시태그 연결 제거 완료: styleId={}", styleId);
            } catch (Exception e) {
                log.error("해시태그 연결 제거 중 오류 발생: styleId={}, 원인={}", styleId, e.getMessage());
                // 해시태그 오류는 전체 삭제 프로세스를 중단하지 않음
            }

            // 미디어 URL을 통한 파일 삭제
            List<MediaUrl> mediaUrls = new ArrayList<>(style.getMediaUrls());
            for (MediaUrl mediaUrl : mediaUrls) {
                try {
                    mediaUrlCommandService.deleteMediaUrl(mediaUrl);
                    log.debug("스타일 미디어 파일 삭제 완료: styleId={}, mediaUrlId={}",
                            styleId, mediaUrl.getId());
                } catch (Exception e) {
                    log.error("미디어 파일 삭제 중 오류 발생: styleId={}, mediaUrlId={}, 원인={}",
                            styleId, mediaUrl.getId(), e.getMessage());
                    // 개별 미디어 삭제 오류는 전체 프로세스를 중단하지 않음
                }
            }

            // 스타일 삭제
            styleRepository.delete(style);
            log.info("스타일 삭제 완료: styleId={}", styleId);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 삭제 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 뷰 카운트 증가
     *
     * @param styleId 뷰 카운트를 증가시킬 스타일 ID
     * @throws StyleException 스타일이 없거나 뷰 카운트 증가 실패 시
     */
    public void incrementViewCount(Long styleId) {
        log.debug("스타일 뷰 카운트 증가 시작: styleId={}", styleId);

        try {
            Style style = styleRepository.findById(styleId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.STYLE_NOT_FOUND,
                            "스타일을 찾을 수 없습니다: " + styleId));
            style.incrementViewCount();
            styleRepository.save(style);
            log.debug("스타일 뷰 카운트 증가 완료: styleId={}, 새 뷰 카운트={}", styleId, style.getViewCount());
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 뷰 카운트 증가 중 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 뷰 카운트 증가 중 오류가 발생했습니다.", e);
        }
    }
}