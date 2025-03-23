package com.fream.back.domain.warehouseStorage.controller.query;

import com.fream.back.domain.warehouseStorage.dto.WarehouseStatusCountDto;
import com.fream.back.domain.warehouseStorage.exception.WarehouseStorageAccessDeniedException;
import com.fream.back.domain.warehouseStorage.service.query.WarehouseStorageQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse-storage/queries")
@Slf4j
public class WarehouseStorageQueryController {

    private final WarehouseStorageQueryService warehouseStorageQueryService;

    /**
     * 현재 로그인한 사용자의 창고 보관 상태별 개수를 조회합니다.
     *
     * @return 창고 보관 상태별 개수 정보
     * @throws WarehouseStorageAccessDeniedException 사용자 정보를 가져올 수 없는 경우
     */
    @GetMapping("/status-count")
    public ResponseEntity<WarehouseStatusCountDto> getWarehouseStatusCount() {
        String userEmail = SecurityUtils.extractEmailFromSecurityContext();

        if (userEmail == null || userEmail.isEmpty()) {
            log.warn("창고 보관 상태 조회 시 사용자 이메일을 가져올 수 없습니다.");
            throw new WarehouseStorageAccessDeniedException("사용자 정보를 가져올 수 없습니다. 로그인 상태를 확인해주세요.");
        }

        log.info("사용자 [{}]의 창고 보관 상태 개수를 조회합니다.", userEmail);
        WarehouseStatusCountDto countDto = warehouseStorageQueryService.getWarehouseStatusCount(userEmail);

        return ResponseEntity.ok(countDto);
    }
}