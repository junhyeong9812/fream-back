package com.fream.back.domain.shipment.service.command;

import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleStatus;
import com.fream.back.domain.sale.repository.SaleRepository;
import com.fream.back.domain.shipment.entity.SellerShipment;
import com.fream.back.domain.shipment.entity.ShipmentStatus;
import com.fream.back.domain.shipment.exception.ShipmentErrorCode;
import com.fream.back.domain.shipment.exception.ShipmentException;
import com.fream.back.domain.shipment.repository.SellerShipmentRepository;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 판매자 배송 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerShipmentCommandService {

    private final SellerShipmentRepository sellerShipmentRepository;
    private final SaleRepository saleRepository;
    private final WarehouseStorageCommandService warehouseStorageCommandService;

    /**
     * 판매에 대한 판매자 배송 정보를 생성합니다.
     *
     * @param saleId 판매 ID
     * @param courier 택배사 이름
     * @param trackingNumber 운송장 번호
     * @return 생성된 판매자 배송 정보
     * @throws ShipmentException 판매 정보를 찾을 수 없는 경우 또는 배송 정보 생성 실패 시
     */
    @Transactional
    public SellerShipment createSellerShipment(Long saleId, String courier, String trackingNumber) {
        log.info("판매자 배송 정보 생성 시작: saleId={}, courier={}, trackingNumber={}",
                saleId, courier, trackingNumber);

        if (courier == null || courier.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            log.warn("유효하지 않은 배송사 또는 운송장 번호: saleId={}", saleId);
            throw new ShipmentException(ShipmentErrorCode.TRACKING_INFO_REQUIRED);
        }

        try {
            // Sale 조회
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new ShipmentException(
                            ShipmentErrorCode.SALE_NOT_FOUND_FOR_SHIPMENT,
                            "해당 Sale을 찾을 수 없습니다: " + saleId
                    ));

            log.debug("판매 정보 조회: saleId={}, sellerId={}, currentStatus={}",
                    sale.getId(), sale.getSeller().getId(), sale.getStatus());

            // SellerShipment 생성
            SellerShipment shipment = SellerShipment.builder()
                    .sale(sale)
                    .courier(courier)
                    .trackingNumber(trackingNumber)
                    .status(ShipmentStatus.IN_TRANSIT) // 배송 중 상태로 설정
                    .build();

            // 연관관계 설정
            sale.assignSellerShipment(shipment);

            SellerShipment savedShipment = sellerShipmentRepository.save(shipment);
            log.info("판매자 배송 정보 저장 완료: shipmentId={}, saleId={}",
                    savedShipment.getId(), saleId);

            // 창고 보관 상품인 경우 추가 처리
            if (sale.isWarehouseStorage()) {
                log.info("창고 보관 상품 처리: saleId={}", saleId);

                // 창고 보관 데이터 생성
                warehouseStorageCommandService.createSellerStorage(sale, sale.getSeller());

                // 창고 보관 상태 업데이트
                sale.updateStatus(SaleStatus.IN_STORAGE);
                log.info("판매 상태 IN_STORAGE로 변경: saleId={}", saleId);
            }

            return savedShipment;

        } catch (ShipmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("판매자 배송 정보 생성 실패: saleId={}, error={}", saleId, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.SALE_NOT_FOUND_FOR_SHIPMENT,
                    "판매자 배송 정보 생성 중 오류 발생: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 판매자 배송 정보를 업데이트합니다.
     *
     * @param shipmentId 배송 ID
     * @param courier 택배사 이름
     * @param trackingNumber 운송장 번호
     * @return 업데이트된 판매자 배송 정보
     * @throws ShipmentException 배송 정보를 찾을 수 없는 경우 또는 업데이트 실패 시
     */
    @Transactional
    public SellerShipment updateShipment(Long shipmentId, String courier, String trackingNumber) {
        log.info("판매자 배송 정보 업데이트: shipmentId={}, courier={}, trackingNumber={}",
                shipmentId, courier, trackingNumber);

        if (courier == null || courier.isBlank() || trackingNumber == null || trackingNumber.isBlank()) {
            log.warn("유효하지 않은 배송사 또는 운송장 번호: shipmentId={}", shipmentId);
            throw new ShipmentException(ShipmentErrorCode.TRACKING_INFO_REQUIRED);
        }

        try {
            SellerShipment sellerShipment = sellerShipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ShipmentException(
                            ShipmentErrorCode.SELLER_SHIPMENT_NOT_FOUND,
                            "해당 Shipment를 찾을 수 없습니다: " + shipmentId
                    ));

            log.debug("판매자 배송 정보 조회: shipmentId={}, saleId={}, currentStatus={}",
                    sellerShipment.getId(),
                    sellerShipment.getSale().getId(),
                    sellerShipment.getStatus());

            sellerShipment.updateTrackingInfo(courier, trackingNumber);
            log.info("판매자 배송 정보 업데이트 완료: shipmentId={}", shipmentId);

            return sellerShipment;

        } catch (ShipmentException e) {
            throw e;
        } catch (Exception e) {
            log.error("판매자 배송 정보 업데이트 실패: shipmentId={}, error={}",
                    shipmentId, e.getMessage(), e);
            throw new ShipmentException(
                    ShipmentErrorCode.SELLER_SHIPMENT_NOT_FOUND,
                    "판매자 배송 정보 업데이트 중 오류 발생: " + e.getMessage(),
                    e
            );
        }
    }
}