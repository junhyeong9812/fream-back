package com.fream.back.domain.characterization;

import com.fream.back.domain.order.entity.Order;
import com.fream.back.domain.order.entity.OrderStatus;
import com.fream.back.domain.payment.entity.GeneralPayment;
import com.fream.back.domain.user.entity.User;
import com.fream.back.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SCC 특성화 테스트 — Order ↔ Payment ↔ User 엔티티 관계.
 *
 * <p>모듈리스 리팩토링은 이 크로스 도메인 FK 순환(order↔payment, payment→user)을
 * ID 참조/이벤트로 끊을 예정이다. 본 테스트는 끊기 전 현재의 영속/연관 동작과
 * 불변식(Payment는 항상 User를 가진다)을 고정해, 전환 중 회귀를 감지하는 그물이다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class OrderPaymentRelationshipTest {

    @Autowired
    private TestEntityManager em;

    @Test
    void orderAndPayment_bidirectionalRelationship_persistsAndLoads() {
        User buyer = newUser("buyer@test.com", "REF-BUYER");
        em.persist(buyer);

        Order order = Order.builder()
                .user(buyer)
                .totalAmount(10_000)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        GeneralPayment payment = GeneralPayment.builder()
                .impUid("imp_001")
                .paidAmount(10_000)
                .build();
        payment.assignUser(buyer);

        // Order.payment(cascade ALL) ↔ Payment.order 양방향 설정
        order.assignPayment(payment);

        em.persist(order);
        em.flush();
        em.clear();

        Order reloaded = em.find(Order.class, order.getId());
        assertThat(reloaded.getPayment()).isNotNull();
        assertThat(reloaded.getPayment().getId()).isNotNull();
        assertThat(reloaded.getPayment().getOrder().getId()).isEqualTo(reloaded.getId());
        assertThat(reloaded.getPayment().getUser().getId()).isEqualTo(buyer.getId());
        assertThat(reloaded.getUser().getId()).isEqualTo(buyer.getId());
    }

    @Test
    void payment_userIsMandatory_notNullConstraintHolds() {
        Order order = Order.builder()
                .totalAmount(1_000)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        GeneralPayment payment = GeneralPayment.builder()
                .impUid("imp_no_user")
                .paidAmount(1_000)
                .build();
        // 의도적으로 user 미지정 — payment.user_id 는 NOT NULL
        order.assignPayment(payment);

        assertThatThrownBy(() -> {
            em.persist(order);
            em.flush();
        }).isInstanceOf(Exception.class);
    }

    private User newUser(String email, String referralCode) {
        return User.builder()
                .email(email)
                .password("encoded-pw")
                .referralCode(referralCode)
                .phoneNumber("010-0000-0000")
                .build();
    }
}
