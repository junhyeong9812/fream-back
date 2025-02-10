package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.CollectionRequestDto;
import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.service.collection.CollectionCommandService;
import com.fream.back.domain.product.service.collection.CollectionQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionCommandService collectionCommandService;
    private final CollectionQueryService collectionQueryService;
    private final UserQueryService userQueryService;

    // 관리자 권한 확인용 메서드
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }

    @PostMapping
    public ResponseEntity<CollectionResponseDto> createCollection(@RequestBody CollectionRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        return ResponseEntity.ok(collectionCommandService.createCollection(request));
    }

    @PutMapping("/{collectionId}")
    public ResponseEntity<CollectionResponseDto> updateCollection(
            @PathVariable("collectionId") Long id,
            @RequestBody CollectionRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        return ResponseEntity.ok(collectionCommandService.updateCollection(id, request));
    }

    @DeleteMapping("/{collectionName}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable("collectionName") String name) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        collectionCommandService.deleteCollection(name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<CollectionResponseDto>> getAllCollections() {
        return ResponseEntity.ok(collectionQueryService.findAllCollections());
    }
}
