package com.fream.back.domain.inspection.controller.command;

import com.fream.back.domain.inspection.dto.InspectionStandardCreateRequestDto;
import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.dto.InspectionStandardUpdateRequestDto;
import com.fream.back.domain.inspection.service.command.InspectionStandardCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
public class InspectionCommandController {

    private final InspectionStandardCommandService commandService;
    private final UserQueryService userQueryService;

    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    // === 검수 기준 생성 ===
    @PostMapping
    public ResponseEntity<InspectionStandardResponseDto> createStandard(
            @ModelAttribute InspectionStandardCreateRequestDto requestDto) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        InspectionStandardResponseDto response = commandService.createStandard(requestDto);
        return ResponseEntity.ok(response);
    }

    // === 검수 기준 수정 ===
    @PutMapping("/{id}")
    public ResponseEntity<InspectionStandardResponseDto> updateStandard(
            @PathVariable("id") Long id,
            @ModelAttribute InspectionStandardUpdateRequestDto requestDto) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        InspectionStandardResponseDto response = commandService.updateStandard(id, requestDto);
        return ResponseEntity.ok(response);
    }

    // === 검수 기준 삭제 ===
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStandard(@PathVariable("id") Long id) throws IOException {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        commandService.deleteStandard(id);
        return ResponseEntity.noContent().build();
    }
}

