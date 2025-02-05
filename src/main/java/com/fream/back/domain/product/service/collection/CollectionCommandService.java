package com.fream.back.domain.product.service.collection;

import com.fream.back.domain.product.dto.CollectionRequestDto;
import com.fream.back.domain.product.dto.CollectionResponseDto;
import com.fream.back.domain.product.entity.Collection;
import com.fream.back.domain.product.repository.CollectionRepository;
import com.fream.back.domain.product.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@AllArgsConstructor
public class CollectionCommandService {

    private final CollectionRepository collectionRepository;
    private final ProductRepository productRepository;


    public CollectionResponseDto createCollection(CollectionRequestDto request) {
        if (collectionRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 컬렉션 이름입니다.");
        }

        Collection collection = Collection.builder()
                .name(request.getName())
                .build();
        collectionRepository.save(collection);
        return CollectionResponseDto.fromEntity(collection);
    }

    public CollectionResponseDto updateCollection(Long id, CollectionRequestDto request) {
        Collection collection = collectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 컬렉션입니다."));

        // 더티체크를 위한 업데이트
        collection.updateName(request.getName());
        return CollectionResponseDto.fromEntity(collection);
    }

    public void deleteCollection(String name) {
        Collection collection = collectionRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 컬렉션입니다."));

        boolean hasAssociatedProducts = productRepository.existsByCollection(collection);
        if (hasAssociatedProducts) {
            throw new IllegalStateException("해당 컬렉션과 연관된 상품이 존재합니다. 연관된 상품을 삭제 후 컬렉션을 삭제해주세요.");
        }

        collectionRepository.delete(collection);
    }

    public Collection createOrGetCollection(String name) {
        return collectionRepository.findByName(name)
                .orElseGet(() -> collectionRepository.save(Collection.builder().name(name).build()));
    }
}
