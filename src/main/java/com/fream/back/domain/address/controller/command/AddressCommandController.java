package com.fream.back.domain.address.controller.command;

import com.fream.back.domain.address.dto.AddressCreateDto;
import com.fream.back.domain.address.dto.AddressUpdateDto;
import com.fream.back.domain.address.exception.AddressErrorCode;
import com.fream.back.domain.address.exception.AddressException;
import com.fream.back.domain.address.service.command.AddressCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 주소 생성, 수정, 삭제 등을 담당하는
 * Command 전용 컨트롤러
 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressCommandController {

    private final AddressCommandService addressCommandService;

    /**
     * 주소 생성
     * @param createDto 주소 생성 정보
     * @return 생성 결과 메시지
     */
    @PostMapping
    public ResponseEntity<String> createAddress(@RequestBody @Validated AddressCreateDto createDto) {
        try {
            // SecurityUtils 활용
            String email = SecurityUtils.extractEmailFromSecurityContext();
            log.info("주소 생성 API 요청: 사용자={}", email);

            addressCommandService.createAddress(email, createDto);

            log.info("주소 생성 API 응답: 사용자={}, 상태=성공", email);
            return ResponseEntity.ok("주소록 생성이 완료되었습니다.");
        } catch (Exception e) {
            // 예외는 GlobalExceptionHandler에서 처리되므로 여기서는 로깅만 수행
            log.error("주소 생성 API 오류: {}", e.getMessage(), e);
            throw e; // 그대로 던져서 GlobalExceptionHandler가 처리하도록 함
        }
    }

    /**
     * 주소 수정
     * @param updateDto 주소 수정 정보
     * @return 수정 결과 메시지
     */
    @PutMapping
    public ResponseEntity<String> updateAddress(@RequestBody @Validated AddressUpdateDto updateDto) {
        try {
            String email = SecurityUtils.extractEmailFromSecurityContext();
            log.info("주소 수정 API 요청: 사용자={}, 주소ID={}", email, updateDto.getAddressId());

            // 입력 유효성 검증 - ID 필수
            if (updateDto.getAddressId() == null) {
                throw new AddressException(
                        AddressErrorCode.ADDRESS_INVALID_DATA,
                        "주소 ID는 필수 입력사항입니다."
                );
            }

            addressCommandService.updateAddress(email, updateDto);

            log.info("주소 수정 API 응답: 사용자={}, 주소ID={}, 상태=성공", email, updateDto.getAddressId());
            return ResponseEntity.ok("주소록 수정이 완료되었습니다.");
        } catch (Exception e) {
            // 예외는 GlobalExceptionHandler에서 처리되므로 여기서는 로깅만 수행
            log.error("주소 수정 API 오류: {}", e.getMessage(), e);
            throw e; // 그대로 던져서 GlobalExceptionHandler가 처리하도록 함
        }
    }

    /**
     * 주소 삭제
     * @param addressId 삭제할 주소 ID
     * @return 삭제 결과 메시지
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable("addressId") Long addressId) {
        try {
            // 입력 유효성 검증
            if (addressId == null || addressId <= 0) {
                throw new AddressException(
                        AddressErrorCode.ADDRESS_INVALID_DATA,
                        "유효하지 않은 주소 ID입니다."
                );
            }

            String email = SecurityUtils.extractEmailFromSecurityContext();
            log.info("주소 삭제 API 요청: 사용자={}, 주소ID={}", email, addressId);

            addressCommandService.deleteAddress(email, addressId);

            log.info("주소 삭제 API 응답: 사용자={}, 주소ID={}, 상태=성공", email, addressId);
            return ResponseEntity.ok("주소록 삭제가 완료되었습니다.");
        } catch (Exception e) {
            // 예외는 GlobalExceptionHandler에서 처리되므로 여기서는 로깅만 수행
            log.error("주소 삭제 API 오류: 주소ID={}, {}", addressId, e.getMessage(), e);
            throw e; // 그대로 던져서 GlobalExceptionHandler가 처리하도록 함
        }
    }
}