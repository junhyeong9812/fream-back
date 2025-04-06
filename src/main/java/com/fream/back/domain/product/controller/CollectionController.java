package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.CollectionRequestDto;
import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.service.collection.CollectionCommandService;
import com.fream.back.domain.product.service.collection.CollectionQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ResponseDto<CollectionResponseDto>> createCollection(@RequestBody CollectionRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        CollectionResponseDto response = collectionCommandService.createCollection(request);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @PutMapping("/{collectionId}")
    public ResponseEntity<ResponseDto<CollectionResponseDto>> updateCollection(
            @PathVariable("collectionId") Long id,
            @RequestBody CollectionRequestDto request) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        CollectionResponseDto response = collectionCommandService.updateCollection(id, request);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @DeleteMapping("/{collectionName}")
    public ResponseEntity<ResponseDto<Void>> deleteCollection(
            @PathVariable("collectionName") String name) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 권한 확인

        collectionCommandService.deleteCollection(name);
        return ResponseEntity.ok(ResponseDto.success(null));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<CollectionResponseDto>>> getAllCollections() {
        List<CollectionResponseDto> response = collectionQueryService.findAllCollections();
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    // 페이징된 컬렉션 조회
    @GetMapping("/page")
    public ResponseEntity<ResponseDto<Page<CollectionResponseDto>>> getCollectionsPaging(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CollectionResponseDto> response = collectionQueryService.findCollectionsPaging(pageable);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    // 컬렉션 검색 (페이징)
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<CollectionResponseDto>>> searchCollections(
            @RequestParam String keyword,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CollectionResponseDto> response = collectionQueryService.searchCollectionsByName(keyword, pageable);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @GetMapping("/{collectionId}")
    public ResponseEntity<ResponseDto<CollectionResponseDto>> getCollectionById(@PathVariable("collectionId") Long id) {
        CollectionResponseDto response = collectionQueryService.findCollectionById(id);
        return ResponseEntity.ok(ResponseDto.success(response));
    }

    @GetMapping("/name/{collectionName}")
    public ResponseEntity<ResponseDto<CollectionResponseDto>> getCollectionByName(@PathVariable("collectionName") String name) {
        CollectionResponseDto response = collectionQueryService.findByName(name);
        return ResponseEntity.ok(ResponseDto.success(response));
    }
}