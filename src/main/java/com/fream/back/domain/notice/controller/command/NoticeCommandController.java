package com.fream.back.domain.notice.controller.command;

import com.fream.back.domain.notice.dto.NoticeCreateRequestDto;
import com.fream.back.domain.notice.dto.NoticeResponseDto;
import com.fream.back.domain.notice.dto.NoticeUpdateRequestDto;
import com.fream.back.domain.notice.entity.NoticeCategory;
import com.fream.back.domain.notice.exception.NoticeErrorCode;
import com.fream.back.domain.notice.exception.NoticeException;
import com.fream.back.domain.notice.exception.NoticeFileException;
import com.fream.back.domain.notice.exception.NoticeNotFoundException;
import com.fream.back.domain.notice.exception.NoticePermissionException;
import com.fream.back.domain.notice.service.command.NoticeCommandService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
@Slf4j
public class NoticeCommandController {

    private final NoticeCommandService noticeCommandService;
    private final UserQueryService userQueryService; // 권한 확인 서비스

    /**
     * 보안 컨텍스트에서 사용자 이메일 추출
     */
    private String extractEmailFromSecurityContext() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                throw new NoticePermissionException("인증 정보가 없습니다.");
            }

            if (authentication.getPrincipal() instanceof String) {
                return (String) authentication.getPrincipal();
            }

            throw new NoticePermissionException("인증된 사용자 정보를 찾을 수 없습니다.");
        } catch (NoticePermissionException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 인증 정보 추출 중 오류 발생: ", e);
            throw new NoticePermissionException("사용자 인증 정보 추출 중 오류가 발생했습니다.");
        }
    }

    /**
     * 관리자 권한 검증
     */
    private void checkAdminPermission(String email) {
        try {
            userQueryService.checkAdminRole(email);
        } catch (Exception e) {
            log.error("관리자 권한 확인 중 오류 발생: email={}", email, e);
            throw new NoticePermissionException("공지사항 관리는 관리자만 가능합니다.");
        }
    }

    /**
     * 공지사항 생성 요청 검증
     */
    private void validateCreateRequest(NoticeCreateRequestDto requestDto) {
        if (requestDto == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "요청 데이터가 없습니다.");
        }

        if (requestDto.getTitle() == null || requestDto.getTitle().trim().isEmpty()) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "제목은 필수입니다.");
        }

        if (requestDto.getContent() == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "내용은 필수입니다.");
        }

        if (requestDto.getCategory() == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_CATEGORY, "카테고리는 필수입니다.");
        }
    }

    /**
     * 공지사항 수정 요청 검증
     */
    private void validateUpdateRequest(Long noticeId, NoticeUpdateRequestDto requestDto) {
        if (noticeId == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "공지사항 ID는 필수입니다.");
        }

        if (requestDto == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "요청 데이터가 없습니다.");
        }

        if (requestDto.getTitle() == null || requestDto.getTitle().trim().isEmpty()) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "제목은 필수입니다.");
        }

        if (requestDto.getContent() == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "내용은 필수입니다.");
        }

        if (requestDto.getCategory() == null) {
            throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_CATEGORY, "카테고리는 필수입니다.");
        }
    }

    /**
     * 공지사항 생성 API
     */
    @PostMapping
    public ResponseEntity<NoticeResponseDto> createNotice(@ModelAttribute NoticeCreateRequestDto requestDto) throws IOException {
        log.debug("공지사항 생성 API 요청: {}", requestDto);

        try {
            // 관리자 권한 확인
            String email = extractEmailFromSecurityContext();
            checkAdminPermission(email);

            // 요청 데이터 검증
            validateCreateRequest(requestDto);

            // 공지사항 생성 처리
            NoticeResponseDto response = noticeCommandService.createNotice(requestDto);

            log.info("공지사항 생성 API 완료: id={}, title={}", response.getId(), response.getTitle());
            return ResponseEntity.ok(response);
        } catch (NoticePermissionException e) {
            // 권한 관련 예외
            log.warn("공지사항 생성 권한 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            log.error("공지사항 생성 중 파일 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외
            log.error("공지사항 생성 중 오류: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("공지사항 생성 중 파일 IO 오류: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                    "파일 저장 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("공지사항 생성 중 예상치 못한 오류: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_SAVE_ERROR,
                    "공지사항 생성 중 시스템 오류가 발생했습니다.");
        }
    }

    /**
     * 공지사항 수정 API
     */
    @PutMapping("/{noticeId}")
    public ResponseEntity<NoticeResponseDto> updateNotice(
            @PathVariable("noticeId") Long noticeId,
            @ModelAttribute NoticeUpdateRequestDto requestDto
    ) throws IOException {
        log.debug("공지사항 수정 API 요청: id={}, {}", noticeId, requestDto);

        try {
            // 관리자 권한 확인
            String email = extractEmailFromSecurityContext();
            checkAdminPermission(email);

            // 요청 데이터 검증
            validateUpdateRequest(noticeId, requestDto);

            // 공지사항 수정 처리
            NoticeResponseDto response = noticeCommandService.updateNotice(noticeId, requestDto);

            log.info("공지사항 수정 API 완료: id={}, title={}", response.getId(), response.getTitle());
            return ResponseEntity.ok(response);
        } catch (NoticePermissionException e) {
            // 권한 관련 예외
            log.warn("공지사항 수정 권한 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeNotFoundException e) {
            // 공지사항을 찾을 수 없는 경우
            log.warn("수정할 공지사항을 찾을 수 없음: id={}, error={}", noticeId, e.getMessage());
            throw e;
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            log.error("공지사항 수정 중 파일 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외
            log.error("공지사항 수정 중 오류: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("공지사항 수정 중 파일 IO 오류: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_SAVE_ERROR,
                    "파일 저장 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("공지사항 수정 중 예상치 못한 오류: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_UPDATE_ERROR,
                    "공지사항 수정 중 시스템 오류가 발생했습니다.");
        }
    }

    /**
     * 공지사항 삭제 API
     */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable("noticeId") Long noticeId) throws IOException {
        log.debug("공지사항 삭제 API 요청: id={}", noticeId);

        try {
            // 관리자 권한 확인
            String email = extractEmailFromSecurityContext();
            checkAdminPermission(email);

            // ID 검증
            if (noticeId == null) {
                throw new NoticeException(NoticeErrorCode.NOTICE_INVALID_REQUEST_DATA, "공지사항 ID는 필수입니다.");
            }

            // 공지사항 삭제 처리
            noticeCommandService.deleteNotice(noticeId);

            log.info("공지사항 삭제 API 완료: id={}", noticeId);
            return ResponseEntity.noContent().build();
        } catch (NoticePermissionException e) {
            // 권한 관련 예외
            log.warn("공지사항 삭제 권한 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeNotFoundException e) {
            // 공지사항을 찾을 수 없는 경우
            log.warn("삭제할 공지사항을 찾을 수 없음: id={}, error={}", noticeId, e.getMessage());
            throw e;
        } catch (NoticeFileException e) {
            // 파일 관련 예외
            log.error("공지사항 삭제 중 파일 오류: {}", e.getMessage());
            throw e;
        } catch (NoticeException e) {
            // 기타 공지사항 관련 예외
            log.error("공지사항 삭제 중 오류: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("공지사항 삭제 중 파일 IO 오류: ", e);
            throw new NoticeFileException(NoticeErrorCode.NOTICE_FILE_DELETE_ERROR,
                    "파일 삭제 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("공지사항 삭제 중 예상치 못한 오류: ", e);
            throw new NoticeException(NoticeErrorCode.NOTICE_DELETE_ERROR,
                    "공지사항 삭제 중 시스템 오류가 발생했습니다.");
        }
    }
}