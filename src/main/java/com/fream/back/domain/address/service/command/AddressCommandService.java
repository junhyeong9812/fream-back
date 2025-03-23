package com.fream.back.domain.address.service.command;

import com.fream.back.domain.address.dto.AddressCreateDto;
import com.fream.back.domain.address.dto.AddressUpdateDto;
import com.fream.back.domain.address.entity.Address;
import com.fream.back.domain.address.exception.AddressErrorCode;
import com.fream.back.domain.address.exception.AddressException;
import com.fream.back.domain.address.exception.AddressNotFoundException;
import com.fream.back.domain.address.exception.AddressUserNotFoundException;
import com.fream.back.domain.address.repository.AddressRepository;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressCommandService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    //주소지 생성
    @Transactional
    public void createAddress(String email, AddressCreateDto createDto) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AddressUserNotFoundException(
                            "이메일 '" + email + "'에 해당하는 사용자를 찾을 수 없습니다."
                    ));

            log.info("주소 생성 시작: 사용자={}, 수신자={}", email, createDto.getRecipientName());

            if (createDto.getIsDefault() != null && createDto.getIsDefault()) {
                // 기존 기본 주소 해제
                log.debug("기존 기본 주소 해제: 사용자={}", email);
                user.getAddresses().forEach(address -> address.updateAddress(
                        null, null, null, null, null, false));
            }

            // 입력값 검증
            validateCreateDto(createDto);

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
            log.info("주소 생성 완료: 사용자={}, 주소ID={}", email, newAddress.getId());
        } catch (AddressException e) {
            throw e; // 이미 AddressException이면 그대로 전파
        } catch (Exception e) {
            log.error("주소 생성 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new AddressException(AddressErrorCode.ADDRESS_CREATE_ERROR, e);
        }
    }

    //주소지 변경
    @Transactional
    public void updateAddress(String email, AddressUpdateDto updateDto) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AddressUserNotFoundException(
                            "이메일 '" + email + "'에 해당하는 사용자를 찾을 수 없습니다."
                    ));

            log.info("주소 수정 시작: 사용자={}, 주소ID={}", email, updateDto.getAddressId());

            Address address = addressRepository.findByIdAndUser(updateDto.getAddressId(), user)
                    .orElseThrow(() -> new AddressNotFoundException(
                            "주소 ID '" + updateDto.getAddressId() + "'에 해당하는 주소를 찾을 수 없습니다."
                    ));

            // 입력값 검증
            validateUpdateDto(updateDto);

            if (updateDto.getIsDefault() != null && updateDto.getIsDefault()) {
                log.debug("기존 기본 주소 해제: 사용자={}", email);
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

            log.info("주소 수정 완료: 사용자={}, 주소ID={}", email, updateDto.getAddressId());
        } catch (AddressException e) {
            throw e; // 이미 AddressException이면 그대로 전파
        } catch (Exception e) {
            log.error("주소 수정 중 오류 발생: 사용자={}, 주소ID={}, 오류={}",
                    email, updateDto.getAddressId(), e.getMessage(), e);
            throw new AddressException(AddressErrorCode.ADDRESS_UPDATE_ERROR, e);
        }
    }

    //주소지 삭제
    @Transactional
    public void deleteAddress(String email, Long addressId) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AddressUserNotFoundException(
                            "이메일 '" + email + "'에 해당하는 사용자를 찾을 수 없습니다."
                    ));

            log.info("주소 삭제 시작: 사용자={}, 주소ID={}", email, addressId);

            Address address = addressRepository.findByIdAndUser(addressId, user)
                    .orElseThrow(() -> new AddressNotFoundException(
                            "주소 ID '" + addressId + "'에 해당하는 주소를 찾을 수 없습니다."
                    ));

            addressRepository.delete(address);
            log.info("주소 삭제 완료: 사용자={}, 주소ID={}", email, addressId);
        } catch (AddressException e) {
            throw e; // 이미 AddressException이면 그대로 전파
        } catch (Exception e) {
            log.error("주소 삭제 중 오류 발생: 사용자={}, 주소ID={}, 오류={}",
                    email, addressId, e.getMessage(), e);
            throw new AddressException(AddressErrorCode.ADDRESS_DELETE_ERROR, e);
        }
    }

    // 주소 생성 데이터 유효성 검증
    private void validateCreateDto(AddressCreateDto createDto) {
        if (createDto.getRecipientName() == null || createDto.getRecipientName().isBlank()) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_DATA,
                    "수령인 이름은 필수 입력사항입니다."
            );
        }

        if (createDto.getPhoneNumber() == null || createDto.getPhoneNumber().isBlank()) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_DATA,
                    "연락처는 필수 입력사항입니다."
            );
        }

        // 전화번호 형식 검증 (간단한 예시, 필요에 따라 정교한 정규식으로 대체 가능)
        if (!createDto.getPhoneNumber().matches("\\d{10,11}")) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_PHONE_NUMBER,
                    "전화번호 형식이 올바르지 않습니다. 숫자 10-11자리로 입력해주세요."
            );
        }

        if (createDto.getZipCode() == null || createDto.getZipCode().isBlank()) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_DATA,
                    "우편번호는 필수 입력사항입니다."
            );
        }

        // 우편번호 형식 검증
        if (!createDto.getZipCode().matches("\\d{5}")) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_ZIP_CODE,
                    "우편번호 형식이 올바르지 않습니다. 5자리 숫자로 입력해주세요."
            );
        }

        if (createDto.getAddress() == null || createDto.getAddress().isBlank()) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_DATA,
                    "주소는 필수 입력사항입니다."
            );
        }
    }

    // 주소 수정 데이터 유효성 검증
    private void validateUpdateDto(AddressUpdateDto updateDto) {
        if (updateDto.getAddressId() == null) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_DATA,
                    "주소 ID는 필수 입력사항입니다."
            );
        }

        // 전화번호가 제공되었다면 형식 검증
        if (updateDto.getPhoneNumber() != null && !updateDto.getPhoneNumber().isBlank() &&
                !updateDto.getPhoneNumber().matches("\\d{10,11}")) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_PHONE_NUMBER,
                    "전화번호 형식이 올바르지 않습니다. 숫자 10-11자리로 입력해주세요."
            );
        }

        // 우편번호가 제공되었다면 형식 검증
        if (updateDto.getZipCode() != null && !updateDto.getZipCode().isBlank() &&
                !updateDto.getZipCode().matches("\\d{5}")) {
            throw new AddressException(
                    AddressErrorCode.ADDRESS_INVALID_ZIP_CODE,
                    "우편번호 형식이 올바르지 않습니다. 5자리 숫자로 입력해주세요."
            );
        }
    }
}