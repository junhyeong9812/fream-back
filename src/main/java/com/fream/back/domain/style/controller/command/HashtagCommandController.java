package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.dto.HashtagCreateRequestDto;
import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.dto.HashtagUpdateRequestDto;
import com.fream.back.domain.style.service.command.HashtagCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hashtags/commands")
@RequiredArgsConstructor
public class HashtagCommandController {

    private final HashtagCommandService hashtagCommandService;

    /**
     * 해시태그 생성
     */
    @PostMapping
    public ResponseEntity<HashtagResponseDto> createHashtag(@RequestBody HashtagCreateRequestDto requestDto) {
        // 관리자 권한 확인 (필요한 경우)
        // SecurityUtils.checkAdminRole();

        HashtagResponseDto createdHashtag = hashtagCommandService.create(requestDto);
        return ResponseEntity.ok(createdHashtag);
    }

    /**
     * 해시태그 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<HashtagResponseDto> updateHashtag(
            @PathVariable("id") Long id,
            @RequestBody HashtagUpdateRequestDto requestDto) {
        // 관리자 권한 확인 (필요한 경우)
        // SecurityUtils.checkAdminRole();

        HashtagResponseDto updatedHashtag = hashtagCommandService.update(id, requestDto);
        return ResponseEntity.ok(updatedHashtag);
    }

    /**
     * 해시태그 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHashtag(@PathVariable("id") Long id) {
        // 관리자 권한 확인 (필요한 경우)
        // SecurityUtils.checkAdminRole();

        hashtagCommandService.delete(id);
        return ResponseEntity.ok().build();
    }
}