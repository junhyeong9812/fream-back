package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleComment;
import com.fream.back.domain.style.repository.StyleCommentRepository;
import com.fream.back.domain.style.service.query.StyleQueryService;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.service.profile.ProfileQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StyleCommentCommandService {

    private final StyleCommentRepository styleCommentRepository;
    private final ProfileQueryService profileQueryService;
    private final StyleQueryService styleQueryService;

    // 댓글 생성
    public StyleComment addComment(String email, Long styleId, String content, Long parentCommentId) {
        // 1. 프로필 조회
        Profile profile = profileQueryService.getProfileByEmail(email);

        // 2. 스타일 조회
        Style style = styleQueryService.findStyleById(styleId);

        // 3. 부모 댓글 조회 (Optional)
        StyleComment parentComment = parentCommentId != null
                ? styleCommentRepository.findById(parentCommentId)
                .orElseThrow(() -> new IllegalArgumentException("부모 댓글을 찾을 수 없습니다: " + parentCommentId))
                : null;

        // 4. 댓글 생성
        StyleComment comment = StyleComment.builder()
                .style(style)
                .profile(profile)
                .content(content)
                .parentComment(parentComment)
                .build();

        // 5. 연관관계 설정
        style.addComment(comment);          // Style -> Comment
        if (parentComment != null) {
            parentComment.addChildComment(comment); // Parent -> Child Comment
        }

        // 6. 댓글 저장
        return styleCommentRepository.save(comment);
    }
    // 댓글 수정
    public void updateComment(Long commentId, String updatedContent) {
        StyleComment comment = styleCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        // 댓글 내용 업데이트
        comment.updateContent(updatedContent);
    }

    // 댓글 삭제
    public void deleteComment(Long commentId) {
        StyleComment comment = styleCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다: " + commentId));

        styleCommentRepository.delete(comment); // 자식 댓글은 cascade로 자동 삭제
    }
}

