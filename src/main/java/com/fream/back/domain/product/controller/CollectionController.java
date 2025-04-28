package com.fream.back.domain.product.controller;

import com.fream.back.domain.product.dto.CollectionRequestDto;
import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.service.collection.CollectionCommandService;
import com.fream.back.domain.product.service.collection.CollectionQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 컬렉션 관련 컨트롤러
 * 컬렉션의 생성, 수정, 삭제, 조회 기능을 제공합니다.
 */
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
@Slf4j
public class CollectionController {

    private final CollectionCommandService collectionCommandService;
    private final CollectionQueryService collectionQueryService;
    private final UserQueryService userQueryService;

    /**
     * 관리자 권한 확인용 메서드
     * 로그인된 사용자의 이메일을 반환합니다.
     *
     * @return 사용자 이메일
     * @throws ProductException 인증된 사용자가 없는 경우
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new ProductException(ProductErrorCode.COLLECTION_CREATION_FAILED, "인증된 사용자가 없습니다.");
    }

    /**
     * 컬렉션 생성 API
     *
     * @param request 컬렉션 생성 요청 DTO
     * @return 생성된 컬렉션 정보
     */
    @PostMapping
    public ResponseEntity<ResponseDto<CollectionResponseDto>> createCollection(@RequestBody CollectionRequestDto request) {
        log.info("컬렉션 생성 요청 - 컬렉션명: {}", request.getName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            CollectionResponseDto response = collectionCommandService.createCollection(request);

            log.info("컬렉션 생성 성공 - 컬렉션ID: {}, 컬렉션명: {}", response.getId(), response.getName());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("컬렉션 생성 실패 - 컬렉션명: {}, 오류: {}", request.getName(), e.getMessage(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_CREATION_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("컬렉션 생성 중 예상치 못한 오류 발생 - 컬렉션명: {}", request.getName(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_CREATION_FAILED, "컬렉션 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 수정 API
     *
     * @param id 컬렉션 ID
     * @param request 컬렉션 수정 요청 DTO
     * @return 수정된 컬렉션 정보
     */
    @PutMapping("/{collectionId}")
    public ResponseEntity<ResponseDto<CollectionResponseDto>> updateCollection(
            @PathVariable("collectionId") Long id,
            @RequestBody CollectionRequestDto request) {
        log.info("컬렉션 수정 요청 - 컬렉션ID: {}, 컬렉션명: {}", id, request.getName());

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            CollectionResponseDto response = collectionCommandService.updateCollection(id, request);

            log.info("컬렉션 수정 성공 - 컬렉션ID: {}, 컬렉션명: {}", id, response.getName());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("컬렉션 수정 실패 - 컬렉션ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_UPDATE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("컬렉션 수정 중 예상치 못한 오류 발생 - 컬렉션ID: {}", id, e);
            throw new ProductException(ProductErrorCode.COLLECTION_UPDATE_FAILED, "컬렉션 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 삭제 API
     *
     * @param name 컬렉션명
     * @return 성공 응답
     */
    @DeleteMapping("/{collectionName}")
    public ResponseEntity<ResponseDto<Void>> deleteCollection(
            @PathVariable("collectionName") String name) {
        log.info("컬렉션 삭제 요청 - 컬렉션명: {}", name);

        try {
            String email = extractEmailFromSecurityContext();
            userQueryService.checkAdminRole(email); // 권한 확인

            log.debug("관리자 권한 확인 완료: {}", email);

            collectionCommandService.deleteCollection(name);

            log.info("컬렉션 삭제 성공 - 컬렉션명: {}", name);
            return ResponseEntity.ok(ResponseDto.success(null));
        } catch (IllegalArgumentException e) {
            log.error("컬렉션 삭제 실패 - 컬렉션명: {}, 오류: {}", name, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_DELETION_FAILED, e.getMessage(), e);
        } catch (IllegalStateException e) {
            log.error("컬렉션 삭제 실패 (사용 중) - 컬렉션명: {}, 오류: {}", name, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_IN_USE, e.getMessage(), e);
        } catch (Exception e) {
            log.error("컬렉션 삭제 중 예상치 못한 오류 발생 - 컬렉션명: {}", name, e);
            throw new ProductException(ProductErrorCode.COLLECTION_DELETION_FAILED, "컬렉션 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 모든 컬렉션 조회 API
     *
     * @return 컬렉션 목록
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<CollectionResponseDto>>> getAllCollections() {
        log.info("모든 컬렉션 조회 요청");

        try {
            List<CollectionResponseDto> response = collectionQueryService.findAllCollections();

            log.info("컬렉션 조회 성공 - 컬렉션 수: {}", response.size());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            log.error("컬렉션 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, "컬렉션 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 페이징된 컬렉션 조회 API
     *
     * @param pageable 페이징 정보
     * @return 페이징된 컬렉션 목록
     */
    @GetMapping("/page")
    public ResponseEntity<ResponseDto<Page<CollectionResponseDto>>> getCollectionsPaging(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("페이징된 컬렉션 조회 요청 - 페이지: {}, 사이즈: {}", pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<CollectionResponseDto> response = collectionQueryService.findCollectionsPaging(pageable);

            log.info("페이징된 컬렉션 조회 성공 - 총 컬렉션 수: {}, 현재 페이지: {}", response.getTotalElements(), response.getNumber());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            log.error("페이징된 컬렉션 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, "컬렉션 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 검색 API (페이징)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 컬렉션 목록
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<Page<CollectionResponseDto>>> searchCollections(
            @RequestParam String keyword,
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        log.info("컬렉션 검색 요청 - 키워드: {}, 페이지: {}, 사이즈: {}", keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<CollectionResponseDto> response = collectionQueryService.searchCollectionsByName(keyword, pageable);

            log.info("컬렉션 검색 성공 - 키워드: {}, 검색 결과 수: {}", keyword, response.getTotalElements());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (Exception e) {
            log.error("컬렉션 검색 중 예상치 못한 오류 발생 - 키워드: {}", keyword, e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, "컬렉션 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 ID로 조회 API
     *
     * @param id 컬렉션 ID
     * @return 컬렉션 정보
     */
    @GetMapping("/{collectionId:[0-9]+}")
    public ResponseEntity<ResponseDto<CollectionResponseDto>> getCollectionById(@PathVariable("collectionId") Long id) {
        log.info("컬렉션 ID로 조회 요청 - 컬렉션ID: {}", id);

        try {
            CollectionResponseDto response = collectionQueryService.findCollectionById(id);

            log.info("컬렉션 ID로 조회 성공 - 컬렉션ID: {}, 컬렉션명: {}", id, response.getName());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("컬렉션 ID로 조회 실패 - 컬렉션ID: {}, 오류: {}", id, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("컬렉션 ID로 조회 중 예상치 못한 오류 발생 - 컬렉션ID: {}", id, e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, "컬렉션 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션명으로 조회 API
     *
     * @param name 컬렉션명
     * @return 컬렉션 정보
     */
    @GetMapping("/name/{collectionName}")
    public ResponseEntity<ResponseDto<CollectionResponseDto>> getCollectionByName(@PathVariable("collectionName") String name) {
        log.info("컬렉션명으로 조회 요청 - 컬렉션명: {}", name);

        try {
            CollectionResponseDto response = collectionQueryService.findByName(name);

            log.info("컬렉션명으로 조회 성공 - 컬렉션명: {}, 컬렉션ID: {}", name, response.getId());
            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (IllegalArgumentException e) {
            log.error("컬렉션명으로 조회 실패 - 컬렉션명: {}, 오류: {}", name, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, e.getMessage(), e);
        } catch (Exception e) {
            log.error("컬렉션명으로 조회 중 예상치 못한 오류 발생 - 컬렉션명: {}", name, e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND, "컬렉션 조회 중 오류가 발생했습니다.", e);
        }
    }
}