package com.fream.back.domain.notice.controller.command;

import com.fream.back.domain.notice.dto.NoticeCreateRequestDto;
import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.dto.NoticeUpdateRequestDto;
import com.fream.back.domain.notice.service.command.NoticeCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeCommandController {

    private final NoticeCommandService noticeCommandService;
    private final UserQueryService userQueryService; // 권한 확인 서비스

    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    // === 공지사항 생성 ===
    @PostMapping
    public ResponseEntity<NoticeResponseDto> createNotice(@ModelAttribute NoticeCreateRequestDto requestDto) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        NoticeResponseDto response = noticeCommandService.createNotice(
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getCategory(),
                requestDto.getFiles()
        );
        return ResponseEntity.ok(response);
    }

    // === 공지사항 수정 ===
    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeResponseDto> updateNotice(
            @PathVariable("noticeId") Long noticeId,
            @ModelAttribute NoticeUpdateRequestDto requestDto
    ) throws IOException {
        NoticeResponseDto response = noticeCommandService.updateNotice(
                noticeId,
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getCategory(),
                requestDto.getExistingImageUrls(),
                requestDto.getNewFiles()
        );
        return ResponseEntity.ok(response);
    }

    // === 공지사항 삭제 ===
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable("noticeId") Long noticeId) throws IOException {
        noticeCommandService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}
