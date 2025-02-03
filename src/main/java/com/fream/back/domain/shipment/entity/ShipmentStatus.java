package com.fream.back.domain.shipment.entity;

public enum ShipmentStatus {
    PENDING {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == SHIPPED || nextStatus == CANCELED;
        }
    },
    SHIPPED {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == IN_TRANSIT || nextStatus == RETURNED || nextStatus == CANCELED;
        }
    },
    IN_TRANSIT {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == OUT_FOR_DELIVERY || nextStatus == DELAYED || nextStatus == CANCELED
                    || nextStatus == DELIVERED;
        }
    },
    OUT_FOR_DELIVERY {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == DELIVERED || nextStatus == FAILED_DELIVERY || nextStatus == CANCELED;
        }
    },
    DELIVERED {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return false; // 배송 완료 이후로는 상태 전환 없음
        }
    },
    RETURNED {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == CANCELED;
        }
    },
    CANCELED {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return false; // 취소 이후 상태 전환 없음
        }
    },
    DELAYED {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == IN_TRANSIT || nextStatus == CANCELED;
        }
    },
    FAILED_DELIVERY {
        @Override
        public boolean canTransitionTo(ShipmentStatus nextStatus) {
            return nextStatus == RETURNED || nextStatus == OUT_FOR_DELIVERY || nextStatus == CANCELED;
        }
    };

    public abstract boolean canTransitionTo(ShipmentStatus nextStatus);
}
