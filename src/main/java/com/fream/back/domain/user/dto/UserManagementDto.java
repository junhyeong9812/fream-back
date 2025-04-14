package com.fream.back.domain.user.dto;

import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.entity.Role;
import com.fream.back.domain.user.entity.ShoeSize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class UserManagementDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSearchRequestDto {
        private String keyword;
        private String email;
        private String phoneNumber;
        private Integer ageStart;
        private Integer ageEnd;
        private Gender gender;
        private LocalDate registrationDateStart;
        private LocalDate registrationDateEnd;
        private Boolean isVerified;
        private Integer sellerGrade;
        private ShoeSize shoeSize;
        private Role role;
        private String sortField;
        private String sortDirection;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSearchResponseDto {
        private Long id;
        private String email;
        private String phoneNumber;
        private Integer age;
        private Gender gender;
        private Boolean isVerified;
        private Boolean isActive;
        private Role role;
        private Integer sellerGrade;
        private String profileName;
        private String profileImageUrl;
        private Integer totalPurchaseAmount;
        private Integer totalPoints;
        private LocalDateTime createdDate;
        private LocalDateTime updatedDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDetailResponseDto {
        private Long id;
        private String email;
        private String phoneNumber;
        private Integer age;
        private Gender gender;
        private ShoeSize shoeSize;
        private Boolean isVerified;
        private Boolean isActive;
        private String ci;
        private String di;
        private Role role;
        private Boolean termsAgreement;
        private Boolean phoneNotificationConsent;
        private Boolean emailNotificationConsent;
        private Boolean optionalPrivacyAgreement;
        private Integer sellerGrade;
        private ProfileDto profile;
        private List<AddressDto> addresses;
        private BankAccountDto bankAccount;
        private List<SanctionBriefDto> sanctions;
        private List<InterestBriefDto> interests;
        private Integer totalPoints;
        private Integer availablePoints;
        private LocalDateTime createdDate;
        private LocalDateTime updatedDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileDto {
        private Long id;
        private String profileName;
        private String name;
        private String bio;
        private Boolean isPublic;
        private String profileImageUrl;
        private Long followersCount;
        private Long followingCount;
        private Long stylesCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private Long id;
        private String name;
        private String recipientName;
        private String recipientPhone;
        private String zipCode;
        private String address1;
        private String address2;
        private Boolean isDefault;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankAccountDto {
        private Long id;
        private String bankName;
        private String accountNumber;
        private String accountHolder;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SanctionBriefDto {
        private Long id;
        private String reason;
        private String type;
        private String status;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterestBriefDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productImageUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserStatusUpdateRequestDto {
        private Long userId;
        private Boolean status;
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserGradeUpdateRequestDto {
        private Long userId;
        private Integer gradeId;
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRoleUpdateRequestDto {
        private Long userId;
        private Role role;
        private String reason;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserPointRequestDto {
        private Long userId;
        private Integer amount;
        private String reason;
        private LocalDate expirationDate;
    }
}