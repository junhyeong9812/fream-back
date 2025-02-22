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
public class Style extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile; // 작성자


    private String content; // 텍스트 컨텐츠
    @Builder.Default
    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StyleOrderItem> styleOrderItems = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaUrl> mediaUrls = new ArrayList<>(); // 미디어 URL 리스트

    private Long viewCount; // 뷰 카운트

    @Builder.Default
    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StyleLike> likes = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StyleComment> comments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "style", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StyleInterest> interests = new ArrayList<>();



    // 뷰 카운트 증가
    public void incrementViewCount() {
        this.viewCount++;
    }

    // 콘텐츠 업데이트
    public void updateContent(String content) {
        this.content = content;
    }

    //연관관계 메서드
    public void addLike(StyleLike like) {
        this.likes.add(like);
        like.assignStyle(this);
    }
    // 좋아요 제거
    public void removeLike(StyleLike like) {
        this.likes.remove(like);
    }

    // 관심 제거
    public void removeInterest(StyleInterest interest) {
        this.interests.remove(interest);
    }
    // 연관관계 메서드
    public void addStyleOrderItem(StyleOrderItem styleOrderItem) {
        this.styleOrderItems.add(styleOrderItem);
        styleOrderItem.assignStyle(this);
    }
    public void addDataStyleOrderItem(StyleOrderItem styleOrderItem) {
        this.styleOrderItems.add(styleOrderItem);
//        styleOrderItem.assignStyle(this);
    }

    public void addMediaUrl(MediaUrl mediaUrl) {
        this.mediaUrls.add(mediaUrl);
        mediaUrl.assignStyle(this);
    }
    public void addDataMediaUrl(MediaUrl mediaUrl) {
        this.mediaUrls.add(mediaUrl);
//        mediaUrl.assignStyle(this);
    }

    public void addComment(StyleComment comment) {
        this.comments.add(comment);
        comment.assignStyle(this);
    }

    public void addInterest(StyleInterest interest) {
        this.interests.add(interest);
        interest.assignStyle(this);
    }

    public void assignProfile(Profile profile) {
        this.profile = profile;
        if (profile != null && !profile.getStyles().contains(this)) {
            profile.addStyle(this); // 양방향 동기화
        }
    }

    public void removeMediaUrl(MediaUrl mediaUrl) {
        this.mediaUrls.remove(mediaUrl);
        mediaUrl.unassignStyle(); // MediaUrl에서 Style 해제
    }

    public void removeStyleOrderItem(StyleOrderItem styleOrderItem) {
        this.styleOrderItems.remove(styleOrderItem);
        styleOrderItem.unassignStyle();
    }

}

