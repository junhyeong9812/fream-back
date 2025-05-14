package com.fream.back.domain.inquiry.controller.command;

import com.fream.back.domain.inquiry.dto.InquiryAnswerRequestDto;
import com.fream.back.domain.inquiry.dto.InquiryCreateRequestDto;
import com.fream.back.domain.inquiry.dto.InquiryResponseDto;
import com.fream.back.domain.inquiry.dto.InquiryUpdateRequestDto;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.service.command.InquiryCommandService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import com.fream.back.global.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 1대1 문의 Command 컨트롤러
 * 생성, 수정, 삭제 등 상태 변경 작업 담당
 */
@RestController
@RequestMapping("/inquiry")
@RequiredArgsConstructor
@Slf4j
public class InquiryCommandController {

    private final InquiryCommandService inquiryCommandService;
    private final UserQueryService userQueryService;

    /**
     * 1대1 문의 생성
     *
     * @param requestDto 문의 생성 요청 DTO
     * @return 생성된 문의 정보
     */
    @PostMapping
    public ResponseEntity<ResponseDto<InquiryResponseDto>> createInquiry(
            @Valid @ModelAttribute InquiryCreateRequestDto requestDto) {

        // 로그인한 사용자 정보 가져오기
        String email = SecurityUtils.extractEmailFromSecurityContext();
        User user = userQueryService.findByEmail(email);
        Long userId = user.getId();

        log.info("문의 생성 요청: 사용자 ID={}, 제목={}", userId, requestDto.getTitle());

        InquiryResponseDto response = inquiryCommandService.createInquiry(userId, requestDto);

        log.info("문의 생성 완료: 문의 ID={}", response.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.success(response, "문의가 성공적으로 등록되었습니다."));
    }

    /**
     * 1대1 문의 수정
     *
     * @param inquiryId 문의 ID
     * @param requestDto 문의 수정 요청 DTO
     * @return 수정된 문의 정보
     */
    @PutMapping("/{inquiryId}")
    public ResponseEntity<ResponseDto<InquiryResponseDto>> updateInquiry(
            @PathVariable Long inquiryId,
            @Valid @ModelAttribute InquiryUpdateRequestDto requestDto) {

        // 로그인한 사용자 정보 가져오기
        String email = SecurityUtils.extractEmailFromSecurityContext();
        User user = userQueryService.findByEmail(email);
        Long userId = user.getId();

        log.info("문의 수정 요청: 문의 ID={}, 사용자 ID={}", inquiryId, userId);

        InquiryResponseDto response = inquiryCommandService.updateInquiry(inquiryId, userId, requestDto);

        log.info("문의 수정 완료: 문의 ID={}", inquiryId);

        return ResponseEntity.ok(ResponseDto.success(response, "문의가 성공적으로 수정되었습니다."));
    }

    /**
     * 1대1 문의 삭제
     *
     * @param inquiryId 문의 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<ResponseDto<Void>> deleteInquiry(@PathVariable Long inquiryId) {

        // 로그인한 사용자 정보 가져오기
        String email = SecurityUtils.extractEmailFromSecurityContext();
        User user = userQueryService.findByEmail(email);
        Long userId = user.getId();
        boolean isAdmin = user.getRole() != null && user.getRole().name().equals("ADMIN");

        log.info("문의 삭제 요청: 문의 ID={}, 사용자 ID={}, 관리자 여부={}", inquiryId, userId, isAdmin);

        inquiryCommandService.deleteInquiry(inquiryId, userId, isAdmin);

        log.info("문의 삭제 완료: 문의 ID={}", inquiryId);

        return ResponseEntity.ok(ResponseDto.success(null, "문의가 성공적으로 삭제되었습니다."));
    }

    /**
     * 문의 답변 (관리자용)
     *
     * @param inquiryId 문의 ID
     * @param requestDto 답변 작성 요청 DTO
     * @return 답변이 작성된 문의 정보
     */
    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<ResponseDto<InquiryResponseDto>> answerInquiry(
            @PathVariable Long inquiryId,
            @Valid @ModelAttribute InquiryAnswerRequestDto requestDto) {

        // 관리자 권한 확인
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        log.info("문의 답변 요청: 문의 ID={}, 관리자={}", inquiryId, email);

        InquiryResponseDto response = inquiryCommandService.answerInquiry(inquiryId, requestDto);

        log.info("문의 답변 완료: 문의 ID={}", inquiryId);

        return ResponseEntity.ok(ResponseDto.success(response, "문의 답변이 성공적으로 등록되었습니다."));
    }

    /**
     * 문의 상태 변경 (관리자용)
     *
     * @param inquiryId 문의 ID
     * @param status 변경할 상태
     * @return 상태가 변경된 문의 정보
     */
    @PutMapping("/{inquiryId}/status")
    public ResponseEntity<ResponseDto<InquiryResponseDto>> updateInquiryStatus(
            @PathVariable Long inquiryId,
            @RequestParam InquiryStatus status) {

        // 관리자 권한 확인
        String email = SecurityUtils.extractEmailFromSecurityContext();
        userQueryService.checkAdminRole(email);

        log.info("문의 상태 변경 요청: 문의 ID={}, 변경할 상태={}, 관리자={}", inquiryId, status, email);

        InquiryResponseDto response = inquiryCommandService.updateInquiryStatus(inquiryId, status);

        log.info("문의 상태 변경 완료: 문의 ID={}, 변경된 상태={}", inquiryId, status);

        return ResponseEntity.ok(ResponseDto.success(response, "문의 상태가 " + status.getDescription() + "로 변경되었습니다."));
    }
}