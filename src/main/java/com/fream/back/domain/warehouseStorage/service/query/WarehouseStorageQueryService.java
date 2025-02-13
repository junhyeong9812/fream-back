package com.fream.back.domain.warehouseStorage.service.query;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.warehouseStorage.dto.WarehouseStatusCountDto;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStatus;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.repository.WarehouseStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseStorageQueryService {

    private final WarehouseStorageRepository warehouseStorageRepository;

    @Transactional(readOnly = true)
    public WarehouseStorage findBySale(Sale sale) {
        return warehouseStorageRepository.findBySale(sale)
                .orElseThrow(() -> new IllegalArgumentException("해당 Sale에 연결된 창고 정보를 찾을 수 없습니다."));
    }
    @Transactional(readOnly = true)
    public WarehouseStatusCountDto getWarehouseStatusCount(String userEmail) {
        List<WarehouseStorage> storages = warehouseStorageRepository.findByUser_Email(userEmail);

        return WarehouseStatusCountDto.builder()
                .inStorageCount(countByStatus(storages, WarehouseStatus.IN_STORAGE))
                .associatedWithOrderCount(countByStatus(storages, WarehouseStatus.ASSOCIATED_WITH_ORDER))
                .removedFromStorageCount(countByStatus(storages, WarehouseStatus.REMOVED_FROM_STORAGE))
                .onAuctionCount(countByStatus(storages, WarehouseStatus.ON_AUCTION))
                .soldCount(countByStatus(storages, WarehouseStatus.SOLD))
                .build();
    }

    private long countByStatus(List<WarehouseStorage> storages, WarehouseStatus status) {
        return storages.stream()
                .filter(storage -> storage.getStatus() == status)
                .count();
    }
}
