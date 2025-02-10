package com.fream.back.domain.address.controller.command;

import com.fream.back.domain.address.dto.AddressCreateDto;
import com.fream.back.domain.address.dto.AddressUpdateDto;
import com.fream.back.domain.address.service.command.AddressCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressCommandController {

    private final AddressCommandService addressCommandService;

    /**
     * 주소 생성
     */
    @PostMapping
    public ResponseEntity<String> createAddress(@RequestBody @Validated AddressCreateDto createDto) {
        // SecurityUtils 활용
        String email = SecurityUtils.extractEmailFromSecurityContext();
        addressCommandService.createAddress(email, createDto);

        return ResponseEntity.ok("주소록 생성이 완료되었습니다.");
    }

    /**
     * 주소 수정
     */
    @PutMapping
    public ResponseEntity<String> updateAddress(@RequestBody @Validated AddressUpdateDto updateDto) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        addressCommandService.updateAddress(email, updateDto);
        return ResponseEntity.ok("주소록 수정이 완료되었습니다.");
    }

    /**
     * 주소 삭제
     */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> deleteAddress(@PathVariable("addressId") Long addressId) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        addressCommandService.deleteAddress(email, addressId);
        return ResponseEntity.ok("주소록 삭제가 완료되었습니다.");
    }
}
