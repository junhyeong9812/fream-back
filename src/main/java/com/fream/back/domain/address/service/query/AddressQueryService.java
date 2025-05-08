package com.fream.back.domain.address.service.query;

import com.fream.back.domain.address.dto.AddressResponseDto;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressQueryService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PersonalDataEncryptionUtil encryptionUtil;

    //주소지 목록 조회
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getAddresses(String email) {
        try {
            log.info("주소 목록 조회 시작: 사용자={}", email);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AddressUserNotFoundException(
                            "이메일 '" + email + "'에 해당하는 사용자를 찾을 수 없습니다."
                    ));

            List<AddressResponseDto> addresses = user.getAddresses().stream()
                    .map(address -> new AddressResponseDto(
                            address.getId(),
                            // 암호화된 값 복호화
                            encryptionUtil.decrypt(address.getRecipientName()),
                            encryptionUtil.decrypt(address.getPhoneNumber()),
                            encryptionUtil.decrypt(address.getZipCode()),
                            encryptionUtil.decrypt(address.getAddress()),
                            address.getDetailedAddress() != null ?
                                    encryptionUtil.decrypt(address.getDetailedAddress()) : null,
                            address.isDefault()))
                    .collect(Collectors.toList());

            log.info("주소 목록 조회 완료: 사용자={}, 조회된 주소 수={}", email, addresses.size());
            return addresses;
        } catch (AddressException e) {
            throw e; // 이미 AddressException이면 그대로 전파
        } catch (Exception e) {
            log.error("주소 목록 조회 중 오류 발생: 사용자={}, 오류={}", email, e.getMessage(), e);
            throw new AddressException(AddressErrorCode.ADDRESS_QUERY_ERROR, e);
        }
    }

    //단일 주소지 조회
    @Transactional(readOnly = true)
    public AddressResponseDto getAddress(String email, Long addressId) {
        try {
            log.info("단일 주소 조회 시작: 사용자={}, 주소ID={}", email, addressId);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AddressUserNotFoundException(
                            "이메일 '" + email + "'에 해당하는 사용자를 찾을 수 없습니다."
                    ));

            Address address = addressRepository.findByIdAndUser(addressId, user)
                    .orElseThrow(() -> new AddressNotFoundException(
                            "주소 ID '" + addressId + "'에 해당하는 주소를 찾을 수 없습니다."
                    ));

            // 암호화된 값 복호화하여 DTO 생성
            AddressResponseDto responseDto = new AddressResponseDto(
                    address.getId(),
                    encryptionUtil.decrypt(address.getRecipientName()),
                    encryptionUtil.decrypt(address.getPhoneNumber()),
                    encryptionUtil.decrypt(address.getZipCode()),
                    encryptionUtil.decrypt(address.getAddress()),
                    address.getDetailedAddress() != null ?
                            encryptionUtil.decrypt(address.getDetailedAddress()) : null,
                    address.isDefault()
            );

            log.info("단일 주소 조회 완료: 사용자={}, 주소ID={}", email, addressId);
            return responseDto;
        } catch (AddressException e) {
            throw e; // 이미 AddressException이면 그대로 전파
        } catch (Exception e) {
            log.error("단일 주소 조회 중 오류 발생: 사용자={}, 주소ID={}, 오류={}",
                    email, addressId, e.getMessage(), e);
            throw new AddressException(AddressErrorCode.ADDRESS_QUERY_ERROR, e);
        }
    }
}