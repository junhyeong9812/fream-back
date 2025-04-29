package com.fream.back.domain.payment.service.command;

import com.fream.back.domain.notification.entity.NotificationCategory;
import com.fream.back.domain.notification.entity.NotificationType;
import com.fream.back.domain.notification.service.command.NotificationCommandService;
import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderBid;
import com.fream.back.domain.payment.dto.AccountPaymentRequestDto;
import com.fream.back.domain.payment.dto.CardPaymentRequestDto;
import com.fream.back.domain.payment.dto.GeneralPaymentRequestDto;
import com.fream.back.domain.payment.dto.PaymentRequestDto;
import com.fream.back.domain.payment.entity.*;
import com.fream.back.domain.payment.exception.*;
import com.fream.back.domain.payment.portone.PortOneApiClient;
import com.fream.back.domain.payment.repository.PaymentRepository;
import com.fream.back.domain.payment.service.query.PaymentInfoQueryService;
import com.fream.back.domain.sale.entity.Sale;
import com.fream.back.domain.sale.entity.SaleBid;
import com.fream.back.domain.sale.entity.SaleStatus;
import com.fream.back.domain.sale.service.query.SaleBidQueryService;
import com.fream.back.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 결제 명령 서비스
 * 결제 요청 처리, 환불 등 결제 관련 명령을 처리
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentCommandService {

    private final PaymentRepository paymentRepository;
    private final PaymentInfoQueryService paymentInfoQueryService;
    private final PortOneApiClient portOneApiClient;
    private final NotificationCommandService notificationCommandService;
    private final SaleBidQueryService saleBidQueryService;

    /**
     * 결제 처리
     * 결제 유형에 따라 적절한 결제 처리 로직 수행
     *
     * @param order      결제 대상 주문
     * @param user       결제 요청 사용자
     * @param requestDto 결제 요청 정보
     * @return 생성된 결제 정보
     * @throws PaymentException 결제 처리 실패 시
     */
    public Payment processPayment(Order order, User user, PaymentRequestDto requestDto) {
        Instant start = Instant.now();
        try {
            validatePaymentRequest(order, user, requestDto);

            log.info("결제 처리 시작: 주문ID={}, 사용자={}, 결제유형={}",
                    order.getId(), user.getEmail(), requestDto.getResolvedPaymentType());

            Payment payment;

            switch (requestDto.getResolvedPaymentType()) {
                case "CARD":
                    payment = createCardPayment(order, user, (CardPaymentRequestDto) requestDto);
                    break;
                case "ACCOUNT":
                    payment = createAccountPayment(order, user, (AccountPaymentRequestDto) requestDto);
                    break;
                case "GENERAL":
                    payment = createGeneralPayment(order, user, (GeneralPaymentRequestDto) requestDto);
                    break;
                default:
                    throw new PaymentException(PaymentErrorCode.PAYMENT_VALIDATION_FAILED,
                            "유효하지 않은 결제 유형: " + requestDto.getPaymentType());
            }

            // 알림 전송 및 Sale 상태 업데이트
            notifyUsersAndUpdateSaleStatus(order, payment);

            log.info("결제 처리 완료: 주문ID={}, 결제ID={}, 상태={}",
                    order.getId(), payment.getId(), payment.getStatus());

            return payment;
        } catch (PaymentException e) {
            log.error("결제 처리 실패: 주문ID={}, 사용자={}, 에러코드={}, 메시지={}",
                    order != null ? order.getId() : "null",
                    user != null ? user.getEmail() : "null",
                    e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 처리 중 예상치 못한 오류 발생: 주문ID={}, 사용자={}, 오류={}",
                    order != null ? order.getId() : "null",
                    user != null ? user.getEmail() : "null",
                    e.getMessage(), e);
            throw new PaymentProcessingException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 처리 총 소요 시간: {}ms", duration.toMillis());
        }
    }

    /**
     * 결제 요청 검증
     *
     * @param order      주문 정보
     * @param user       사용자 정보
     * @param requestDto 결제 요청 DTO
     * @throws PaymentException 검증 실패 시
     */
    private void validatePaymentRequest(Order order, User user, PaymentRequestDto requestDto) {
        if (order == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PROCESSING_FAILED, "주문 정보가 없습니다.");
        }

        if (user == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_PROCESSING_FAILED, "사용자 정보가 없습니다.");
        }

        if (requestDto == null) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_VALIDATION_FAILED, "결제 요청 정보가 없습니다.");
        }

        // 주문 ID와 사용자 이메일이 일치하는지 확인
        if (!order.getId().equals(requestDto.getOrderId())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_VALIDATION_FAILED,
                    "주문 ID가 일치하지 않습니다. 요청ID: " + requestDto.getOrderId() + ", 실제ID: " + order.getId());
        }

        if (!user.getEmail().equals(requestDto.getUserEmail())) {
            throw new PaymentException(PaymentErrorCode.PAYMENT_VALIDATION_FAILED,
                    "사용자 이메일이 일치하지 않습니다. 요청이메일: " + requestDto.getUserEmail() + ", 실제이메일: " + user.getEmail());
        }

        // 결제 금액 검증
        if (requestDto.getPaidAmount() <= 0) {
            throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_AMOUNT,
                    "결제 금액은 0보다 커야 합니다. 금액: " + requestDto.getPaidAmount());
        }
    }

    /**
     * 카드 결제 생성
     *
     * @param order      주문 정보
     * @param user       사용자 정보
     * @param requestDto 카드 결제 요청 DTO
     * @return 생성된 카드 결제 정보
     * @throws PaymentException 결제 처리 실패 시
     */
    private Payment createCardPayment(Order order, User user, CardPaymentRequestDto requestDto) {
        Instant start = Instant.now();
        try {
            log.info("카드 결제 처리 시작: 주문ID={}, 결제정보ID={}",
                    order.getId(), requestDto.getPaymentInfoId());

            // 1. PaymentInfo 조회
            PaymentInfo paymentInfo;
            try {
                paymentInfo = paymentInfoQueryService.getPaymentInfoEntity(user.getEmail(), requestDto.getPaymentInfoId());
                if (paymentInfo == null) {
                    throw new PaymentInfoNotFoundException("결제 정보 ID: " + requestDto.getPaymentInfoId() + "를 찾을 수 없습니다.");
                }
            } catch (PaymentException e) {
                throw e; // 이미 PaymentException이면 그대로 전파
            } catch (Exception e) {
                throw new PaymentInfoNotFoundException("결제 정보 조회 중 오류가 발생했습니다.", e);
            }

            // 2. 결제 요청 생성
            Map<String, Object> response;
            try {
                response = portOneApiClient.processCardPayment(paymentInfo, requestDto.getPaidAmount());
            } catch (PaymentException e) {
                throw e; // 이미 PaymentException이면 그대로 전파
            } catch (Exception e) {
                throw new PaymentApiException("카드 결제 요청 중 오류가 발생했습니다: " + e.getMessage());
            }

            // 3. CardPayment 생성 및 저장
            CardPayment cardPayment = CardPayment.builder()
                    .cardNumber(paymentInfo.getCardNumber())
                    .cardPassword(paymentInfo.getCardPassword())
                    .cardExpiration(paymentInfo.getExpirationDate())
                    .birthDate(paymentInfo.getBirthDate())
                    .cardType((String) response.get("card_name"))
                    .paidAmount(requestDto.getPaidAmount())
                    .impUid((String) response.get("imp_uid"))
                    .receiptUrl((String) response.get("receipt_url"))
                    .pgProvider((String) response.get("pg_provider"))
                    .pgTid((String) response.get("pg_tid"))
                    .build();

            cardPayment.assignOrder(order);
            cardPayment.assignUser(user);

            if ("success".equals(response.get("status"))) {
                cardPayment.updateStatus(PaymentStatus.PAID);
                cardPayment.updateSuccessStatus(true); // 결제 성공
            }

            CardPayment savedPayment = (CardPayment) paymentRepository.save(cardPayment);
            log.info("카드 결제 처리 완료: 주문ID={}, 결제ID={}, 상태={}",
                    order.getId(), savedPayment.getId(), savedPayment.getStatus());

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("카드 결제 처리 시간: {}ms", duration.toMillis());

            return savedPayment;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("카드 결제 처리 중 예상치 못한 오류 발생: 주문ID={}, 오류={}",
                    order.getId(), e.getMessage(), e);
            throw new PaymentProcessingException("카드 결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 계좌이체 결제 생성
     *
     * @param order      주문 정보
     * @param user       사용자 정보
     * @param requestDto 계좌이체 결제 요청 DTO
     * @return 생성된 계좌이체 결제 정보
     * @throws PaymentException 결제 처리 실패 시
     */
    private Payment createAccountPayment(Order order, User user, AccountPaymentRequestDto requestDto) {
        Instant start = Instant.now();
        try {
            log.info("계좌이체 결제 처리 시작: 주문ID={}, 은행={}",
                    order.getId(), requestDto.getBankName());

            // 은행 유효성 검증
            try {
                validateBank(requestDto.getBankName());
            } catch (Exception e) {
                throw new PaymentException(PaymentErrorCode.INVALID_BANK_INFO,
                        "유효하지 않은 은행 정보: " + requestDto.getBankName());
            }

            AccountPayment accountPayment = AccountPayment.builder()
                    .bankName(requestDto.getBankName())
                    .accountNumber(requestDto.getAccountNumber())
                    .accountHolder(requestDto.getAccountHolder())
                    .receiptRequested(requestDto.isReceiptRequested())
                    .paidAmount(requestDto.getPaidAmount())
                    .build();

            accountPayment.assignOrder(order);
            accountPayment.assignUser(user);
            accountPayment.updateStatus(PaymentStatus.PENDING);

            AccountPayment savedPayment = (AccountPayment) paymentRepository.save(accountPayment);
            log.info("계좌이체 결제 처리 완료: 주문ID={}, 결제ID={}, 상태={}",
                    order.getId(), savedPayment.getId(), savedPayment.getStatus());

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("계좌이체 결제 처리 시간: {}ms", duration.toMillis());

            return savedPayment;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("계좌이체 결제 처리 중 예상치 못한 오류 발생: 주문ID={}, 오류={}",
                    order.getId(), e.getMessage(), e);
            throw new PaymentProcessingException("계좌이체 결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 일반 결제 생성
     *
     * @param order      주문 정보
     * @param user       사용자 정보
     * @param requestDto 일반 결제 요청 DTO
     * @return 생성된 일반 결제 정보
     * @throws PaymentException 결제 처리 실패 시
     */
    private Payment createGeneralPayment(Order order, User user, GeneralPaymentRequestDto requestDto) {
        Instant start = Instant.now();
        try {
            log.info("일반 결제 처리 시작: 주문ID={}, impUid={}",
                    order.getId(), requestDto.getImpUid());

            // 필수 데이터 검증
            if (requestDto.getImpUid() == null || requestDto.getImpUid().isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_VALIDATION_FAILED,
                        "결제 ID(impUid)는 필수 항목입니다.");
            }

            GeneralPayment generalPayment = GeneralPayment.builder()
                    .impUid(requestDto.getImpUid())
                    .pgProvider(requestDto.getPgProvider())
                    .receiptUrl(requestDto.getReceiptUrl())
                    .buyerName(requestDto.getBuyerName())
                    .buyerEmail(requestDto.getBuyerEmail())
                    .paidAmount(requestDto.getPaidAmount())
                    .build();

            generalPayment.assignOrder(order);
            generalPayment.assignUser(user);
            generalPayment.updateSuccessStatus(true);
            generalPayment.updateStatus(PaymentStatus.PAID);

            GeneralPayment savedPayment = (GeneralPayment) paymentRepository.save(generalPayment);
            log.info("일반 결제 처리 완료: 주문ID={}, 결제ID={}, 상태={}",
                    order.getId(), savedPayment.getId(), savedPayment.getStatus());

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("일반 결제 처리 시간: {}ms", duration.toMillis());

            return savedPayment;
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("일반 결제 처리 중 예상치 못한 오류 발생: 주문ID={}, 오류={}",
                    order.getId(), e.getMessage(), e);
            throw new PaymentProcessingException("일반 결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 환불 처리 (필요한 경우)
     *
     * @param payment 환불할 결제 정보
     * @throws PaymentException 환불 처리 실패 시
     */
    private void handleRefundIfNecessary(Payment payment) {
        Instant start = Instant.now();
        try {
            if (payment.getImpUid() == null || payment.getImpUid().isBlank()) {
                log.warn("환불 처리 불가: 결제ID={}, 결제식별자(impUid)가 없습니다.", payment.getId());
                return;
            }

            log.info("결제 환불 처리 시작: 결제ID={}, impUid={}", payment.getId(), payment.getImpUid());

            boolean refundSuccess = portOneApiClient.cancelPayment(payment.getImpUid());

            if (refundSuccess) {
                payment.updateStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
                log.info("결제 환불 처리 완료: 결제ID={}, 상태=REFUNDED", payment.getId());
            } else {
                log.error("결제 환불 처리 실패: 결제ID={}", payment.getId());
                throw new PaymentException(PaymentErrorCode.PAYMENT_CANCELLATION_FAILED,
                        "환불 요청이 실패했습니다. 관리자에게 문의하세요.");
            }

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 환불 처리 시간: {}ms", duration.toMillis());
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 환불 처리 중 예상치 못한 오류 발생: 결제ID={}, 오류={}",
                    payment.getId(), e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_CANCELLATION_FAILED,
                    "환불 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 결제 완료 알림 및 판매 상태 업데이트
     *
     * @param order   주문 정보
     * @param payment 결제 정보
     */
    private void notifyUsersAndUpdateSaleStatus(Order order, Payment payment) {
        try {
            log.info("결제 관련 알림 및 판매 상태 업데이트 시작: 주문ID={}", order.getId());

            // 1. OrderBid 조회
            OrderBid orderBid = order.getOrderBid();
            if (orderBid == null) {
                // OrderBid가 없으면 SaleBid 탐색
                SaleBid saleBid = saleBidQueryService.findByOrderId(order.getId());
                if (saleBid == null) {
                    log.warn("주문에 연결된 판매 입찰 정보가 없습니다: 주문ID={}", order.getId());
                    return;
                }

                Sale sale = saleBid.getSale();
                if (sale == null) {
                    log.warn("판매 입찰에 연결된 판매 정보가 없습니다: 판매입찰ID={}", saleBid.getId());
                    return;
                }

                sendNotifications(order, sale); // 알림 전송
                sale.updateStatus(SaleStatus.SOLD); // Sale 상태 변경
                log.info("판매 상태 업데이트 완료: 판매ID={}, 상태=SOLD", sale.getId());
                return;
            }

            // 2. Sale 조회
            Sale sale = orderBid.getSale();
            if (sale == null) {
                log.warn("주문 입찰에 연결된 판매 정보가 없습니다: 주문입찰ID={}", orderBid.getId());
                return;
            }

            sendNotifications(order, sale); // 알림 전송

            // 3. Sale 상태 변경
            sale.updateStatus(SaleStatus.SOLD);
            log.info("판매 상태 업데이트 완료: 판매ID={}, 상태=SOLD", sale.getId());
        } catch (Exception e) {
            // 알림 및 상태 업데이트 실패 로깅만 하고 예외는 발생시키지 않음 (핵심 기능이 아니므로)
            log.error("결제 관련 알림 및 판매 상태 업데이트 중 오류 발생: 주문ID={}, 오류={}",
                    order.getId(), e.getMessage(), e);
        }
    }

    /**
     * 알림 전송
     *
     * @param order 주문 정보
     * @param sale  판매 정보
     */
    private void sendNotifications(Order order, Sale sale) {
        try {
            User buyer = order.getUser();
            if (buyer != null) {
                notificationCommandService.createNotification(
                        buyer.getId(),
                        NotificationCategory.SHOPPING,
                        NotificationType.BID,
                        "결제가 완료되었습니다. 주문 ID: " + order.getId()
                );
                log.debug("구매자 알림 전송 완료: 구매자ID={}", buyer.getId());
            }

            User seller = sale.getSeller();
            if (seller != null) {
                notificationCommandService.createNotification(
                        seller.getId(),
                        NotificationCategory.SHOPPING,
                        NotificationType.BID,
                        "구매자가 결제를 완료하였습니다. 판매 ID: " + sale.getId()
                );
                log.debug("판매자 알림 전송 완료: 판매자ID={}", seller.getId());
            }
        } catch (Exception e) {
            // 알림 전송 실패 로깅만 하고 예외는 발생시키지 않음
            log.warn("결제 완료 알림 전송 중 오류 발생: 주문ID={}, 판매ID={}, 오류={}",
                    order.getId(), sale.getId(), e.getMessage());
        }
    }

    /**
     * 은행 유효성 검사
     *
     * @param bankName 은행명
     * @throws PaymentException 유효하지 않은 은행명인 경우
     */
    private void validateBank(String bankName) {
        try {
            if (bankName == null || bankName.isBlank()) {
                throw new PaymentException(PaymentErrorCode.INVALID_BANK_INFO, "은행 이름은 필수입니다.");
            }

            try {
                Bank.valueOf(bankName.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PaymentException(PaymentErrorCode.INVALID_BANK_INFO,
                        "지원하지 않는 은행입니다: " + bankName);
            }
        } catch (PaymentException e) {
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            throw new PaymentException(PaymentErrorCode.INVALID_BANK_INFO,
                    "은행 정보 검증 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 결제 상태 변경
     *
     * @param payment   상태 변경할 결제 정보
     * @param newStatus 새로운 상태
     * @throws PaymentException 상태 변경 실패 시
     */
    @Transactional
    public void updatePaymentStatus(Payment payment, PaymentStatus newStatus) {
        Instant start = Instant.now();
        try {
            if (payment == null) {
                throw new PaymentNotFoundException("결제 정보가 없습니다.");
            }

            if (!payment.getStatus().canTransitionTo(newStatus)) {
                throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS,
                        "현재 상태(" + payment.getStatus() + ")에서 " + newStatus + " 상태로 변경할 수 없습니다.");
            }

            log.info("결제 상태 변경: 결제ID={}, 현재상태={}, 새상태={}",
                    payment.getId(), payment.getStatus(), newStatus);

            payment.updateStatus(newStatus);
            paymentRepository.save(payment);

            log.info("결제 상태 변경 완료: 결제ID={}, 상태={}", payment.getId(), newStatus);

            // 처리 시간 로깅
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            log.debug("결제 상태 변경 처리 시간: {}ms", duration.toMillis());
        } catch (PaymentException e) {
            log.error("결제 상태 변경 실패: 결제ID={}, 에러코드={}, 메시지={}",
                    payment != null ? payment.getId() : "null",
                    e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 상태 변경 중 예상치 못한 오류 발생: 결제ID={}, 오류={}",
                    payment != null ? payment.getId() : "null", e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_UPDATE_FAILED,
                    "결제 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 결제 환불 요청
     *
     * @param paymentId 환불할 결제 ID
     * @return 환불 처리 결과 메시지
     * @throws PaymentException 환불 처리 실패 시
     */
    @Transactional
    public String refundPayment(Long paymentId) {
        Instant start = Instant.now();
        try {
            log.info("결제 환불 요청 시작: 결제ID={}", paymentId);

            // 1. Payment 정보 조회
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new PaymentNotFoundException("결제 ID: " + paymentId + "를 찾을 수 없습니다."));

            // 2. 현재 상태 확인
            if (!payment.getStatus().canTransitionTo(PaymentStatus.REFUND_REQUESTED)) {
                throw new PaymentException(PaymentErrorCode.INVALID_PAYMENT_STATUS,
                        "현재 상태에서 환불 요청을 진행할 수 없습니다. 현재 상태: " + payment.getStatus());
            }

            // 3. 포트원 API를 통해 환불 요청
            if (payment.getImpUid() == null || payment.getImpUid().isBlank()) {
                throw new PaymentException(PaymentErrorCode.PAYMENT_CANCELLATION_FAILED,
                        "환불 가능한 결제 ID가 없습니다.");
            }

            boolean refundSuccess = portOneApiClient.cancelPayment(payment.getImpUid());

            if (refundSuccess) {
                payment.updateStatus(PaymentStatus.REFUNDED); // 상태를 REFUNDED로 변경
                paymentRepository.save(payment);
                log.info("결제 환불 요청 성공: 결제ID={}, 상태=REFUNDED", paymentId);

                // 환불 성공 시 알림 발송
                sendRefundNotification(payment);

                // 처리 시간 로깅
                Instant end = Instant.now();
                Duration duration = Duration.between(start, end);
                log.debug("결제 환불 처리 시간: {}ms", duration.toMillis());

                return "환불이 성공적으로 완료되었습니다.";
            } else {
                payment.updateStatus(PaymentStatus.REFUND_REQUESTED); // 환불 요청 상태로 업데이트
                paymentRepository.save(payment);
                log.warn("결제 환불 요청 실패: 결제ID={}, 상태=REFUND_REQUESTED", paymentId);
                return "환불 요청이 실패하였습니다. 관리자에게 문의하세요.";
            }
        } catch (PaymentException e) {
            log.error("결제 환불 요청 실패: 결제ID={}, 에러코드={}, 메시지={}",
                    paymentId, e.getErrorCode().getCode(), e.getMessage());
            throw e; // 이미 PaymentException이면 그대로 전파
        } catch (Exception e) {
            log.error("결제 환불 요청 중 예상치 못한 오류 발생: 결제ID={}, 오류={}",
                    paymentId, e.getMessage(), e);
            throw new PaymentException(PaymentErrorCode.PAYMENT_CANCELLATION_FAILED,
                    "환불 요청 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 환불 알림 발송
     *
     * @param payment 환불된 결제 정보
     */
    private void sendRefundNotification(Payment payment) {
        try {
            User user = payment.getUser();
            if (user == null) {
                log.warn("환불 알림 발송 실패: 결제ID={}, 사용자 정보가 없습니다.", payment.getId());
                return;
            }

            Order order = payment.getOrder();
            String orderInfo = order != null ? "주문 ID: " + order.getId() : "환불금액: " + payment.getPaidAmount() + "원";

            notificationCommandService.createNotification(
                    user.getId(),
                    NotificationCategory.SHOPPING,
                    NotificationType.REFUND,
                    "결제가 환불되었습니다. " + orderInfo
            );
            log.debug("환불 알림 전송 완료: 사용자ID={}, 결제ID={}", user.getId(), payment.getId());

            // 판매자 알림 - 주문과 연결된 판매 정보가 있는 경우
            if (order != null) {
                Sale sale = null;

                // OrderBid를 통한 Sale 조회
                OrderBid orderBid = order.getOrderBid();
                if (orderBid != null) {
                    sale = orderBid.getSale();
                }
                // SaleBid를 통한 Sale 조회
                else {
                    SaleBid saleBid = saleBidQueryService.findByOrderId(order.getId());
                    if (saleBid != null) {
                        sale = saleBid.getSale();
                    }
                }

                if (sale != null && sale.getSeller() != null) {
                    User seller = sale.getSeller();
                    notificationCommandService.createNotification(
                            seller.getId(),
                            NotificationCategory.SHOPPING,
                            NotificationType.REFUND,
                            "구매자의 결제가 환불되었습니다. 판매 ID: " + sale.getId()
                    );
                    log.debug("판매자 환불 알림 전송 완료: 판매자ID={}, 판매ID={}",
                            seller.getId(), sale.getId());

                    // 중고거래 특성상 판매 완료 후에는 상태를 변경하지 않음
                    // 판매자가 직접 새로운 판매 게시글을 등록해야 함
                }
            }
        } catch (Exception e) {
            // 알림 전송 실패 로깅만 하고 예외는 발생시키지 않음
            log.warn("환불 알림 전송 중 오류 발생: 결제ID={}, 오류={}",
                    payment.getId(), e.getMessage());
        }
    }
}