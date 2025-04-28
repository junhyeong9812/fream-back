package com.fream.back.domain.product.service.collection;

import com.fream.back.domain.product.dto.CollectionRequestDto;
import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.entity.Collection;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.CollectionRepository;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 컬렉션 명령(Command) 서비스
 * 컬렉션의 생성, 수정, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CollectionCommandService {

    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;

    /**
     * 컬렉션 생성
     *
     * @param request 컬렉션 생성 요청 DTO
     * @return 생성된 컬렉션 응답 DTO
     * @throws ProductException 컬렉션 생성 실패 시
     */
    public CollectionResponseDto createCollection(CollectionRequestDto request) {
        log.info("컬렉션 생성 요청 - 컬렉션명: {}", request.getName());

        try {
            if (collectionRepository.findByName(request.getName()).isPresent()) {
                log.warn("컬렉션 생성 실패 - 이미 존재하는 컬렉션명: {}", request.getName());
                throw new ProductException(ProductErrorCode.COLLECTION_ALREADY_EXISTS,
                        "이미 존재하는 컬렉션 이름입니다: " + request.getName());
            }

            Collection collection = Collection.builder()
                    .name(request.getName())
                    .build();

            Collection savedCollection = collectionRepository.save(collection);

            log.info("컬렉션 생성 성공 - 컬렉션ID: {}, 컬렉션명: {}",
                    savedCollection.getId(), savedCollection.getName());
            return CollectionResponseDto.fromEntity(savedCollection);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("컬렉션 생성 중 예상치 못한 오류 발생 - 컬렉션명: {}", request.getName(), e);
            throw new ProductException(ProductErrorCode.COLLECTION_CREATION_FAILED,
                    "컬렉션 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 수정
     *
     * @param id 컬렉션 ID
     * @param request 컬렉션 수정 요청 DTO
     * @return 수정된 컬렉션 응답 DTO
     * @throws ProductException 컬렉션 수정 실패 시
     */
    public CollectionResponseDto updateCollection(Long id, CollectionRequestDto request) {
        log.info("컬렉션 수정 요청 - 컬렉션ID: {}, 새 컬렉션명: {}", id, request.getName());

        try {
            Collection collection = collectionRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("컬렉션 수정 실패 - 존재하지 않는 컬렉션ID: {}", id);
                        return new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                                "존재하지 않는 컬렉션입니다. ID: " + id);
                    });

            // 이름 중복 확인
            if (!collection.getName().equals(request.getName()) &&
                    collectionRepository.existsByName(request.getName())) {
                log.warn("컬렉션 수정 실패 - 이미 존재하는 컬렉션명: {}", request.getName());
                throw new ProductException(ProductErrorCode.COLLECTION_ALREADY_EXISTS,
                        "이미 존재하는 컬렉션 이름입니다: " + request.getName());
            }

            // 더티체크를 위한 업데이트
            collection.updateName(request.getName());

            log.info("컬렉션 수정 성공 - 컬렉션ID: {}, 새 컬렉션명: {}", id, collection.getName());
            return CollectionResponseDto.fromEntity(collection);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("컬렉션 수정 중 예상치 못한 오류 발생 - 컬렉션ID: {}", id, e);
            throw new ProductException(ProductErrorCode.COLLECTION_UPDATE_FAILED,
                    "컬렉션 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 삭제
     *
     * @param name 컬렉션명
     * @throws ProductException 컬렉션 삭제 실패 시
     */
    public void deleteCollection(String name) {
        log.info("컬렉션 삭제 요청 - 컬렉션명: {}", name);

        try {
            Collection collection = collectionRepository.findByName(name)
                    .orElseThrow(() -> {
                        log.warn("컬렉션 삭제 실패 - 존재하지 않는 컬렉션명: {}", name);
                        return new ProductException(ProductErrorCode.COLLECTION_NOT_FOUND,
                                "존재하지 않는 컬렉션입니다: " + name);
                    });

            boolean hasAssociatedProducts = productRepository.existsByCollection(collection);
            if (hasAssociatedProducts) {
                log.warn("컬렉션 삭제 실패 - 연관된 상품이 존재하는 컬렉션: {}", name);
                throw new ProductException(ProductErrorCode.COLLECTION_IN_USE,
                        "해당 컬렉션과 연관된 상품이 존재합니다. 연관된 상품을 삭제 후 컬렉션을 삭제해주세요.");
            }

            collectionRepository.delete(collection);
            log.info("컬렉션 삭제 성공 - 컬렉션ID: {}, 컬렉션명: {}", collection.getId(), name);
        } catch (ProductException e) {
            throw e; // 이미 적절한 예외라면 그대로 전파
        } catch (Exception e) {
            log.error("컬렉션 삭제 중 예상치 못한 오류 발생 - 컬렉션명: {}", name, e);
            throw new ProductException(ProductErrorCode.COLLECTION_DELETION_FAILED,
                    "컬렉션 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 컬렉션 생성 또는 조회
     * 이미 존재하는 컬렉션명이면 해당 컬렉션을 반환하고, 없으면 새로 생성합니다.
     *
     * @param name 컬렉션명
     * @return 컬렉션 엔티티
     */
    public Collection createOrGetCollection(String name) {
        log.info("컬렉션 생성 또는 조회 요청 - 컬렉션명: {}", name);

        try {
            return collectionRepository.findByName(name)
                    .orElseGet(() -> {
                        log.debug("새 컬렉션 생성 - 컬렉션명: {}", name);
                        Collection newCollection = Collection.builder().name(name).build();
                        Collection savedCollection = collectionRepository.save(newCollection);
                        log.info("새 컬렉션 생성 성공 - 컬렉션ID: {}, 컬렉션명: {}",
                                savedCollection.getId(), savedCollection.getName());
                        return savedCollection;
                    });
        } catch (Exception e) {
            log.error("컬렉션 생성 또는 조회 중 예상치 못한 오류 발생 - 컬렉션명: {}", name, e);
            throw new ProductException(ProductErrorCode.COLLECTION_CREATION_FAILED,
                    "컬렉션 생성 또는 조회 중 오류가 발생했습니다.", e);
        }
    }
}