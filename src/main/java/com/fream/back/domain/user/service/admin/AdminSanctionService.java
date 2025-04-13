package com.fream.back.domain.user.service.admin;

import com.fream.back.domain.user.dto.SanctionDto.*;
import com.fream.back.domain.user.entity.*;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.repository.UserSanctionRepository;
import com.fream.back.domain.user.repository.UserSanctionSpecifications;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminSanctionService {

    private final UserSanctionRepository userSanctionRepository;
    private final UserRepository userRepository;

    /**
     * 제재 검색 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<SanctionResponseDto> searchSanctions(SanctionSearchRequestDto searchDto, int page, int size) {
        // 정렬 방향 설정
        Sort.Direction direction = searchDto.getSortDirection() != null &&
                searchDto.getSortDirection().equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 정렬 필드 설정 (기본값: createdDate)
        String sortField = searchDto.getSortField() != null ? searchDto.getSortField() : "createdDate";

        // 페이징 및 정렬 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        // Specification을 사용한 동적 쿼리 생성
        Specification<UserSanction> spec = UserSanctionSpecifications.withSearchCriteria(searchDto);

        // 제재 조회 및 DTO 변환
        return userSanctionRepository.findAll(spec, pageable)
                .map(this::convertToSanctionResponseDto);
    }

    /**
     * 제재 상세 조회
     */
    @Transactional(readOnly = true)
    public SanctionResponseDto getSanctionById(Long sanctionId) {
        UserSanction sanction = userSanctionRepository.findById(sanctionId)
                .orElseThrow(() -> new EntityNotFoundException("Sanction not found with id: " + sanctionId));

        return convertToSanctionResponseDto(sanction);
    }

    /**
     * 제재 생성
     */
    @Transactional
    public SanctionResponseDto createSanction(SanctionCreateRequestDto requestDto, String adminEmail) {
        // 사용자 조회
        User user = userRepository.findById(requestDto.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + requestDto.getUserId()));

        // 시작일, 종료일 설정
        LocalDateTime startDate = requestDto.getStartDate() != null ?
                requestDto.getStartDate() :
                LocalDateTime.now();

        LocalDateTime endDate = requestDto.getEndDate();

        // 영구 정지가 아닌 경우 종료일 필수
        if (requestDto.getType() != SanctionType.PERMANENT_BAN && endDate == null) {
            throw new InvalidRequestException("End date is required for non-permanent sanctions");
        }

        // 제재 생성
        UserSanction sanction = UserSanction.builder()
                .user(user)
                .reason(requestDto.getReason())
                .details(requestDto.getDetails())
                .type(requestDto.getType())
                .status(SanctionStatus.PENDING) // 기본 상태는 승인 대기중
                .startDate(startDate)
                .endDate(endDate)
                .targetId(requestDto.getTargetId())
                .targetType(requestDto.getTargetType())
                .createdBy(adminEmail)
                .build();

        userSanctionRepository.save(sanction);

        // 제재 유형에 따른 사용자 상태 변경 (즉시 승인 시)
        if (requestDto.getType() == SanctionType.PERMANENT_BAN ||
                requestDto.getType() == SanctionType.TEMPORARY_BAN) {
            // 자동 승인 처리를 원한다면 여기에 코드 추가
            // sanction.approve(adminEmail);
            // user.setActive(false);
        }

        return convertToSanctionResponseDto(sanction);
    }

    /**
     * 제재 수정
     */
    @Transactional
    public SanctionResponseDto updateSanction(Long sanctionId, SanctionUpdateRequestDto requestDto) {
        UserSanction sanction = userSanctionRepository.findById(sanctionId)
                .orElseThrow(() -> new EntityNotFoundException("Sanction not found with id: " + sanctionId));

        // PENDING 상태일 때만 수정 가능
        if (sanction.getStatus() != SanctionStatus.PENDING) {
            throw new InvalidRequestException("Only pending sanctions can be updated");
        }

        // 사유 변경
        if (requestDto.getReason() != null) {
            sanction.setReason(requestDto.getReason());
        }

        // 상세 내용 변경
        if (requestDto.getDetails() != null) {
            sanction.setDetails(requestDto.getDetails());
        }

        // 제재 유형 변경
        if (requestDto.getType() != null) {
            sanction.setType(requestDto.getType());
        }

        // 시작일 변경
        if (requestDto.getStartDate() != null) {
            sanction.setStartDate(requestDto.getStartDate());
        }

        // 종료일 변경
        if (requestDto.getEndDate() != null ||
                (requestDto.getType() == SanctionType.PERMANENT_BAN && sanction.getEndDate() != null)) {
            sanction.setEndDate(requestDto.getEndDate());
        }

        return convertToSanctionResponseDto(sanction);
    }

    /**
     * 제재 승인/거부
     */
    @Transactional
    public SanctionResponseDto reviewSanction(Long sanctionId, SanctionReviewRequestDto requestDto, String adminEmail) {
        UserSanction sanction = userSanctionRepository.findById(sanctionId)
                .orElseThrow(() -> new EntityNotFoundException("Sanction not found with id: " + sanctionId));

        // PENDING 상태일 때만 승인/거부 가능
        if (sanction.getStatus() != SanctionStatus.PENDING) {
            throw new InvalidRequestException("Only pending sanctions can be reviewed");
        }

        User user = sanction.getUser();

        // 승인/거부 처리
        if (requestDto.getApproved()) {
            // 승인
            sanction.approve(adminEmail);

            // 제재 유형에 따른 사용자 상태 변경
            if (sanction.getType() == SanctionType.PERMANENT_BAN ||
                    sanction.getType() == SanctionType.TEMPORARY_BAN) {
                user.addActive(false);
            }
        } else {
            // 거부 (거부 사유 필수)
            if (requestDto.getRejectionReason() == null || requestDto.getRejectionReason().isEmpty()) {
                throw new InvalidRequestException("Rejection reason is required");
            }

            sanction.reject(adminEmail, requestDto.getRejectionReason());
        }

        return convertToSanctionResponseDto(sanction);
    }

    /**
     * 제재 취소
     */
    @Transactional
    public SanctionResponseDto cancelSanction(Long sanctionId) {
        UserSanction sanction = userSanctionRepository.findById(sanctionId)
                .orElseThrow(() -> new EntityNotFoundException("Sanction not found with id: " + sanctionId));

        // ACTIVE 상태일 때만 취소 가능
        if (sanction.getStatus() != SanctionStatus.ACTIVE) {
            throw new InvalidRequestException("Only active sanctions can be canceled");
        }

        User user = sanction.getUser();

        // 제재 취소
        sanction.cancel();

        // 사용자 상태 복구 (다른 활성 제재가 없는 경우)
        if ((sanction.getType() == SanctionType.PERMANENT_BAN ||
                sanction.getType() == SanctionType.TEMPORARY_BAN) &&
                !hasOtherActiveBanSanctions(user, sanction.getId())) {
            user.addActive(true);
        }

        return convertToSanctionResponseDto(sanction);
    }

    /**
     * 특정 사용자의 제재 내역 조회
     */
    @Transactional(readOnly = true)
    public List<SanctionResponseDto> getUserSanctions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        List<UserSanction> sanctions = userSanctionRepository.findByUserOrderByCreatedDateDesc(user);

        return sanctions.stream()
                .map(this::convertToSanctionResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 제재 통계 조회
     */
    @Transactional(readOnly = true)
    public SanctionStatisticsDto getSanctionStatistics() {
        long totalCount = userSanctionRepository.countTotalSanctions();
        long activeCount = userSanctionRepository.countActiveSanctions();
        long expiredCount = userSanctionRepository.countExpiredSanctions();
        long pendingCount = userSanctionRepository.countPendingSanctions();

        return SanctionStatisticsDto.builder()
                .total(totalCount)
                .active(activeCount)
                .expired(expiredCount)
                .pending(pendingCount)
                .build();
    }

    /**
     * 만료된 제재 처리 (배치 작업용)
     */
    @Transactional
    public int processExpiredSanctions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSanction> expirableSanctions = userSanctionRepository.findExpirableSanctions(now);

        int processedCount = 0;

        for (UserSanction sanction : expirableSanctions) {
            sanction.expire();

            // 사용자 상태 복구 (임시 정지 제재)
            if (sanction.getType() == SanctionType.TEMPORARY_BAN &&
                    !hasOtherActiveBanSanctions(sanction.getUser(), sanction.getId())) {
                sanction.getUser().addActive(true);
            }

            processedCount++;
        }

        return processedCount;
    }

    /**
     * 사용자에게 다른 활성 밴 제재가 있는지 확인
     */
    private boolean hasOtherActiveBanSanctions(User user, Long excludeSanctionId) {
        List<UserSanction> activeSanctions = userSanctionRepository.findByUserAndStatusOrderByStartDateDesc(user, SanctionStatus.ACTIVE);

        return activeSanctions.stream()
                .anyMatch(s -> !s.getId().equals(excludeSanctionId) &&
                        (s.getType() == SanctionType.PERMANENT_BAN ||
                                s.getType() == SanctionType.TEMPORARY_BAN));
    }

    /**
     * UserSanction 엔티티를 SanctionResponseDto로 변환
     */
    private SanctionResponseDto convertToSanctionResponseDto(UserSanction sanction) {
        return SanctionResponseDto.builder()
                .id(sanction.getId())
                .userId(sanction.getUser().getId())
                .userEmail(sanction.getUser().getEmail())
                .userProfileName(sanction.getUser().getProfile() != null ?
                        sanction.getUser().getProfile().getProfileName() : null)
                .targetId(sanction.getTargetId())
                .targetType(sanction.getTargetType())
                .reason(sanction.getReason())
                .details(sanction.getDetails())
                .type(sanction.getType())
                .status(sanction.getStatus())
                .startDate(sanction.getStartDate())
                .endDate(sanction.getEndDate())
                .approvedBy(sanction.getApprovedBy())
                .rejectedBy(sanction.getRejectedBy())
                .rejectionReason(sanction.getRejectionReason())
                .createdBy(sanction.getCreatedBy())
                .createdDate(sanction.getCreatedDate())
                .updatedDate(sanction.getModifiedDate())
                .build();
    }

    /**
     * UserSanction에 대한 편의 메서드
     */
    public static class SanctionUtils {
        // reason 설정 메서드
        public static void setReason(UserSanction sanction, String reason) {
            sanction.setReason(reason);
        }

        // details 설정 메서드
        public static void setDetails(UserSanction sanction, String details) {
            sanction.setDetails(details);
        }

        // type 설정 메서드
        public static void setType(UserSanction sanction, SanctionType type) {
            sanction.setType(type);
        }

        // startDate 설정 메서드
        public static void setStartDate(UserSanction sanction, LocalDateTime startDate) {
            sanction.setStartDate(startDate);
        }

        // endDate 설정 메서드
        public static void setEndDate(UserSanction sanction, LocalDateTime endDate) {
            sanction.setEndDate(endDate);
        }
    }
}
