package com.fream.back.domain.product.service.collection;

import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.entity.Collection;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 컬렉션 조회(Query) 서비스
 * 컬렉션 조회 관련 기능을 제공합니다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CollectionQueryService {

    private final CollectionRepository collectionRepository;

    /**
     * 모든 컬렉션 조회
     *
     * @return 컬렉션 응답 DTO 목록
     */
    public List<CollectionResponseDto> findAllCollections() {
        log.info("모든 컬렉션 조회 요청");

        try {
            List<Collection> collections = collectionRepository.findAllByOrderByNameDesc();

            List<CollectionResponseDto> collectionDtos = collections.stream()
                    .map(CollectionResponseDto::fromEntity)
                    .collect(Collectors.toList());

            log.info("모든 컬렉션 조회 성공 - 컬렉션 수: {}", collectionDtos.size());
            return collectionDtos;
        } catch (Exception e) {
            log.error("모든 컬렉션 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                    "컬렉션 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션명으로 컬렉션 조회
     *
     * @param name 컬렉션명
     * @return 컬렉션 응답 DTO
     * @throws ProductException 컬렉션이 존재하지 않을 경우
     */
    public CollectionResponseDto findByName(String name) {
        log.info("컬렉션명으로 컬렉션 조회 - 컬렉션명: {}", name);

        try {
            Collection collection = collectionRepository.findByName(name)
                    .orElseThrow(() -> {
                        log.warn("컬렉션 조회 실패 - 존재하지 않는 컬렉션명: {}", name);
                        return new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                                "해당 컬렉션이 존재하지 않습니다: " + name);
                    });

            log.debug("컬렉션명으로 컬렉션 조회 성공 - 컬렉션ID: {}, 컬렉션명: {}",
                    collection.getId(), collection.getName());
            return CollectionResponseDto.fromEntity(collection);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("컬렉션명으로 컬렉션 조회 중 예상치 못한 오류 발생 - 컬렉션명: {}", name, e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                    "컬렉션 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 ID로 컬렉션 조회
     *
     * @param id 컬렉션 ID
     * @return 컬렉션 응답 DTO
     * @throws ProductException 컬렉션이 존재하지 않을 경우
     */
    public CollectionResponseDto findCollectionById(Long id) {
        log.info("컬렉션 ID로 컬렉션 조회 - 컬렉션ID: {}", id);

        try {
            Collection collection = collectionRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("컬렉션 조회 실패 - 존재하지 않는 컬렉션ID: {}", id);
                        return new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                                "해당 컬렉션이 존재하지 않습니다. ID: " + id);
                    });

            log.debug("컬렉션 ID로 컬렉션 조회 성공 - 컬렉션ID: {}, 컬렉션명: {}",
                    id, collection.getName());
            return CollectionResponseDto.fromEntity(collection);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("컬렉션 ID로 컬렉션 조회 중 예상치 못한 오류 발생 - 컬렉션ID: {}", id, e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                    "컬렉션 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 페이징으로 컬렉션 조회
     *
     * @param pageable 페이징 정보
     * @return 페이징된 컬렉션 응답 DTO
     */
    public Page<CollectionResponseDto> findCollectionsPaging(Pageable pageable) {
        log.info("페이징으로 컬렉션 조회 - 페이지: {}, 사이즈: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<Collection> collectionPage = collectionRepository.findAll(pageable);
            Page<CollectionResponseDto> dtoPage = collectionPage.map(CollectionResponseDto::fromEntity);

            log.info("페이징으로 컬렉션 조회 성공 - 총 컬렉션 수: {}, 총 페이지 수: {}",
                    dtoPage.getTotalElements(), dtoPage.getTotalPages());
            return dtoPage;
        } catch (Exception e) {
            log.error("페이징으로 컬렉션 조회 중 예상치 못한 오류 발생", e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                    "컬렉션 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션명으로 검색 (페이징)
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 페이징된 컬렉션 응답 DTO
     */
    public Page<CollectionResponseDto> searchCollectionsByName(String keyword, Pageable pageable) {
        log.info("컬렉션명으로 검색 - 키워드: {}, 페이지: {}, 사이즈: {}",
                keyword, pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<Collection> collectionPage = collectionRepository.findByNameContainingIgnoreCase(keyword, pageable);
            Page<CollectionResponseDto> dtoPage = collectionPage.map(CollectionResponseDto::fromEntity);

            log.info("컬렉션명으로 검색 성공 - 키워드: {}, 검색 결과 수: {}",
                    keyword, dtoPage.getTotalElements());
            return dtoPage;
        } catch (Exception e) {
            log.error("컬렉션명으로 검색 중 예상치 못한 오류 발생 - 키워드: {}", keyword, e);
            throw new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                    "컬렉션 검색 중 오류가 발생했습니다.", e);
        }
    }
}