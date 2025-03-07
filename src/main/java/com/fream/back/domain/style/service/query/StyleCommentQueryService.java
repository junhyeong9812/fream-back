package com.fream.back.domain.style.service.query;

import com.fream.back.domain.style.dto.StyleCommentResponseDto;
import com.fream.back.domain.style.dto.StyleCommentsResponseDto;
import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.repository.StyleCommentRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StyleCommentQueryService {

    private final StyleCommentRepository styleCommentRepository;
    private final StyleCommentLikeQueryService styleCommentLikeQueryService;
    private final ProfileQueryService profileQueryService;

    /**
     * 특정 스타일의 댓글 목록 페이징 조회 (대댓글 포함)
     */
    public StyleCommentsResponseDto getCommentsByStyleId(Long styleId, String email, int page, int size) {
        // 페이지 요청 생성 (최신순)
        Pageable pageable = PageRequest.of(page, size);

        // 루트 댓글 조회
        Page<StyleComment> rootCommentsPage = styleCommentRepository.findRootCommentsByStyleId(styleId, pageable);
        List<StyleComment> rootComments = rootCommentsPage.getContent();

        // 총 댓글 수 조회
        Long totalComments = styleCommentRepository.countCommentsByStyleId(styleId);

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
        }

        // 좋아요 상태 조회
        Set<Long> likedCommentIds = styleCommentLikeQueryService.getLikedCommentIds(email, allCommentIds);

        // DTO 변환
        List<StyleCommentResponseDto> commentDtos = convertToResponseDtos(rootComments, repliesMap, likedCommentIds);

        // 현재 로그인한 사용자의 프로필 이미지 URL 조회
        String userProfileImageUrl = null;
        if (email != null && !email.isEmpty() && !"anonymousUser".equals(email)) {
            try {
                Profile userProfile = profileQueryService.getProfileByEmail(email);
                userProfileImageUrl = userProfile.getProfileImageUrl();
            } catch (Exception e) {
                // 프로필 조회 실패 시 기본값 유지
            }
        }

        return new StyleCommentsResponseDto(commentDtos, totalComments, userProfileImageUrl);
    }

    /**
     * 댓글 엔티티를 DTO로 변환
     */
    private List<StyleCommentResponseDto> convertToResponseDtos(
            List<StyleComment> rootComments,
            Map<Long, List<StyleComment>> repliesMap,
            Set<Long> likedCommentIds
    ) {
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
    }

    /**
     * 특정 댓글 조회
     */
    public StyleComment findCommentById(Long commentId) {
        return styleCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글을 찾을 수 없습니다: " + commentId));
    }

    /**
     * 특정 댓글에 대한 사용자의 좋아요 여부 확인
     */
    public boolean checkUserLikedComment(String email, Long commentId) {
        if (email == null) {
            return false;
        }

        try {
            Profile profile = profileQueryService.getProfileByEmail(email);
            return styleCommentLikeQueryService.isCommentLikedByProfile(commentId, profile.getId());
        } catch (Exception e) {
            return false;
        }
    }
    // 특정 스타일 ID로 연결된 모든 댓글 조회
    public List<StyleComment> findByStyleId(Long styleId) {
        return styleCommentRepository.findByStyleId(styleId);
    }

    // 댓글 ID로 조회
    public StyleComment findById(Long commentId) {
        return styleCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("해당 댓글을 찾을 수 없습니다: " + commentId));
    }

    // 특정 프로필 ID로 작성된 댓글 조회
    public List<StyleComment> findByProfileId(Long profileId) {
        return styleCommentRepository.findByProfileId(profileId);
    }
}

