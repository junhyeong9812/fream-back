package com.fream.back.domain.payment.service.command;

import com.fream.back.domain.payment.dto.paymentInfo.PaymentInfoCreateDto;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.payment.portone.PortOneApiClient;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentInfoCommandService {

    private final UserRepository userRepository;
    private final PortOneApiClient portOneApiClient; // 포트원 API 클라이언트

    @Transactional
    public void createPaymentInfo(String email, PaymentInfoCreateDto dto) {
        // 1. 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 2. 최대 결제 정보 저장 개수 확인
        if (user.getPaymentInfos().size() >= 5) {
            throw new IllegalArgumentException("최대 5개의 결제 정보만 저장할 수 있습니다.");
        }

        // 3. 테스트 결제 요청
        String impUid = portOneApiClient.requestTestPayment(dto);

        // 4. 테스트 결제 환불
        boolean refundSuccess =portOneApiClient.cancelTestPayment(impUid);


        // 5. 환불 성공 여부 확인 후 결제 정보 저장
        if (refundSuccess) {
            PaymentInfo paymentInfo = PaymentInfo.builder()
                    .user(user)
                    .cardNumber(dto.getCardNumber())
                    .cardPassword(dto.getCardPassword())
                    .expirationDate(dto.getExpirationDate())
                    .birthDate(dto.getBirthDate())
                    .build();

            user.addPaymentInfo(paymentInfo);
        } else {
            throw new IllegalArgumentException("환불 처리에 실패하여 결제 정보를 저장할 수 없습니다.");
        }
    }

    @Transactional
    public void deletePaymentInfo(String email, Long id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        PaymentInfo paymentInfo = user.getPaymentInfos().stream()
                .filter(pi -> pi.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("삭제할 결제 정보를 찾을 수 없습니다."));

        user.removePaymentInfo(paymentInfo);
    }
}
