package com.fream.back.domain.user.controller;

import com.fream.back.domain.user.dto.follow.FollowDto;
import com.fream.back.domain.user.service.follow.FollowCommandService;
import com.fream.back.domain.user.service.follow.FollowQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowCommandService followCommandService;
    private final FollowQueryService followQueryService;

    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    @PostMapping("/{profileId}")
    public ResponseEntity<String> createFollow(@PathVariable("profileId") Long profileId) {
        String email = extractEmailFromSecurityContext();
        followCommandService.createFollow(email, profileId);
        return ResponseEntity.ok("팔로우가 성공적으로 추가되었습니다.");
    }

    @DeleteMapping("/{profileId}")
    public ResponseEntity<String> deleteFollow(@PathVariable("profileId") Long profileId) {
        String email = extractEmailFromSecurityContext();
        followCommandService.deleteFollow(email, profileId);
        return ResponseEntity.ok("팔로우가 성공적으로 삭제되었습니다.");
    }

    @GetMapping("/followers")
    public ResponseEntity<Page<FollowDto>> getFollowers(Pageable pageable) {
        String email = extractEmailFromSecurityContext();
        Page<FollowDto> followers = followQueryService.getFollowers(email, pageable);
        return ResponseEntity.ok(followers);
    }

    @GetMapping("/followings")
    public ResponseEntity<Page<FollowDto>> getFollowings(Pageable pageable) {
        String email = extractEmailFromSecurityContext();
        Page<FollowDto> followings = followQueryService.getFollowings(email, pageable);
        return ResponseEntity.ok(followings);
    }
}
