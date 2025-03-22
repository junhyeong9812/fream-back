package com.fream.back.domain.inspection.controller.command;

import com.fream.back.domain.inspection.dto.InspectionStandardCreateRequestDto;
import com.fream.back.domain.inspection.dto.InspectionStandardResponseDto;
import com.fream.back.domain.inspection.dto.InspectionStandardUpdateRequestDto;
import com.fream.back.domain.inspection.exception.InspectionErrorCode;
import com.fream.back.domain.inspection.exception.InspectionException;
import com.fream.back.domain.inspection.exception.InspectionFileException;
import com.fream.back.domain.inspection.exception.InspectionNotFoundException;
import com.fream.back.domain.inspection.exception.InspectionPermissionException;
import com.fream.back.domain.inspection.service.command.InspectionStandardCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/inspections")
@RequiredArgsConstructor
@Slf4j
public class InspectionCommandController {

    private final InspectionStandardCommandService commandService;
    private final UserQueryService userQueryService;

    /**
     * SecurityContext에서 사용자 이메일 추출
     */
    private String extractEmailFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("보안 컨텍스트에 인증 정보가 없습니다.");
            throw new InspectionPermissionException("인증 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }

        if (!(authentication.getPrincipal() instanceof String)) {
            log.warn("인증 주체가 예상 타입(String)이 아닙니다: {}",
                    authentication.getPrincipal() != null
                            ? authentication.getPrincipal().getClass().getName()
                            : "null");
            throw new InspectionPermissionException("인증 정보에 문제가 있습니다. 다시 로그인해주세요.");
        }

        String email = (String) authentication.getPrincipal();
        if (email == null || email.isEmpty()) {
            log.warn("보안 컨텍스트에서 이메일이 비어있습니다.");
            throw new InspectionPermissionException("이메일 정보를 찾을 수 없습니다. 다시 로그인해주세요.");
        }

        return email;
    }

    /**
     * 검수 기준 생성 API
     */
    @PostMapping
    public ResponseEntity<ResponseDto<InspectionStandardResponseDto>> createStandard(
            @ModelAttribute InspectionStandardCreateRequestDto requestDto) {
        String email = null;

        try {
            // 요청 기본 검증
            if (requestDto == null) {
                throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_REQUEST_DATA,
                        "검수 기준 데이터가 필요합니다.");
            }

            // 인증 정보 추출
            email = extractEmailFromSecurityContext();
            log.info("검수 기준 생성 시도: 사용자={}", email);

            // 관리자 권한 확인
            try {
                userQueryService.checkAdminRole(email);
            } catch (AccessDeniedException e) {
                log.warn("관리자 권한 없는 사용자의 검수 기준 생성 시도: {}", email);
                throw new InspectionPermissionException("검수 기준 생성은 관리자만 가능합니다.", e);
            }

            // 검수 기준 생성 서비스 호출
            InspectionStandardResponseDto response = commandService.createStandard(requestDto);
            log.info("검수 기준 생성 완료: ID={}, 사용자={}", response.getId(), email);

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (InspectionNotFoundException e) {
            // 리소스를 찾을 수 없는 경우 (검수 기준 생성에서는 발생 가능성이 낮음)
            log.warn("검수 기준 생성 중 리소스를 찾을 수 없음: {}", e.getMessage());
            throw e;
        } catch (InspectionPermissionException e) {
            // 권한 관련 예외
            log.warn("검수 기준 생성 권한 없음: 사용자={}, 메시지={}", email, e.getMessage());
            throw e;
        } catch (InspectionFileException e) {
            // 파일 처리 예외
            log.error("검수 기준 생성 중 파일 오류: 사용자={}, 메시지={}", email, e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 기준 관련 예외
            log.error("검수 기준 생성 중 오류: 사용자={}, 메시지={}", email, e.getMessage());
            throw e;
        } catch (IOException e) {
            // IOException을 InspectionFileException으로 변환
            log.error("검수 기준 생성 중 IO 오류: 사용자={}", email, e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                    "파일 처리 중 오류가 발생했습니다. 파일 크기와 형식을 확인해주세요.", e);
        } catch (Exception e) {
            // 예상치 못한 예외
            log.error("검수 기준 생성 중 예상치 못한 오류: 사용자={}", email, e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_SAVE_ERROR,
                    "검수 기준 생성 중 오류가 발생했습니다. 관리자에게 문의해주세요.", e);
        }
    }

    /**
     * 검수 기준 수정 API
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseDto<InspectionStandardResponseDto>> updateStandard(
            @PathVariable("id") Long id,
            @ModelAttribute InspectionStandardUpdateRequestDto requestDto) {
        String email = null;

        try {
            // 요청 기본 검증
            if (id == null) {
                throw new InspectionNotFoundException("수정할 검수 기준 ID가 필요합니다.");
            }

            if (requestDto == null) {
                throw new InspectionException(InspectionErrorCode.INSPECTION_INVALID_REQUEST_DATA,
                        "수정할 검수 기준 데이터가 필요합니다.");
            }

            // 인증 정보 추출
            email = extractEmailFromSecurityContext();
            log.info("검수 기준 수정 시도: ID={}, 사용자={}", id, email);

            // 관리자 권한 확인
            try {
                userQueryService.checkAdminRole(email);
            } catch (AccessDeniedException e) {
                log.warn("관리자 권한 없는 사용자의 검수 기준 수정 시도: {}", email);
                throw new InspectionPermissionException("검수 기준 수정은 관리자만 가능합니다.", e);
            }

            // 검수 기준 수정 서비스 호출
            InspectionStandardResponseDto response = commandService.updateStandard(id, requestDto);
            log.info("검수 기준 수정 완료: ID={}, 사용자={}", id, email);

            return ResponseEntity.ok(ResponseDto.success(response));
        } catch (InspectionNotFoundException e) {
            // 리소스를 찾을 수 없는 경우
            log.warn("검수 기준 수정 중 기준을 찾을 수 없음: ID={}, 사용자={}", id, email);
            throw e;
        } catch (InspectionPermissionException e) {
            // 권한 관련 예외
            log.warn("검수 기준 수정 권한 없음: ID={}, 사용자={}, 메시지={}", id, email, e.getMessage());
            throw e;
        } catch (InspectionFileException e) {
            // 파일 처리 예외
            log.error("검수 기준 수정 중 파일 오류: ID={}, 사용자={}, 메시지={}", id, email, e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 기준 관련 예외
            log.error("검수 기준 수정 중 오류: ID={}, 사용자={}, 메시지={}", id, email, e.getMessage());
            throw e;
        } catch (IOException e) {
            // IOException을 InspectionFileException으로 변환
            log.error("검수 기준 수정 중 IO 오류: ID={}, 사용자={}", id, email, e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_SAVE_ERROR,
                    "파일 처리 중 오류가 발생했습니다. 파일 크기와 형식을 확인해주세요.", e);
        } catch (Exception e) {
            // 예상치 못한 예외
            log.error("검수 기준 수정 중 예상치 못한 오류: ID={}, 사용자={}", id, email, e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_UPDATE_ERROR,
                    "검수 기준 수정 중 오류가 발생했습니다. 관리자에게 문의해주세요.", e);
        }
    }

    /**
     * 검수 기준 삭제 API
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteStandard(@PathVariable("id") Long id) {
        String email = null;

        try {
            // 요청 기본 검증
            if (id == null) {
                throw new InspectionNotFoundException("삭제할 검수 기준 ID가 필요합니다.");
            }

            // 인증 정보 추출
            email = extractEmailFromSecurityContext();
            log.info("검수 기준 삭제 시도: ID={}, 사용자={}", id, email);

            // 관리자 권한 확인
            try {
                userQueryService.checkAdminRole(email);
            } catch (AccessDeniedException e) {
                log.warn("관리자 권한 없는 사용자의 검수 기준 삭제 시도: {}", email);
                throw new InspectionPermissionException("검수 기준 삭제는 관리자만 가능합니다.", e);
            }

            // 검수 기준 삭제 서비스 호출
            commandService.deleteStandard(id);
            log.info("검수 기준 삭제 완료: ID={}, 사용자={}", id, email);

            return ResponseEntity.ok(ResponseDto.success(null));
        } catch (InspectionNotFoundException e) {
            // 리소스를 찾을 수 없는 경우
            log.warn("검수 기준 삭제 중 기준을 찾을 수 없음: ID={}, 사용자={}", id, email);
            throw e;
        } catch (InspectionPermissionException e) {
            // 권한 관련 예외
            log.warn("검수 기준 삭제 권한 없음: ID={}, 사용자={}, 메시지={}", id, email, e.getMessage());
            throw e;
        } catch (InspectionFileException e) {
            // 파일 처리 예외
            log.error("검수 기준 삭제 중 파일 오류: ID={}, 사용자={}, 메시지={}", id, email, e.getMessage());
            throw e;
        } catch (InspectionException e) {
            // 기타 검수 기준 관련 예외
            log.error("검수 기준 삭제 중 오류: ID={}, 사용자={}, 메시지={}", id, email, e.getMessage());
            throw e;
        } catch (IOException e) {
            // IOException을 InspectionFileException으로 변환
            log.error("검수 기준 삭제 중 IO 오류: ID={}, 사용자={}", id, email, e);
            throw new InspectionFileException(InspectionErrorCode.INSPECTION_FILE_DELETE_ERROR,
                    "파일 삭제 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            // 예상치 못한 예외
            log.error("검수 기준 삭제 중 예상치 못한 오류: ID={}, 사용자={}", id, email, e);
            throw new InspectionException(InspectionErrorCode.INSPECTION_DELETE_ERROR,
                    "검수 기준 삭제 중 오류가 발생했습니다. 관리자에게 문의해주세요.", e);
        }
    }
}