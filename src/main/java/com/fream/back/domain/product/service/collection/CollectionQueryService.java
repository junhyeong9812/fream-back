package com.fream.back.domain.product.service.collection;

import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.repository.CollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}