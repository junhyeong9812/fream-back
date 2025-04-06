package com.fream.back.domain.product.service.collection;

import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.repository.CollectionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CollectionQueryService {

    private final CollectionRepository collectionRepository;

    public CollectionQueryService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    public List<CollectionResponseDto> findAllCollections() {
        return collectionRepository.findAllByOrderByNameDesc()
                .stream()
                .map(CollectionResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    // 컬렉션명으로 DTO 조회
    public CollectionResponseDto findByName(String name) {
        com.fream.back.domain.product.entity.Collection collection = collectionRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("해당 컬렉션이 존재하지 않습니다."));
        return CollectionResponseDto.fromEntity(collection);
    }

    // 컬렉션 ID로 DTO 조회
    public CollectionResponseDto findCollectionById(Long id) {
        com.fream.back.domain.product.entity.Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 컬렉션이 존재하지 않습니다."));
        return CollectionResponseDto.fromEntity(collection);
    }

    // 페이징으로 컬렉션 조회
    public Page<CollectionResponseDto> findCollectionsPaging(Pageable pageable) {
        return collectionRepository.findAll(pageable)
                .map(CollectionResponseDto::fromEntity);
    }

    // 컬렉션명으로 검색 (페이징)
    public Page<CollectionResponseDto> searchCollectionsByName(String keyword, Pageable pageable) {
        return collectionRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(CollectionResponseDto::fromEntity);
    }
}