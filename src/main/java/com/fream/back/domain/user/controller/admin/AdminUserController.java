package com.fream.back.domain.user.controller.admin;

import com.fream.back.domain.user.dto.UserManagementDto.*;
import com.fream.back.domain.user.service.admin.AdminUserService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final UserQueryService userQueryService; // 권한 확인 서비스
    // SecurityContext에서 이메일 추출
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal(); // 이메일 반환
        }
        throw new IllegalStateException("인증된 사용자가 없습니다.");
    }
    /**
     * 사용자 검색 (페이징)
     */
    @GetMapping("/search")
    public ResponseEntity<Page<UserSearchResponseDto>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Integer ageStart,
            @RequestParam(required = false) Integer ageEnd,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String registrationDateStart,
            @RequestParam(required = false) String registrationDateEnd,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Integer sellerGrade,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) String role,
            @RequestParam(required = false, defaultValue = "createdDate") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String adminEmail = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(adminEmail); // 관리자 권한 확인
        UserSearchRequestDto searchDto = UserSearchRequestDto.builder()
                .keyword(keyword)
                .email(email)
                .phoneNumber(phoneNumber)
                .ageStart(ageStart)
                .ageEnd(ageEnd)
                .gender(gender != null ? com.fream.back.domain.user.entity.Gender.valueOf(gender) : null)
                .registrationDateStart(registrationDateStart != null ? LocalDate.parse(registrationDateStart, dateFormatter) : null)
                .registrationDateEnd(registrationDateEnd != null ? LocalDate.parse(registrationDateEnd, dateFormatter) : null)
                .isVerified(isVerified)
                .sellerGrade(sellerGrade)
                .shoeSize(shoeSize != null ? com.fream.back.domain.user.entity.ShoeSize.valueOf(shoeSize) : null)
                .role(role != null ? com.fream.back.domain.user.entity.Role.valueOf(role) : null)
                .sortField(sort)
                .sortDirection(direction)
                .build();

        Page<UserSearchResponseDto> result = adminUserService.searchUsers(searchDto, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 사용자 상세 정보 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponseDto> getUserDetail(@PathVariable Long userId) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        UserDetailResponseDto userDetail = adminUserService.getUserDetail(userId);
        return ResponseEntity.ok(userDetail);
    }

    /**
     * 사용자 상태 업데이트 (활성/비활성)
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserDetailResponseDto> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateRequestDto requestDto) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        // userId를 요청 DTO에 설정
        if (requestDto.getUserId() == null) {
            requestDto = UserStatusUpdateRequestDto.builder()
                    .userId(userId)
                    .status(requestDto.getStatus())
                    .reason(requestDto.getReason())
                    .build();
        }

        UserDetailResponseDto updatedUser = adminUserService.updateUserStatus(requestDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 사용자 등급 업데이트
     */
    @PatchMapping("/{userId}/grade")
    public ResponseEntity<UserDetailResponseDto> updateUserGrade(
            @PathVariable Long userId,
            @RequestBody UserGradeUpdateRequestDto requestDto) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        // userId를 요청 DTO에 설정
        if (requestDto.getUserId() == null) {
            requestDto = UserGradeUpdateRequestDto.builder()
                    .userId(userId)
                    .gradeId(requestDto.getGradeId())
                    .reason(requestDto.getReason())
                    .build();
        }

        UserDetailResponseDto updatedUser = adminUserService.updateUserGrade(requestDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 사용자 역할 업데이트
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserDetailResponseDto> updateUserRole(
            @PathVariable Long userId,
            @RequestBody UserRoleUpdateRequestDto requestDto) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        // userId를 요청 DTO에 설정
        if (requestDto.getUserId() == null) {
            requestDto = UserRoleUpdateRequestDto.builder()
                    .userId(userId)
                    .role(requestDto.getRole())
                    .reason(requestDto.getReason())
                    .build();
        }

        UserDetailResponseDto updatedUser = adminUserService.updateUserRole(requestDto);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * 사용자 포인트 지급/차감
     */
    @PostMapping("/{userId}/points")
    public ResponseEntity<Void> manageUserPoints(
            @PathVariable Long userId,
            @RequestBody UserPointRequestDto requestDto) {
        String email = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email); // 관리자 권한 확인
        // userId를 요청 DTO에 설정
        if (requestDto.getUserId() == null) {
            requestDto = UserPointRequestDto.builder()
                    .userId(userId)
                    .amount(requestDto.getAmount())
                    .reason(requestDto.getReason())
                    .expirationDate(requestDto.getExpirationDate())
                    .build();
        }

        adminUserService.manageUserPoints(requestDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 데이터 CSV 내보내기
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUserData(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) Integer ageStart,
            @RequestParam(required = false) Integer ageEnd,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String registrationDateStart,
            @RequestParam(required = false) String registrationDateEnd,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) Integer sellerGrade,
            @RequestParam(required = false) String shoeSize,
            @RequestParam(required = false) String role) {
        String adminEmail = extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(adminEmail); // 관리자 권한 확인
        UserSearchRequestDto searchDto = UserSearchRequestDto.builder()
                .keyword(keyword)
                .email(email)
                .phoneNumber(phoneNumber)
                .ageStart(ageStart)
                .ageEnd(ageEnd)
                .gender(gender != null ? com.fream.back.domain.user.entity.Gender.valueOf(gender) : null)
                .registrationDateStart(registrationDateStart != null ? LocalDate.parse(registrationDateStart, dateFormatter) : null)
                .registrationDateEnd(registrationDateEnd != null ? LocalDate.parse(registrationDateEnd, dateFormatter) : null)
                .isVerified(isVerified)
                .sellerGrade(sellerGrade)
                .shoeSize(shoeSize != null ? com.fream.back.domain.user.entity.ShoeSize.valueOf(shoeSize) : null)
                .role(role != null ? com.fream.back.domain.user.entity.Role.valueOf(role) : null)
                .build();

        byte[] csvData = adminUserService.exportUsersCsv(searchDto);
        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment; filename=users_export.csv")
                .header("Content-Type", "text/csv; charset=UTF-8")
                .body(csvData);
    }
}