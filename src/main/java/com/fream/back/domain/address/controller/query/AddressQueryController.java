package com.fream.back.domain.address.controller.query;

import com.fream.back.domain.address.dto.AddressListResponseDto;
import com.fream.back.domain.address.dto.AddressResponseDto;
import com.fream.back.domain.address.exception.AddressErrorCode;
import com.fream.back.domain.address.exception.AddressException;
import com.fream.back.domain.address.service.query.AddressQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressQueryController {

    private final AddressQueryService addressQueryService;

    /**
     * 주소 목록 조회
     * @return 사용자의 모든 주소 목록
     */
    @GetMapping
    public ResponseEntity<AddressListResponseDto> getAddresses() {
        try {
            String email = SecurityUtils.extractEmailFromSecurityContext();
            log.info("주소 목록 조회 API 요청: 사용자={}", email);

            List<AddressResponseDto> addresses = addressQueryService.getAddresses(email);

            log.info("주소 목록 조회 API 응답: 사용자={}, 주소 수={}", email, addresses.size());
            return ResponseEntity.ok(new AddressListResponseDto(addresses));
        } catch (Exception e) {
            // 예외는 GlobalExceptionHandler에서 처리되므로 여기서는 로깅만 수행
            log.error("주소 목록 조회 API 오류: {}", e.getMessage(), e);
            throw e; // 그대로 던져서 GlobalExceptionHandler가 처리하도록 함
        }
    }

    /**
     * 특정 주소 조회
     * @param addressId 조회할 주소 ID
     * @return 해당 ID의 주소 정보
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponseDto> getAddress(@PathVariable("addressId") Long addressId) {
        try {
            // 입력 유효성 검증
            if (addressId == null || addressId <= 0) {
                throw new AddressException(
                        AddressErrorCode.ADDRESS_INVALID_DATA,
                        "유효하지 않은 주소 ID입니다."
                );
            }

            String email = SecurityUtils.extractEmailFromSecurityContext();
            log.info("단일 주소 조회 API 요청: 사용자={}, 주소ID={}", email, addressId);

            AddressResponseDto address = addressQueryService.getAddress(email, addressId);

            log.info("단일 주소 조회 API 응답: 사용자={}, 주소ID={}, 상태=성공", email, addressId);
            return ResponseEntity.ok(address);
        } catch (Exception e) {
            // 예외는 GlobalExceptionHandler에서 처리되므로 여기서는 로깅만 수행
            log.error("단일 주소 조회 API 오류: 주소ID={}, {}", addressId, e.getMessage(), e);
            throw e; // 그대로 던져서 GlobalExceptionHandler가 처리하도록 함
        }
    }
}