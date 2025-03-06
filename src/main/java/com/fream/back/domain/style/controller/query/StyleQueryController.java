package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import com.fream.back.domain.style.service.query.StyleQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/styles/queries")
@RequiredArgsConstructor
public class StyleQueryController {

    private final StyleQueryService styleQueryService;

    @GetMapping("/{styleId}")
    public ResponseEntity<StyleDetailResponseDto> getStyleDetail(
            @PathVariable("styleId") Long styleId) {
        // 현재 로그인한 사용자의 이메일 추출 (없는 경우 null)
        String email = SecurityUtils.extractEmailFromSecurityContext();
        StyleDetailResponseDto detail = styleQueryService.getStyleDetail(styleId, email);
        return ResponseEntity.ok(detail);
    }

    @GetMapping
    public ResponseEntity<Page<StyleResponseDto>> getFilteredStyles(
            @ModelAttribute StyleFilterRequestDto filterRequestDto,
            Pageable pageable
    ) {
        // 현재 로그인한 사용자의 이메일 추출 (없는 경우 null)
        String email = SecurityUtils.extractEmailFromSecurityContext();
        Page<StyleResponseDto> styles = styleQueryService.getFilteredStyles(filterRequestDto, pageable, email);
        return ResponseEntity.ok(styles);
    }

    @GetMapping("/profile/{profileId}")
    public ResponseEntity<Page<ProfileStyleResponseDto>> getStylesByProfile(
            @PathVariable("profileId") Long profileId,
            Pageable pageable
    ) {
        // 현재 로그인한 사용자의 이메일 추출 (없는 경우 null)
        String email = SecurityUtils.extractEmailFromSecurityContext();
        Page<ProfileStyleResponseDto> styles = styleQueryService.getStylesByProfile(profileId, pageable, email);
        return ResponseEntity.ok(styles);
    }
    @GetMapping("/{styleId}/media/{fileName}")
    public ResponseEntity<byte[]> getStyleMedia(
            @PathVariable("styleId") Long styleId,
            @PathVariable("fileName") String fileName
    ) throws IOException {
        // /home/ubuntu/fream/styles/{styleId}/{fileName}
        String baseDir = "/home/ubuntu/fream";
        String directory = "styles/" + styleId;
        String fullPath = baseDir + File.separator + directory + File.separator + fileName;

        File mediaFile = new File(fullPath);
        if (!mediaFile.exists()) {
            throw new IllegalArgumentException("스타일 미디어 파일이 존재하지 않습니다.");
        }

        byte[] fileBytes = Files.readAllBytes(mediaFile.toPath());
        String mimeType = Files.probeContentType(mediaFile.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header("Content-Type", mimeType)
                .body(fileBytes);
    }
}
