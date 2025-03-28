package com.fream.back.domain.warehouseStorage.entity;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStorage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    private Order order; // 구매와 연결된 정보 (nullable)

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = true)
    private Sale sale; // 판매와 연결된 정보 (nullable)

    private String storageLocation; // 창고 위치 정보

    private LocalDate startDate; // 보관 시작 날짜
    private LocalDate endDate; // 보관 종료 날짜

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status; // 보관 상태

    private boolean isLinkedToOrder; // 구매와 연결된 보관 여부

    // ======= 상태 및 관계 관리 메서드 =======
    public void assignOrder(Order order) {
        this.order = order;
        this.sale = null; // 배타적 관계 설정
        this.isLinkedToOrder = true;
        this.status = WarehouseStatus.ASSOCIATED_WITH_ORDER;
    }

    public void assignSale(Sale sale) {
        this.sale = sale;
        this.order = null; // 배타적 관계 설정
        this.isLinkedToOrder = false;
        this.status = WarehouseStatus.IN_STORAGE;
    }

    public void markAsReturned() {
        this.status = WarehouseStatus.REMOVED_FROM_STORAGE;
    }

    public void updateStatus(WarehouseStatus newStatus) {
        this.status = newStatus;
    }

    public void setStorageDates(LocalDate startDate, int initialPeriodDays) {
        this.startDate = startDate;
        this.endDate = startDate.plusDays(initialPeriodDays);
    }

    public void extendStorage(int additionalDays) {
        this.endDate = this.endDate.plusDays(additionalDays);
    }

    /**
     * 보관 종료일을 업데이트합니다.
     *
     * @param newEndDate 새로운 종료일
     */
    public void updateEndDate(LocalDate newEndDate) {
        if (newEndDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("종료일은 현재 날짜보다 이후여야 합니다.");
        }
        this.endDate = newEndDate;
    }

    /**
     * 현재 보관 남은 일수를 계산합니다.
     *
     * @return 남은 보관 일수 (이미 종료된 경우 0 반환)
     */
    public int getRemainingDays() {
        LocalDate today = LocalDate.now();
        if (endDate.isBefore(today)) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(today, endDate);
    }

    /**
     * 보관 기간이 만료되었는지 확인합니다.
     *
     * @return 만료 여부
     */
    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate);
    }

    /**
     * 보관 기간 총 일수를 계산합니다.
     *
     * @return 총 보관 일수
     */
    public int getTotalStorageDays() {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
    }
}