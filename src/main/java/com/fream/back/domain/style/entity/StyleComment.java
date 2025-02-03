package com.fream.back.domain.style.entity;

import com.fream.back.domain.user.entity.Profile;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StyleComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "style_id", nullable = false)
    private Style style; // 댓글 대상 Style

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile; // 댓글 작성자

    private String content; // 댓글 내용

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", nullable = true)
    private StyleComment parentComment; // 부모 댓글 (null이면 최상위 댓글)

    @Builder.Default
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StyleComment> childComments = new ArrayList<>(); // 대댓글 목록

    @Builder.Default
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StyleCommentLike> likes = new ArrayList<>(); // 댓글 좋아요 목록


    //연관관계 메서드
    public void assignStyle(Style style) {
        this.style = style;
    }

    public void assignProfile(Profile profile) {
        this.profile = profile;
    }
    public void addChildComment(StyleComment childComment) {
        this.childComments.add(childComment);
        childComment.setParentComment(this);
    }

    public void setParentComment(StyleComment parentComment) {
        this.parentComment = parentComment;
    }

    public void addLike(StyleCommentLike like) {
        this.likes.add(like);
        like.assignComment(this);
    }
    // StyleComment 엔티티
    public void updateContent(String newContent) {
        if (newContent == null || newContent.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 비어 있을 수 없습니다.");
        }
        this.content = newContent; // Dirty Checking
    }
}

