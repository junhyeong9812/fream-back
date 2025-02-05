package com.fream.back.domain.warehouseStorage.service.query;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.repository.WarehouseStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseStorageQueryService {

    private final WarehouseStorageRepository warehouseStorageRepository;

    @Transactional(readOnly = true)
    public WarehouseStorage findBySale(Sale sale) {
        return warehouseStorageRepository.findBySale(sale)
                .orElseThrow(() -> new IllegalArgumentException("해당 Sale에 연결된 창고 정보를 찾을 수 없습니다."));
    }
}
