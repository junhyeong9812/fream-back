package com.fream.back.domain.user.entity;

import com.fream.back.domain.address.entity.Address;
import com.fream.back.domain.payment.entity.PaymentInfo;
import com.fream.back.domain.product.entity.Interest;
import com.fream.back.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 사용자 ID

    @Column(nullable = false, unique = true)
    private String email; // 이메일 주소

    @Column(nullable = false)
    private String password; // 비밀번호

    @Column(nullable = false, unique = true)
    private String referralCode; // 고유 추천인 코드

    @Column(nullable = false, unique = true)
    private String phoneNumber; // 전화번호 추가

    @Enumerated(EnumType.STRING)
    private ShoeSize shoeSize; // 신발 사이즈 (Enum)

    private boolean termsAgreement; // 이용약관 동의 여부

    private boolean phoneNotificationConsent; // 전화 알림 수신 동의 여부
    private boolean emailNotificationConsent; // 이메일 수신 동의 여부
    private boolean optionalPrivacyAgreement; // 선택적 개인정보 동의 여부 추가

    @Builder.Default
    private boolean isVerified = false; // 본인인증 완료 여부

    @Column
    private String ci; // 연계정보 (Connecting Information)

    @Column
    private String di; // 중복가입확인정보 (Duplication Information)


    @Enumerated(EnumType.STRING)
    private Role role; // USER, ADMIN 등으로 역할 구분

    private Integer sellerGrade; // 판매자 등급 (1~5)

    private Integer age; // 나이

    @Enumerated(EnumType.STRING)
    private Gender gender; // 성별 (Enum)

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Profile profile; // 프로필 (1:1 관계)

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interest> interests = new ArrayList<>(); // 관심 상품 (다대다 중간 테이블)

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>(); // 주소록

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentInfo> paymentInfos = new ArrayList<>(); // 결제 정보

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private BankAccount bankAccount; // 판매 정산 계좌 (1:1 관계)

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Point> points = new ArrayList<>(); // 포인트 내역

    //추천인 로직
    @Builder.Default
    @OneToMany(mappedBy = "referrer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<User> referredUsers = new ArrayList<>(); // 내가 추천한 사용자 리스트 (1:N 관계)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrerCode", referencedColumnName = "referralCode", insertable = false, updatable = false)
    private User referrer; // 나를 추천한 사용자 정보 (N:1 관계)

    // **편의 메서드 - 값 업데이트**
    public void updateUser(String email, String password, ShoeSize shoeSize, Boolean phoneNotificationConsent, Boolean emailNotificationConsent, Integer age, Gender gender) {
        if (email != null) {
            this.email = email;
        }
        if (password != null) {
            this.password = password;
        }
        if (shoeSize != null) {
            this.shoeSize = shoeSize;
        }
        if (phoneNotificationConsent != null) {
            this.phoneNotificationConsent = phoneNotificationConsent;
        }
        if (emailNotificationConsent != null) {
            this.emailNotificationConsent = emailNotificationConsent;
        }
        if (age != null) {
            this.age = age;
        }
        if (gender != null) {
            this.gender = gender;
        }
    }
    public void updatePassword(String newPassword) {
        this.password = newPassword; // 비밀번호는 반드시 해싱하여 저장해야 함
    }
    // **개인정보 동의 업데이트 메서드**
    public void updateConsent(Boolean adConsent, Boolean optionalPrivacyAgreement) {
        if (adConsent != null && adConsent) {
            this.phoneNotificationConsent = true;
            this.emailNotificationConsent = true;
        }
        if (optionalPrivacyAgreement != null) {
            this.optionalPrivacyAgreement = optionalPrivacyAgreement;
        }
    }

    //로그인 정보 업데이트 메소드
    public void updateLoginInfo(String newEmail, String newPassword, String newPhoneNumber, ShoeSize newShoeSize,
                                Boolean adConsent, Boolean privacyConsent, Boolean smsConsent, Boolean emailConsent) {
        if (newEmail != null) {
            this.email = newEmail;
        }
        if (newPassword != null) {
            this.password = newPassword;
        }
        if (newPhoneNumber != null) {
            this.phoneNumber=newPhoneNumber;
        }
        if (newShoeSize != null) {
            this.shoeSize = newShoeSize;
        }
        if (adConsent != null) {
            // 광고성 정보 수신 동의
        }
        if (privacyConsent != null) {
            // 개인정보 동의
        }
        if (smsConsent != null) {
            this.phoneNotificationConsent = smsConsent;
        }
        if (emailConsent != null) {
            this.emailNotificationConsent = emailConsent;
        }
    }
    // 추천인 설정 메서드
    public void addReferrer(User referrer) {
        if (referrer != null) {
            this.referralCode = referrer.getReferralCode();
        } else {
            this.referralCode = null;
        }
        this.referrer = referrer;
        if (referrer != null) {
            referrer.getReferredUsers().add(this);
        }
    }

    public void removeReferrer() {
        if (this.referrer != null) {
            this.referrer.getReferredUsers().remove(this);
        }
        this.referralCode = null;
        this.referrer = null;
    }

    // **프로필 관련 편의 메서드**
    public void addProfile(Profile profile) {
        this.profile = profile;
        if (profile != null) {
            profile.addUser(this); // 연관관계 설정
        }
    }

    public void removeProfile() {
        if (this.profile != null) {
            this.profile.addUser(null); // 연관관계 해제
            this.profile = null;
        }
    }

    // 편의 메서드
    public void assignBankAccount(BankAccount bankAccount) {
        if (this.bankAccount != null && this.bankAccount != bankAccount) {
            this.bankAccount.unassignUser();
        }
        this.bankAccount = bankAccount;
        if (bankAccount != null && bankAccount.getUser() != this) {
            bankAccount.assignUser(this);
        }
    }

    public void removeBankAccount() {
        if (this.bankAccount != null) {
            this.bankAccount.unassignUser();
            this.bankAccount = null;
        }
    }

    public void addPaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfos.add(paymentInfo);
        paymentInfo.assignUser(this);
    }

    public void removePaymentInfo(PaymentInfo paymentInfo) {
        this.paymentInfos.remove(paymentInfo);
        paymentInfo.unassignUser();
    }

    public void addAddress(Address address) {
        this.addresses.add(address);
        address.assignUser(this);
    }

    public void removeAddress(Address address) {
        this.addresses.remove(address);
        address.unassignUser();
    }

    public void addInterest(Interest interest) {
        this.interests.add(interest);
        interest.assignUser(this);
    }

    public void removeInterest(Interest interest) {
        this.interests.remove(interest);
        interest.unassignUser();
    }

    public void addPoint(Point point) {
        this.points.add(point);
        point.assignUser(this);
    }

    public void removePoint(Point point) {
        this.points.remove(point);
        point.unassignUser();
    }

    public int calculateTotalPoints() {
        return this.points.stream()
                .filter(Point::isValid)
                .mapToInt(Point::getAmount)
                .sum();
    }

}
