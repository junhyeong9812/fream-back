package com.fream.back.domain.warehouseStorage.service.query;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.warehouseStorage.dto.WarehouseStatusCountDto;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStatus;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.exception.WarehouseStorageNotFoundException;
import com.fream.back.domain.warehouseStorage.exception.WarehouseStorageAccessDeniedException;
import com.fream.back.domain.warehouseStorage.repository.WarehouseStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseStorageQueryService {

    private final WarehouseStorageRepository warehouseStorageRepository;

    /**
     * 판매 정보로 창고 보관 정보를 조회합니다.
     *
     * @param sale 판매 정보
     * @return 창고 보관 정보
     * @throws WarehouseStorageNotFoundException 창고 보관 정보를 찾을 수 없을 경우
     */
    @Transactional(readOnly = true)
    public WarehouseStorage findBySale(Sale sale) {
        return warehouseStorageRepository.findBySale(sale)
                .orElseThrow(() -> new WarehouseStorageNotFoundException("해당 Sale(ID: " + sale.getId() + ")에 연결된 창고 정보를 찾을 수 없습니다."));
    }

    /**
     * 사용자 이메일로 창고 보관 상태별 개수를 조회합니다.
     *
     * @param userEmail 사용자 이메일
     * @return 창고 보관 상태별 개수 정보
     */
    @Transactional(readOnly = true)
    public WarehouseStatusCountDto getWarehouseStatusCount(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) {
            throw new WarehouseStorageAccessDeniedException("사용자 이메일 정보가 없습니다.");
        }

        List<WarehouseStorage> storages = warehouseStorageRepository.findByUser_Email(userEmail);

        return WarehouseStatusCountDto.builder()
                .inStorageCount(countByStatus(storages, WarehouseStatus.IN_STORAGE))
                .associatedWithOrderCount(countByStatus(storages, WarehouseStatus.ASSOCIATED_WITH_ORDER))
                .removedFromStorageCount(countByStatus(storages, WarehouseStatus.REMOVED_FROM_STORAGE))
                .onAuctionCount(countByStatus(storages, WarehouseStatus.ON_AUCTION))
                .soldCount(countByStatus(storages, WarehouseStatus.SOLD))
                .build();
    }

    /**
     * 창고 보관 목록에서 특정 상태에 해당하는 항목 수를 계산합니다.
     *
     * @param storages 창고 보관 목록
     * @param status 확인할 상태
     * @return 해당 상태의 항목 수
     */
    private long countByStatus(List<WarehouseStorage> storages, WarehouseStatus status) {
        return storages.stream()
                .filter(storage -> storage.getStatus() == status)
                .count();
    }

    /**
     * ID로 창고 보관 정보를 조회합니다.
     *
     * @param storageId 창고 보관 ID
     * @return 창고 보관 정보
     * @throws WarehouseStorageNotFoundException 창고 보관 정보를 찾을 수 없을 경우
     */
    @Transactional(readOnly = true)
    public WarehouseStorage findStorageById(Long storageId) {
        if (storageId == null) {
            throw new WarehouseStorageNotFoundException("창고 보관 ID가 없습니다.");
        }

        return warehouseStorageRepository.findById(storageId)
                .orElseThrow(() -> new WarehouseStorageNotFoundException("창고 보관 정보를 찾을 수 없습니다(ID: " + storageId + ")"));
    }
}