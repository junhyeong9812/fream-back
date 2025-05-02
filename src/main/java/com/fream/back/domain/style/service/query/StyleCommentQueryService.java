package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.StyleCommentResponseDto;
import com.fream.back.domain.style.dto.StyleCommentsResponseDto;
import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.repository.StyleCommentRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleCommentQueryService {

    private final StyleCommentRepository styleCommentRepository;
    private final StyleCommentLikeQueryService styleCommentLikeQueryService;
    private final ProfileQueryService profileQueryService;

    /**
     * 특정 스타일의 댓글 목록 페이징 조회 (대댓글 포함)
     *
     * @param styleId 스타일 ID
     * @param email 사용자 이메일
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 댓글 목록 응답 DTO
     */
    public StyleCommentsResponseDto getCommentsByStyleId(Long styleId, String email, int page, int size) {
        log.debug("스타일 댓글 목록 조회 시작: styleId={}, email={}, page={}, size={}",
                styleId, email, page, size);

        try {
            // 입력값 검증
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            if (page < 0) {
                log.warn("페이지 번호가 0보다 작습니다. 0으로 재설정: page={}", page);
                page = 0;
            }

            if (size <= 0 || size > 100) {
                log.warn("페이지 크기가 유효하지 않습니다. 10으로 재설정: size={}", size);
                size = 10;
            }

            // 페이지 요청 생성 (최신순)
            Pageable pageable = PageRequest.of(page, size);

            // 루트 댓글 조회
            Page<StyleComment> rootCommentsPage = styleCommentRepository.findRootCommentsByStyleId(styleId, pageable);
            List<StyleComment> rootComments = rootCommentsPage.getContent();
            log.debug("루트 댓글 조회 완료: styleId={}, 루트 댓글 수={}", styleId, rootComments.size());

            // 총 댓글 수 조회
            Long totalComments = styleCommentRepository.countCommentsByStyleId(styleId);
            log.debug("총 댓글 수 조회 완료: styleId={}, 총 댓글 수={}", styleId, totalComments);

            // 댓글 ID 목록 (루트 댓글 + 대댓글)
            List<Long> allCommentIds = new ArrayList<>();
            Map<Long, List<StyleComment>> repliesMap = new HashMap<>();

            // 루트 댓글 ID 추가
            rootComments.forEach(comment -> allCommentIds.add(comment.getId()));

            // 각 루트 댓글의 대댓글 조회 및 ID 추가
            for (StyleComment rootComment : rootComments) {
                List<StyleComment> replies = styleCommentRepository.findRepliesByParentCommentId(rootComment.getId());
                repliesMap.put(rootComment.getId(), replies);

                replies.forEach(reply -> allCommentIds.add(reply.getId()));
                log.debug("루트 댓글의 대댓글 조회 완료: rootCommentId={}, 대댓글 수={}",
                        rootComment.getId(), replies.size());
            }

            // 좋아요 상태 조회
            Set<Long> likedCommentIds = Collections.emptySet();
            if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
                likedCommentIds = styleCommentLikeQueryService.getLikedCommentIds(email, allCommentIds);
                log.debug("좋아요 상태 조회 완료: email={}, 좋아요 댓글 수={}/{}",
                        email, likedCommentIds.size(), allCommentIds.size());
            }

            // DTO 변환
            List<StyleCommentResponseDto> commentDtos = convertToResponseDtos(rootComments, repliesMap, likedCommentIds);
            log.debug("댓글 DTO 변환 완료: styleId={}, DTO 수={}", styleId, commentDtos.size());

            // 현재 로그인한 사용자의 프로필 이미지 URL 조회
            String userProfileImageUrl = null;
            if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
                try {
                    Profile userProfile = profileQueryService.getProfileByEmail(email);
                    userProfileImageUrl = userProfile.getProfileImageUrl();
                    log.debug("사용자 프로필 이미지 URL 조회 완료: email={}, profileImageUrl={}",
                            email, userProfileImageUrl);
                } catch (Exception e) {
                    log.warn("사용자 프로필 조회 실패 - 기본값 null 사용: email={}, 원인={}",
                            email, e.getMessage());
                }
            }

            StyleCommentsResponseDto response = new StyleCommentsResponseDto(commentDtos, totalComments, userProfileImageUrl);
            log.info("스타일 댓글 목록 조회 완료: styleId={}, 총 댓글 수={}, 현재 페이지 댓글 수={}",
                    styleId, totalComments, commentDtos.size());

            return response;

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 댓글 목록 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 댓글 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 엔티티를 DTO로 변환
     */
    private List<StyleCommentResponseDto> convertToResponseDtos(
            List<StyleComment> rootComments,
            Map<Long, List<StyleComment>> repliesMap,
            Set<Long> likedCommentIds
    ) {
        try {
            return rootComments.stream()
                    .map(rootComment -> {
                        // 루트 댓글의 DTO 생성
                        StyleCommentResponseDto dto = StyleCommentResponseDto.builder()
                                .id(rootComment.getId())
                                .profileId(rootComment.getProfile().getId())
                                .profileName(rootComment.getProfile().getProfileName())
                                .profileImageUrl(rootComment.getProfile().getProfileImageUrl())
                                .content(rootComment.getContent())
                                .likeCount((long) rootComment.getLikes().size())
                                .liked(likedCommentIds.contains(rootComment.getId()))
                                .createdDate(rootComment.getCreatedDate())
                                .build();

                        // 대댓글이 있으면 DTO 목록 생성
                        List<StyleComment> replies = repliesMap.getOrDefault(rootComment.getId(), Collections.emptyList());
                        List<StyleCommentResponseDto> replyDtos = replies.stream()
                                .map(reply -> StyleCommentResponseDto.builder()
                                        .id(reply.getId())
                                        .profileId(reply.getProfile().getId())
                                        .profileName(reply.getProfile().getProfileName())
                                        .profileImageUrl(reply.getProfile().getProfileImageUrl())
                                        .content(reply.getContent())
                                        .likeCount((long) reply.getLikes().size())
                                        .liked(likedCommentIds.contains(reply.getId()))
                                        .createdDate(reply.getCreatedDate())
                                        .build())
                                .collect(Collectors.toList());

                        dto.setReplies(replyDtos);
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("댓글 DTO 변환 중 오류 발생", e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "댓글 정보 변환 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 댓글 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글 엔티티
     * @throws StyleException 댓글을 찾을 수 없는 경우
     */
    public StyleComment findCommentById(Long commentId) {
        log.debug("ID로 댓글 조회 시작: commentId={}", commentId);

        try {
            if (commentId == null) {
                throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
            }

            StyleComment comment = styleCommentRepository.findById(commentId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                            "해당 댓글을 찾을 수 없습니다: " + commentId));

            log.debug("ID로 댓글 조회 완료: commentId={}, styleId={}",
                    commentId, comment.getStyle().getId());

            return comment;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("ID로 댓글 조회 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                    "댓글 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 댓글에 대한 사용자의 좋아요 여부 확인
     *
     * @param email 사용자 이메일
     * @param commentId 댓글 ID
     * @return 좋아요 여부
     */
    public boolean checkUserLikedComment(String email, Long commentId) {
        log.debug("사용자의 댓글 좋아요 여부 확인 시작: email={}, commentId={}", email, commentId);

        if (email == null) {
            log.debug("이메일이 null이므로 좋아요 하지 않음");
            return false;
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            boolean isLiked = styleCommentLikeQueryService.isCommentLikedByProfile(commentId, profile.getId());

            log.debug("사용자의 댓글 좋아요 여부 확인 완료: email={}, commentId={}, 좋아요={}",
                    email, commentId, isLiked);

            return isLiked;
        } catch (Exception e) {
            log.warn("사용자의 댓글 좋아요 여부 확인 중 오류 발생 - false 반환: email={}, commentId={}",
                    email, commentId, e);
            return false;
        }
    }

    /**
     * 특정 스타일 ID로 연결된 모든 댓글 조회
     *
     * @param styleId 스타일 ID
     * @return 댓글 엔티티 목록
     */
    public List<StyleComment> findByStyleId(Long styleId) {
        log.debug("스타일 ID로 모든 댓글 조회 시작: styleId={}", styleId);

        try {
            if (styleId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "스타일 ID가 필요합니다.");
            }

            List<StyleComment> comments = styleCommentRepository.findByStyleId(styleId);
            log.debug("스타일 ID로 모든 댓글 조회 완료: styleId={}, 댓글 수={}", styleId, comments.size());

            return comments;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("스타일 ID로 모든 댓글 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일의 댓글 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 댓글 ID로 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글 엔티티
     * @throws StyleException 댓글을 찾을 수 없는 경우
     */
    public StyleComment findById(Long commentId) {
        log.debug("댓글 ID로 조회 시작: commentId={}", commentId);

        try {
            if (commentId == null) {
                throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND, "댓글 ID가 필요합니다.");
            }

            StyleComment comment = styleCommentRepository.findById(commentId)
                    .orElseThrow(() -> new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                            "해당 댓글을 찾을 수 없습니다: " + commentId));

            log.debug("댓글 ID로 조회 완료: commentId={}", commentId);

            return comment;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("댓글 ID로 조회 중 예상치 못한 오류 발생: commentId={}", commentId, e);
            throw new StyleException(StyleErrorCode.COMMENT_NOT_FOUND,
                    "댓글 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 프로필 ID로 작성된 댓글 조회
     *
     * @param profileId 프로필 ID
     * @return 댓글 엔티티 목록
     */
    public List<StyleComment> findByProfileId(Long profileId) {
        log.debug("프로필 ID로 댓글 조회 시작: profileId={}", profileId);

        try {
            if (profileId == null) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "프로필 ID가 필요합니다.");
            }

            List<StyleComment> comments = styleCommentRepository.findByProfileId(profileId);
            log.debug("프로필 ID로 댓글 조회 완료: profileId={}, 댓글 수={}", profileId, comments.size());

            return comments;
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("프로필 ID로 댓글 조회 중 예상치 못한 오류 발생: profileId={}", profileId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "프로필의 댓글 목록 조회 중 오류가 발생했습니다.", e);
        }
    }
}