package com.fream.back.domain.address.controller.query;

import com.fream.back.domain.address.dto.AddressListResponseDto;
import com.fream.back.domain.address.dto.AddressResponseDto;
import com.fream.back.domain.address.service.query.AddressQueryService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressQueryController {

    private final AddressQueryService addressQueryService;

    /**
     * 주소 목록 조회
     */
    @GetMapping
    public ResponseEntity<AddressListResponseDto> getAddresses() {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        List<AddressResponseDto> addresses = addressQueryService.getAddresses(email);
        return ResponseEntity.ok(new AddressListResponseDto(addresses));
    }

    /**
     * 특정 주소 조회
     */
    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponseDto> getAddress(@PathVariable("addressId") Long addressId) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        AddressResponseDto address = addressQueryService.getAddress(email, addressId);
        return ResponseEntity.ok(address);
    }
}
