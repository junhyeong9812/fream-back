package com.fream.back.domain.user.service.admin;


import com.fream.back.domain.user.dto.UserManagementDto.*;
import com.fream.back.domain.user.entity.*;
import com.fream.back.domain.user.repository.*;

import com.fream.back.global.exception.EntityNotFoundException;
import com.fream.back.global.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PointRepository pointRepository;
    private final UserGradeRepository userGradeRepository;
    private final UserSanctionRepository userSanctionRepository;
    private final BankAccountRepository bankAccountRepository;

    /**
     * 사용자 검색 (페이징)
     */
    public Page<UserSearchResponseDto> searchUsers(UserSearchRequestDto searchDto, int page, int size) {
        // 정렬 방향 설정
        Sort.Direction direction = searchDto.getSortDirection() != null &&
                searchDto.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 정렬 필드 설정 (기본값: createdDate)
        String sortField = searchDto.getSortField() != null ? searchDto.getSortField() : "createdDate";

        // 페이징 및 정렬 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // 검색 사양 구성
        Specification<User> spec = Specification.where(null);

        // 키워드 검색 (이메일, 전화번호, 프로필명)
        if (searchDto.getKeyword() != null && !searchDto.getKeyword().isEmpty()) {
            String keyword = "%" + searchDto.getKeyword() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("email"), keyword),
                            cb.like(root.get("phoneNumber"), keyword),
                            cb.like(root.join("profile").get("profileName"), keyword)
                    )
            );
        }

        // 이메일 필터
        if (searchDto.getEmail() != null && !searchDto.getEmail().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("email"), "%" + searchDto.getEmail() + "%")
            );
        }

        // 전화번호 필터
        if (searchDto.getPhoneNumber() != null && !searchDto.getPhoneNumber().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + searchDto.getPhoneNumber() + "%")
            );
        }

        // 나이 범위 필터
        if (searchDto.getAgeStart() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("age"), searchDto.getAgeStart())
            );
        }
        if (searchDto.getAgeEnd() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("age"), searchDto.getAgeEnd())
            );
        }

        // 성별 필터
        if (searchDto.getGender() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("gender"), searchDto.getGender())
            );
        }

        // 가입일 범위 필터
        if (searchDto.getRegistrationDateStart() != null) {
            LocalDateTime startDateTime = searchDto.getRegistrationDateStart().atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdDate"), startDateTime)
            );
        }
        if (searchDto.getRegistrationDateEnd() != null) {
            LocalDateTime endDateTime = searchDto.getRegistrationDateEnd().plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("createdDate"), endDateTime)
            );
        }

        // 본인인증 여부 필터
        if (searchDto.getIsVerified() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isVerified"), searchDto.getIsVerified())
            );
        }

        // 판매자 등급 필터
        if (searchDto.getSellerGrade() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("sellerGrade"), searchDto.getSellerGrade())
            );
        }

        // 신발 사이즈 필터
        if (searchDto.getShoeSize() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("shoeSize"), searchDto.getShoeSize())
            );
        }

        // 권한 필터
        if (searchDto.getRole() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), searchDto.getRole())
            );
        }

        // 사용자 조회 및 DTO 변환
        return userRepository.findAll(spec, pageable)
                .map(this::convertToUserSearchResponseDto);
    }

    /**
     * 사용자 상세 정보 조회
     */
    public UserDetailResponseDto getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        return convertToUserDetailResponseDto(user);
    }

    /**
     * 사용자 상태 업데이트 (활성/비활성)
     */
    @Transactional
    public UserDetailResponseDto updateUserStatus(UserStatusUpdateRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

        user.addActive(requestDto.getStatus());

        // 비활성화 시 제재 내역 기록 (선택적)
        if (!requestDto.getStatus() && requestDto.getReason() != null) {
            UserSanction sanction = UserSanction.builder()
                    .user(user)
                    .reason(requestDto.getReason())
                    .type(SanctionType.TEMPORARY_BAN)
                    .status(SanctionStatus.ACTIVE)
                    .startDate(LocalDateTime.now())
                    .createdBy("admin") // 실제 구현에서는 인증된 관리자 정보 사용
                    .build();

            userSanctionRepository.save(sanction);
        }

        return convertToUserDetailResponseDto(user);
    }

    /**
     * 사용자 등급 업데이트
     */
    @Transactional
    public UserDetailResponseDto updateUserGrade(UserGradeUpdateRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

        UserGrade grade = userGradeRepository.findById(requestDto.getGradeId().longValue())
                .orElseThrow(() -> new EntityNotFoundException("Grade not found with id: " + requestDto.getGradeId()));

        user.addGrade(grade);

        return convertToUserDetailResponseDto(user);
    }


    /**
     * 사용자 역할 업데이트
     */
    @Transactional
    public UserDetailResponseDto updateUserRole(UserRoleUpdateRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

        // 역할 변경
        user.changeRole(requestDto.getRole());

        // 확장된 updateUser 메서드 호출
        user.updateUser(user.getEmail(), null, user.getShoeSize(),
                user.isPhoneNotificationConsent(), user.isEmailNotificationConsent(),
                user.getAge(), user.getGender(), user.isActive(), requestDto.getRole());

        return convertToUserDetailResponseDto(user);
    }

    /**
     * 사용자 포인트 지급/차감
     */
    @Transactional
    public void manageUserPoints(UserPointRequestDto requestDto) {
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

        // 포인트 지급
        if (requestDto.getAmount() > 0) {
            Point point = new Point(user, requestDto.getAmount(), requestDto.getReason());

            // 유효기간 설정 (기본 90일, 요청에 따라 다르게 설정 가능)
            if (requestDto.getExpirationDate() != null) {
                point.setExpirationDate(requestDto.getExpirationDate());
            }

            pointRepository.save(point);
        }
        // 포인트 차감
        else if (requestDto.getAmount() < 0) {
            int remainingToDeduct = Math.abs(requestDto.getAmount());
            int availablePoints = user.calculateTotalPoints();

            if (availablePoints < remainingToDeduct) {
                throw new InvalidRequestException("Not enough points to deduct. Available: " + availablePoints);
            }

            // 포인트 차감 로직 (만료일 가까운 순으로 차감)
            List<Point> availablePointsList = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(user, PointStatus.AVAILABLE);

            for (Point point : availablePointsList) {
                if (remainingToDeduct <= 0) break;

                int deducted = point.use(remainingToDeduct);
                remainingToDeduct -= deducted;
            }

            // 차감 내역 기록
            Point deductionRecord = new Point(user, requestDto.getAmount(), requestDto.getReason());
            deductionRecord.setStatus(PointStatus.USED);
            pointRepository.save(deductionRecord);
        }
    }

    /**
     * 사용자 데이터 CSV 내보내기
     */
    public byte[] exportUsersCsv(UserSearchRequestDto searchDto) {
        // 검색 사양 구성
        Specification<User> spec = Specification.where(null);

        // 키워드 검색 (이메일, 전화번호, 프로필명)
        if (searchDto.getKeyword() != null && !searchDto.getKeyword().isEmpty()) {
            String keyword = "%" + searchDto.getKeyword() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("email"), keyword),
                            cb.like(root.get("phoneNumber"), keyword),
                            cb.like(root.join("profile").get("profileName"), keyword)
                    )
            );
        }

        // 이메일 필터
        if (searchDto.getEmail() != null && !searchDto.getEmail().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("email"), "%" + searchDto.getEmail() + "%")
            );
        }

        // 전화번호 필터
        if (searchDto.getPhoneNumber() != null && !searchDto.getPhoneNumber().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + searchDto.getPhoneNumber() + "%")
            );
        }

        // 나이 범위 필터
        if (searchDto.getAgeStart() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("age"), searchDto.getAgeStart())
            );
        }
        if (searchDto.getAgeEnd() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("age"), searchDto.getAgeEnd())
            );
        }

        // 성별 필터
        if (searchDto.getGender() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("gender"), searchDto.getGender())
            );
        }

        // 가입일 범위 필터
        if (searchDto.getRegistrationDateStart() != null) {
            LocalDateTime startDateTime = searchDto.getRegistrationDateStart().atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("createdDate"), startDateTime)
            );
        }
        if (searchDto.getRegistrationDateEnd() != null) {
            LocalDateTime endDateTime = searchDto.getRegistrationDateEnd().plusDays(1).atStartOfDay();
            spec = spec.and((root, query, cb) ->
                    cb.lessThan(root.get("createdDate"), endDateTime)
            );
        }

        // 본인인증 여부 필터
        if (searchDto.getIsVerified() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isVerified"), searchDto.getIsVerified())
            );
        }

        // 판매자 등급 필터
        if (searchDto.getSellerGrade() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("sellerGrade"), searchDto.getSellerGrade())
            );
        }

        // 신발 사이즈 필터
        if (searchDto.getShoeSize() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("shoeSize"), searchDto.getShoeSize())
            );
        }

        // 권한 필터
        if (searchDto.getRole() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role"), searchDto.getRole())
            );
        }

        // 사용자 조회
        List<User> users = userRepository.findAll(spec);

        // CSV 헤더
        StringBuilder csv = new StringBuilder();
        csv.append("ID,이메일,전화번호,나이,성별,본인인증,활성상태,역할,등급,포인트,가입일\n");

        // 사용자 데이터 추가
        for (User user : users) {
            csv.append(user.getId()).append(",");
            csv.append(user.getEmail()).append(",");
            csv.append(user.getPhoneNumber()).append(",");
            csv.append(user.getAge() != null ? user.getAge() : "").append(",");
            csv.append(user.getGender() != null ? user.getGender() : "").append(",");
            csv.append(user.isVerified()).append(",");
            csv.append(user.isActive()).append(",");
            csv.append(user.getRole()).append(",");
            csv.append(user.getGrade() != null ? user.getGrade().getName() : "").append(",");
            csv.append(user.calculateTotalPoints()).append(",");
            csv.append(user.getCreatedDate()).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * User 엔티티를 UserSearchResponseDto로 변환
     */
    private UserSearchResponseDto convertToUserSearchResponseDto(User user) {
        Profile profile = profileRepository.findByUser(user).orElse(null);

        return UserSearchResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .age(user.getAge())
                .gender(user.getGender())
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .role(user.getRole())
                .sellerGrade(user.getSellerGrade())
                .profileName(profile != null ? profile.getProfileName() : null)
                .profileImageUrl(profile != null ? profile.getProfileImageUrl() : null)
                .totalPoints(user.calculateTotalPoints())
                .createdDate(user.getCreatedDate())
                .updatedDate(user.getModifiedDate())
                .build();
    }

    /**
     * User 엔티티를 UserDetailResponseDto로 변환
     */
    private UserDetailResponseDto convertToUserDetailResponseDto(User user) {
        Profile profile = profileRepository.findByUser(user).orElse(null);
        BankAccount bankAccount = bankAccountRepository.findByUser_Email(user.getEmail());

        // 활성 제재 내역 조회
        List<UserSanction> sanctions = userSanctionRepository.findByUserAndStatusOrderByStartDateDesc(user, SanctionStatus.ACTIVE);

        // 포인트 조회
        int totalPoints = user.calculateTotalPoints();

        return UserDetailResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .age(user.getAge())
                .gender(user.getGender())
                .shoeSize(user.getShoeSize())
                .isVerified(user.isVerified())
                .ci(user.getCi())
                .di(user.getDi())
                .role(user.getRole())
                .termsAgreement(user.isTermsAgreement())
                .phoneNotificationConsent(user.isPhoneNotificationConsent())
                .emailNotificationConsent(user.isEmailNotificationConsent())
                .optionalPrivacyAgreement(user.isOptionalPrivacyAgreement())
                .sellerGrade(user.getSellerGrade())
                .profile(profile != null ? convertToProfileDto(profile) : null)
                .bankAccount(bankAccount != null ? convertToBankAccountDto(bankAccount) : null)
                .sanctions(convertToSanctionBriefDtoList(sanctions))
                .totalPoints(totalPoints)
                .availablePoints(totalPoints) // 현재는 동일, 추후 가용 포인트 계산 로직 추가
                .createdDate(user.getCreatedDate())
                .updatedDate(user.getModifiedDate())
                .build();
    }

    private ProfileDto convertToProfileDto(Profile profile) {
        return ProfileDto.builder()
                .id(profile.getId())
                .profileName(profile.getProfileName())
                .name(profile.getName())
                .bio(profile.getBio())
                .isPublic(profile.isPublic())
                .profileImageUrl(profile.getProfileImageUrl())
                .followersCount((long) profile.getFollowers().size())
                .followingCount((long) profile.getFollowings().size())
                .stylesCount((long) profile.getStyles().size())
                .build();
    }

    private BankAccountDto convertToBankAccountDto(BankAccount bankAccount) {
        return BankAccountDto.builder()
                .id(bankAccount.getId())
                .bankName(bankAccount.getBankName())
                .accountNumber(bankAccount.getAccountNumber())
                .accountHolder(bankAccount.getAccountHolder())
                .build();
    }

    private List<SanctionBriefDto> convertToSanctionBriefDtoList(List<UserSanction> sanctions) {
        return sanctions.stream()
                .map(this::convertToSanctionBriefDto)
                .collect(Collectors.toList());
    }

    private SanctionBriefDto convertToSanctionBriefDto(UserSanction sanction) {
        return SanctionBriefDto.builder()
                .id(sanction.getId())
                .reason(sanction.getReason())
                .type(sanction.getType().name())
                .status(sanction.getStatus().name())
                .startDate(sanction.getStartDate())
                .endDate(sanction.getEndDate())
                .build();
    }
}
