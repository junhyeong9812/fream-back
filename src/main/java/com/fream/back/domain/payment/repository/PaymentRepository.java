package com.fream.back.domain.payment.repository;

import com.fream.back.domain.payment.entity.CardPayment;
import com.fream.back.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 결제 정보 Repository
 * 멱등성 보장을 위한 조회 메서드 추가
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 사용자 이메일과 성공 여부로 결제 내역 조회
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user.email = :email AND p.isSuccess = :isSuccess")
    List<Payment> findByOrder_User_EmailAndIsSuccess(@Param("email") String email, @Param("isSuccess") boolean isSuccess);

    /**
     * 판매자 이메일과 성공 여부로 수금 내역 조회
     */
    @Query("SELECT p FROM Payment p " +
            "JOIN p.order o " +
            "JOIN o.orderBid ob " +
            "JOIN ob.sale s " +
            "WHERE s.seller.email = :email AND p.isSuccess = :isSuccess")
    List<Payment> findBySale_Seller_EmailAndIsSuccess(@Param("email") String email, @Param("isSuccess") boolean isSuccess);

    /**
     * ImpUid로 카드 결제 정보 조회
     */
    @Query("SELECT cp FROM CardPayment cp WHERE cp.impUid = :impUid")
    Optional<CardPayment> findByImpUid(@Param("impUid") String impUid);

    /**
     * 주문 ID로 결제 정보 조회 (멱등성 보장을 위함)
     * 중복 결제 방지를 위해 사용
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId")
    Optional<Payment> findByOrder_Id(@Param("orderId") Long orderId);

    /**
     * 주문 ID로 성공한 결제가 있는지 확인 (멱등성 체크)
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.order.id = :orderId AND p.isSuccess = true")
    boolean existsSuccessfulPaymentByOrderId(@Param("orderId") Long orderId);

    /**
     * 주문 ID와 결제 상태로 결제 조회
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = :status")
    List<Payment> findByOrder_IdAndStatus(@Param("orderId") Long orderId,
                                          @Param("status") com.fream.back.domain.payment.entity.PaymentStatus status);

    /**
     * 처리 중인 결제 조회 (PENDING, PROCESSING 상태)
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status IN ('PENDING', 'PROCESSING')")
    List<Payment> findProcessingPaymentsByOrderId(@Param("orderId") Long orderId);

    /**
     * 특정 시간 이후 생성된 결제 조회 (모니터링용)
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :since")
    List<Payment> findPaymentsCreatedSince(@Param("since") java.time.LocalDateTime since);
}