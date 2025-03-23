package com.fream.back.domain.warehouseStorage.controller.command;

import com.fream.back.domain.warehouseStorage.dto.StorageExtensionRequestDto;
import com.fream.back.domain.warehouseStorage.dto.StorageExtensionResponseDto;
import com.fream.back.domain.warehouseStorage.entity.WarehouseStorage;
import com.fream.back.domain.warehouseStorage.exception.StorageExtensionFailedException;
import com.fream.back.domain.warehouseStorage.exception.WarehouseStorageAccessDeniedException;
import com.fream.back.domain.warehouseStorage.service.command.WarehouseStorageCommandService;
import com.fream.back.domain.warehouseStorage.service.query.WarehouseStorageQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 창고 보관 관련 명령 처리 컨트롤러
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse-storage/commands")
@Slf4j
public class WarehouseStorageCommandController {

    private final WarehouseStorageCommandService warehouseStorageCommandService;
    private final WarehouseStorageQueryService warehouseStorageQueryService;
    /**
     * 창고 보관 기간을 연장합니다.
     *
     * @param requestDto 보관 기간 연장 요청 정보
     * @return 성공 여부 응답
     * @throws WarehouseStorageAccessDeniedException 사용자 정보를 가져올 수 없는 경우
     * @throws StorageExtensionFailedException 기간 연장에 실패한 경우
     */
    @PostMapping("/extend-period")
    public ResponseEntity<StorageExtensionResponseDto> extendStoragePeriod(@RequestBody StorageExtensionRequestDto requestDto) {
        String userEmail = SecurityUtils.extractEmailFromSecurityContext();

        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("창고 보관 기간 연장 시 사용자 이메일을 가져올 수 없습니다.");
            throw new WarehouseStorageAccessDeniedException("사용자 정보를 가져올 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        // 요청 DTO 유효성 검사
        if (requestDto.getStorageId() == null) {
            throw new StorageExtensionFailedException("창고 보관 ID는 필수입니다.");
        }

        if (requestDto.getNewEndDate() == null) {
            throw new StorageExtensionFailedException("새로운 종료일은 필수입니다.");
        }

        if (requestDto.getNewEndDate().isBefore(java.time.LocalDate.now())) {
            throw new StorageExtensionFailedException("종료일은 현재 날짜 이후여야 합니다.");
        }

        log.info("사용자 [{}]가 창고 보관(ID: {})의 기간을 연장합니다. 새 종료일: {}",
                userEmail, requestDto.getStorageId(), requestDto.getNewEndDate());

        // 이전 종료일을 저장하기 위해 연장 전 정보 조회
        WarehouseStorage storage = warehouseStorageQueryService.findStorageById(requestDto.getStorageId());
        LocalDate previousEndDate = storage.getEndDate();

        // 기간 연장 처리
        warehouseStorageCommandService.extendStoragePeriod(
                requestDto.getStorageId(),
                requestDto.getNewEndDate(),
                userEmail
        );

        // 성공 응답 생성
        StorageExtensionResponseDto responseDto = StorageExtensionResponseDto.success(
                requestDto.getStorageId(),
                requestDto.getNewEndDate(),
                previousEndDate
        );

        return ResponseEntity.ok(responseDto);
    }
}