package com.fream.back.domain.warehouseStorage.service.command;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStatus;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.exception.*;
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

    /**
     * 주문에 대한 창고 보관 정보를 생성합니다.
     *
     * @param order 주문 정보
     * @param user 사용자 정보
     * @return 생성된 창고 보관 정보
     * @throws WarehouseStorageProcessingFailedException 창고 보관 생성 처리 중 오류 발생 시
     */
    public WarehouseStorage createOrderStorage(Order order, User user) {
        try {
            // 입력 유효성 검사
            if (order == null) {
                throw new WarehouseStorageProcessingFailedException("주문 정보가 없습니다.");
            }
            if (user == null) {
                throw new WarehouseStorageProcessingFailedException("사용자 정보가 없습니다.");
            }

            // 이미 창고 보관 정보가 있는지 확인
            if (order.getWarehouseStorage() != null) {
                throw new WarehouseStorageProcessingFailedException("주문(ID: " + order.getId() + ")에 이미 창고 보관 정보가 있습니다.");
            }

            WarehouseStorage warehouseStorage = WarehouseStorage.builder()
                    .user(user)
                    .order(order)
                    .storageLocation("Default Location") // 기본 창고 위치
                    .status(WarehouseStatus.IN_STORAGE)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(1)) // 기본 1개월 보관
                    .build();

            return warehouseStorageRepository.save(warehouseStorage);
        } catch (Exception e) {
            if (e instanceof WarehouseStorageException) {
                throw e;
            }
            throw new WarehouseStorageProcessingFailedException("창고 보관 정보 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 판매자의 상품에 대한 창고 보관 정보를 생성합니다.
     *
     * @param sale 판매 정보
     * @param seller 판매자 정보
     * @return 생성된 창고 보관 정보
     * @throws WarehouseStorageProcessingFailedException 창고 보관 생성 처리 중 오류 발생 시
     */
    public WarehouseStorage createSellerStorage(Sale sale, User seller) {
        try {
            // 입력 유효성 검사
            if (sale == null) {
                throw new WarehouseStorageProcessingFailedException("판매 정보가 없습니다.");
            }
            if (seller == null) {
                throw new WarehouseStorageProcessingFailedException("판매자 정보가 없습니다.");
            }

            // 이미 창고 보관 정보가 있는지 확인
            boolean storageExists = warehouseStorageRepository.findBySale(sale).isPresent();
            if (storageExists) {
                throw new WarehouseStorageProcessingFailedException("판매(ID: " + sale.getId() + ")에 이미 창고 보관 정보가 있습니다.");
            }

            WarehouseStorage warehouseStorage = WarehouseStorage.builder()
                    .user(seller)
                    .sale(sale)
                    .storageLocation("Seller's Warehouse") // 판매자 창고 위치 설정
                    .status(WarehouseStatus.IN_STORAGE)
                    .startDate(LocalDate.now())
                    .endDate(LocalDate.now().plusMonths(1)) // 기본 1개월 보관
                    .build();

            return warehouseStorageRepository.save(warehouseStorage);
        } catch (Exception e) {
            if (e instanceof WarehouseStorageException) {
                throw e;
            }
            throw new WarehouseStorageProcessingFailedException("판매자 창고 보관 정보 생성 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 판매와 연관된 창고 보관 상태를 업데이트합니다.
     *
     * @param sale 판매 정보
     * @param newStatus 새로운 상태
     * @throws WarehouseStorageNotFoundException 창고 보관 정보를 찾을 수 없을 경우
     * @throws InvalidStatusTransitionException 유효하지 않은 상태 전환일 경우
     */
    @Transactional
    public void updateWarehouseStatus(Sale sale, WarehouseStatus newStatus) {
        try {
            // 입력 유효성 검사
            if (sale == null) {
                throw new WarehouseStorageProcessingFailedException("판매 정보가 없습니다.");
            }
            if (newStatus == null) {
                throw new InvalidWarehouseStatusException("새로운 상태 정보가 없습니다.");
            }

            WarehouseStorage storage = warehouseStorageQueryService.findBySale(sale);

            // 상태 전환 유효성 검사
            validateStatusTransition(storage.getStatus(), newStatus);

            storage.updateStatus(newStatus);
            warehouseStorageRepository.save(storage);
        } catch (Exception e) {
            if (e instanceof WarehouseStorageException) {
                throw e;
            }
            throw new WarehouseStorageProcessingFailedException("창고 보관 상태 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 판매와 연관된 창고 보관 상태를 판매 완료로 업데이트합니다.
     *
     * @param sale 판매 정보
     * @throws WarehouseStorageNotFoundException 창고 보관 정보를 찾을 수 없을 경우
     * @throws InvalidStatusTransitionException 유효하지 않은 상태 전환일 경우
     */
    @Transactional
    public void updateWarehouseStatusToSold(Sale sale) {
        if (sale == null) {
            throw new WarehouseStorageProcessingFailedException("판매 정보가 없습니다.");
        }

        if (sale.isWarehouseStorage()) {
            try {
                WarehouseStorage storage = warehouseStorageQueryService.findBySale(sale);

                // 이미 판매된 상품인지 확인
                if (storage.getStatus() == WarehouseStatus.SOLD) {
                    throw new InvalidStatusTransitionException("이미 판매 완료 상태인 상품입니다(ID: " + sale.getId() + ")");
                }

                // 상태 전환 유효성 검사
                validateStatusTransition(storage.getStatus(), WarehouseStatus.SOLD);

                storage.updateStatus(WarehouseStatus.SOLD);
                warehouseStorageRepository.save(storage);
            } catch (Exception e) {
                if (e instanceof WarehouseStorageException) {
                    throw e;
                }
                throw new WarehouseStorageProcessingFailedException("창고 보관 상태를 판매 완료로 업데이트 중 오류가 발생했습니다: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 창고 보관 기간을 연장합니다.
     *
     * @param storageId 창고 보관 ID
     * @param newEndDate 새로운 종료일
     * @param userEmail 요청자 이메일
     * @throws WarehouseStorageNotFoundException 창고 보관 정보를 찾을 수 없을 경우
     * @throws WarehouseStorageAccessDeniedException 접근 권한이 없을 경우
     * @throws StorageExtensionFailedException 기간 연장에 실패한 경우
     */
    @Transactional
    public void extendStoragePeriod(Long storageId, LocalDate newEndDate, String userEmail) {
        if (storageId == null) {
            throw new WarehouseStorageProcessingFailedException("창고 보관 ID가 없습니다.");
        }
        if (newEndDate == null) {
            throw new StorageExtensionFailedException("새로운 종료일이 없습니다.");
        }
        if (newEndDate.isBefore(LocalDate.now())) {
            throw new StorageExtensionFailedException("종료일은 현재 날짜보다 이후여야 합니다.");
        }

        try {
            WarehouseStorage storage = warehouseStorageRepository.findById(storageId)
                    .orElseThrow(() -> new WarehouseStorageNotFoundException("창고 보관 정보를 찾을 수 없습니다(ID: " + storageId + ")"));

            // 접근 권한 확인
            if (!storage.getUser().getEmail().equals(userEmail)) {
                throw new WarehouseStorageAccessDeniedException("해당 창고 보관 정보에 대한 접근 권한이 없습니다.");
            }

            // 이미 판매 완료되었거나 창고에서 제거된 경우 연장 불가
            if (storage.getStatus() == WarehouseStatus.SOLD || storage.getStatus() == WarehouseStatus.REMOVED_FROM_STORAGE) {
                throw new StorageExtensionFailedException("이미 판매 완료되었거나 창고에서 제거된 상품은 보관 기간을 연장할 수 없습니다.");
            }

            storage.updateEndDate(newEndDate);
            warehouseStorageRepository.save(storage);
        } catch (Exception e) {
            if (e instanceof WarehouseStorageException) {
                throw e;
            }
            throw new StorageExtensionFailedException("창고 보관 기간 연장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }


    /**
     * 상태 전환의 유효성을 검사합니다.
     *
     * @param currentStatus 현재 상태
     * @param newStatus 새로운 상태
     * @throws InvalidStatusTransitionException 유효하지 않은 상태 전환일 경우
     */
    private void validateStatusTransition(WarehouseStatus currentStatus, WarehouseStatus newStatus) {
        // 같은 상태로의 전환은 허용
        if (currentStatus == newStatus) {
            return;
        }

        // 상태별 유효한 전환 확인
        switch (currentStatus) {
            case IN_STORAGE:
                // IN_STORAGE에서는 모든 상태로 전환 가능
                break;
            case ASSOCIATED_WITH_ORDER:
                if (newStatus == WarehouseStatus.IN_STORAGE) {
                    throw new InvalidStatusTransitionException(
                            "주문과 연결된 상태(ASSOCIATED_WITH_ORDER)에서 보관 상태(IN_STORAGE)로 직접 전환할 수 없습니다.");
                }
                break;
            case ON_AUCTION:
                if (newStatus != WarehouseStatus.IN_STORAGE && newStatus != WarehouseStatus.SOLD) {
                    throw new InvalidStatusTransitionException(
                            "경매 중인 상태(ON_AUCTION)에서는 보관 상태(IN_STORAGE) 또는 판매됨(SOLD) 상태로만 전환할 수 있습니다.");
                }
                break;
            case SOLD:
                throw new InvalidStatusTransitionException("판매된 상태(SOLD)에서는 다른 상태로 전환할 수 없습니다.");
            case REMOVED_FROM_STORAGE:
                throw new InvalidStatusTransitionException("창고에서 제거된 상태(REMOVED_FROM_STORAGE)에서는 다른 상태로 전환할 수 없습니다.");
        }
    }
}