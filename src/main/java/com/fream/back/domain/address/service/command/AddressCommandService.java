package com.fream.back.domain.address.service.command;

import com.fream.back.domain.address.dto.AddressCreateDto;
import com.fream.back.domain.address.dto.AddressUpdateDto;
import com.fream.back.domain.address.entity.Address;
import com.fream.back.domain.address.repository.AddressRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressCommandService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    //주소지 생성
    @Transactional
    public void createAddress(String email, AddressCreateDto createDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        if (createDto.getIsDefault() != null && createDto.getIsDefault()) {
            // 기존 기본 주소 해제
            user.getAddresses().forEach(address -> address.updateAddress(
                    null, null, null, null, null, false));
        }

        Address newAddress = Address.builder()
                .user(user)
                .recipientName(createDto.getRecipientName())
                .phoneNumber(createDto.getPhoneNumber())
                .zipCode(createDto.getZipCode())
                .address(createDto.getAddress())
                .detailedAddress(createDto.getDetailedAddress())
                .isDefault(createDto.getIsDefault() != null && createDto.getIsDefault())
                .build();

        addressRepository.save(newAddress);
    }

    //주소지 변경
    @Transactional
    public void updateAddress(String email, AddressUpdateDto updateDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Address address = addressRepository.findByIdAndUser(updateDto.getAddressId(), user)
                .orElseThrow(() -> new IllegalArgumentException("해당 주소록을 찾을 수 없습니다."));

        if (updateDto.getIsDefault() != null && updateDto.getIsDefault()) {
            user.getAddresses().forEach(addr -> addr.updateAddress(
                    null, null, null, null, null, false));
        }

        address.updateAddress(
                updateDto.getRecipientName(),
                updateDto.getPhoneNumber(),
                updateDto.getZipCode(),
                updateDto.getAddress(),
                updateDto.getDetailedAddress(),
                updateDto.getIsDefault()
        );
    }

    //주소지 삭제
    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다."));

        Address address = addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new IllegalArgumentException("해당 주소록을 찾을 수 없습니다."));

        addressRepository.delete(address);
    }
}
