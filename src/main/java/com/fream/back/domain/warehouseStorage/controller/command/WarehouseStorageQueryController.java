package com.fream.back.domain.warehouseStorage.controller.command;

import com.fream.back.domain.warehouseStorage.dto.WarehouseStatusCountDto;
import com.fream.back.domain.warehouseStorage.service.query.WarehouseStorageQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/warehouse-storage/queries")
public class WarehouseStorageQueryController {

    private final WarehouseStorageQueryService warehouseStorageQueryService;

    @GetMapping("/status-count")
    public ResponseEntity<WarehouseStatusCountDto> getWarehouseStatusCount() {
        String userEmail = SecurityUtils.extractEmailFromSecurityContext();
        WarehouseStatusCountDto countDto = warehouseStorageQueryService.getWarehouseStatusCount(userEmail);
        return ResponseEntity.ok(countDto);
    }
}