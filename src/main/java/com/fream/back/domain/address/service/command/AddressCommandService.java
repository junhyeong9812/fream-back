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
import com.fream.back.global.utils.PersonalDataEncryptionUtil;
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
    private final PersonalDataEncryptionUtil encryptionUtil;

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

            // 개인정보 암호화 (필드별 차별 적용)
            // 검색 가능한 필드: 결정적 암호화
            String encryptedRecipientName = encryptionUtil.deterministicEncrypt(createDto.getRecipientName());
            String encryptedPhoneNumber = encryptionUtil.deterministicEncrypt(createDto.getPhoneNumber());
            String encryptedZipCode = encryptionUtil.deterministicEncrypt(createDto.getZipCode());
            String encryptedAddress = encryptionUtil.deterministicEncrypt(createDto.getAddress());

            // 상세주소: 양방향 암호화 (보안성 우선)
            String encryptedDetailedAddress = createDto.getDetailedAddress() != null ?
                    encryptionUtil.encrypt(createDto.getDetailedAddress()) : null;

            Address newAddress = Address.builder()
                    .user(user)
                    .recipientName(encryptedRecipientName)
                    .phoneNumber(encryptedPhoneNumber)
                    .zipCode(encryptedZipCode)
                    .address(encryptedAddress)
                    .detailedAddress(encryptedDetailedAddress)
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

            if (updateDto.getIsDefault() != null && updateDto.getIsDefault()) {
                log.debug("기존 기본 주소 해제: 사용자={}", email);
                user.getAddresses().forEach(addr -> addr.updateAddress(
                        null, null, null, null, null, false));
            }

            // 개인정보 암호화 (필드별 차별 적용)
            // 검색 가능한 필드: 결정적 암호화
            String encryptedRecipientName = updateDto.getRecipientName() != null ?
                    encryptionUtil.deterministicEncrypt(updateDto.getRecipientName()) : null;
            String encryptedPhoneNumber = updateDto.getPhoneNumber() != null ?
                    encryptionUtil.deterministicEncrypt(updateDto.getPhoneNumber()) : null;
            String encryptedZipCode = updateDto.getZipCode() != null ?
                    encryptionUtil.deterministicEncrypt(updateDto.getZipCode()) : null;
            String encryptedAddress = updateDto.getAddress() != null ?
                    encryptionUtil.deterministicEncrypt(updateDto.getAddress()) : null;

            // 상세주소: 양방향 암호화 (보안성 우선)
            String encryptedDetailedAddress = updateDto.getDetailedAddress() != null ?
                    encryptionUtil.encrypt(updateDto.getDetailedAddress()) : null;

            address.updateAddress(
                    encryptedRecipientName,
                    encryptedPhoneNumber,
                    encryptedZipCode,
                    encryptedAddress,
                    encryptedDetailedAddress,
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
}