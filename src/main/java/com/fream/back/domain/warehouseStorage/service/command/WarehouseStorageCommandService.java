package com.fream.back.domain.warehouseStorage.service.command;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStatus;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.repository.WarehouseStorageRepository;
import com.fream.back.domain.warehouseStorage.service.query.WarehouseStorageQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseStorageCommandService {

    private final WarehouseStorageRepository warehouseStorageRepository;
    private final WarehouseStorageQueryService warehouseStorageQueryService;

    public WarehouseStorage createOrderStorage(Order order, User user) {
        WarehouseStorage warehouseStorage = WarehouseStorage.builder()
                .user(user)
                .order(order)
                .storageLocation("Default Location") // 기본 창고 위치
                .status(WarehouseStatus.IN_STORAGE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1)) // 기본 1개월 보관
                .build();

        return warehouseStorageRepository.save(warehouseStorage);
    }
    public WarehouseStorage createSellerStorage(Sale sale, User seller) {
        WarehouseStorage warehouseStorage = WarehouseStorage.builder()
                .user(seller)
                .sale(sale)
                .storageLocation("Seller's Warehouse") // 판매자 창고 위치 설정
                .status(WarehouseStatus.IN_STORAGE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1)) // 기본 1개월 보관
                .build();

        return warehouseStorageRepository.save(warehouseStorage);
    }
    @Transactional
    public void updateWarehouseStatus(Sale sale, WarehouseStatus newStatus) {
        WarehouseStorage storage = warehouseStorageQueryService.findBySale(sale);
        storage.updateStatus(newStatus);
        warehouseStorageRepository.save(storage);
    }
    @Transactional
    public void updateWarehouseStatusToSold(Sale sale) {
        if (sale.isWarehouseStorage()) {
            WarehouseStorage storage = warehouseStorageQueryService.findBySale(sale);
            storage.updateStatus(WarehouseStatus.SOLD);
            warehouseStorageRepository.save(storage);
        }
    }

}
