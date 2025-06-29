package com.fream.back.domain.style.controller.query;

import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.kafka.StyleViewEventProducer;
import com.fream.back.domain.style.service.query.StyleQueryService;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.global.config.security.JwtAuthenticationFilter;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@RestController
@RequestMapping("/styles/queries")
@RequiredArgsConstructor
public class StyleQueryController {

    private final StyleQueryService styleQueryService;
    private final StyleViewEventProducer styleViewEventProducer;

    /**
     * 스타일 상세 정보 조회 API
     * 사용자 정보를 추출하여 스타일 조회 이벤트도 발행합니다.
     *
     * @param styleId 스타일 ID
     * @return 스타일 상세 정보
     */
    @GetMapping("/{styleId}")
    public ResponseEntity<StyleDetailResponseDto> getStyleDetail(
            @PathVariable("styleId") Long styleId) {
        log.info("스타일 상세 정보 조회 요청: styleId={}", styleId);

        // 현재 로그인한 사용자의 이메일 추출 (없는 경우 "anonymous")
        String email = "anonymous";

        try {
            email = SecurityUtils.extractEmailOrAnonymous();
            log.debug("사용자 이메일 추출 성공: email={}", email);
        } catch (Exception e) {
            log.warn("이메일 추출 중 오류 발생: {}, 기본값 'anonymous' 사용", e.getMessage());
        }

        // 스타일 상세 정보 조회
        StyleDetailResponseDto detail;
        try {
            detail = styleQueryService.getStyleDetail(styleId, email);
            log.debug("스타일 상세 정보 조회 성공: styleId={}", styleId);
        } catch (StyleException e) {
            log.error("스타일 상세 정보 조회 실패: styleId={}, 코드={}, 메시지={}",
                    styleId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("스타일 상세 정보 조회 중 예상치 못한 오류 발생: styleId={}", styleId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 상세 정보 조회 중 오류가 발생했습니다.", e);
        }

        // 사용자 정보 수집 (로그인 시)
        Integer age = 0;
        Gender gender = Gender.OTHER;

        if (!"anonymousUser".equals(email) && !"anonymous".equals(email)) {
            try {
                // SecurityContext에서 사용자 정보 추출
                JwtAuthenticationFilter.UserInfo userInfo = SecurityUtils.extractUserInfo();
                if (userInfo != null) {
                    age = (userInfo.getAge() == null) ? 0 : userInfo.getAge();
                    gender = (userInfo.getGender() == null) ? Gender.OTHER : userInfo.getGender();
                    log.debug("사용자 상세 정보 추출 성공: email={}, age={}, gender={}", email, age, gender);
                }
            } catch (Exception e) {
                log.warn("사용자 상세 정보 추출 중 오류 발생: {}, 기본값 사용 (age=0, gender=OTHER)", e.getMessage());
            }
        }

        // 스타일 조회 이벤트 발행 (비동기 처리)
        try {
            styleViewEventProducer.sendViewEvent(styleId, email, age, gender);
            log.debug("스타일 조회 이벤트 발행 성공: styleId={}, email={}", styleId, email);
        } catch (Exception e) {
            log.error("스타일 뷰 이벤트 발행 실패: {}", e.getMessage(), e);
            // 이벤트 발행 실패는 사용자 경험에 영향을 주지 않도록 함
        }

        log.info("스타일 상세 정보 조회 응답 완료: styleId={}", styleId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 필터링된 스타일 목록 조회 API
     *
     * @param filterRequestDto 필터 조건
     * @param pageable 페이징 정보
     * @return 필터링된 스타일 목록
     */
    @GetMapping
    public ResponseEntity<Page<StyleResponseDto>> getFilteredStyles(
            @ModelAttribute StyleFilterRequestDto filterRequestDto,
            Pageable pageable
    ) {
        log.info("필터링된 스타일 목록 조회 요청: 필터={}, 페이지={}, 사이즈={}",
                filterRequestDto, pageable.getPageNumber(), pageable.getPageSize());

        // 현재 로그인한 사용자의 이메일 추출 (없는 경우 null)
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.debug("사용자 이메일 추출 결과: {}", (email != null ? email : "비로그인 사용자"));

        Page<StyleResponseDto> styles;
        try {
            styles = styleQueryService.getFilteredStyles(filterRequestDto, pageable, email);
            log.debug("필터링된 스타일 목록 조회 성공: 결과 수={}, 총 페이지={}",
                    styles.getNumberOfElements(), styles.getTotalPages());
        } catch (StyleException e) {
            log.error("필터링된 스타일 목록 조회 실패: 코드={}, 메시지={}",
                    e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("필터링된 스타일 목록 조회 중 예상치 못한 오류 발생", e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "스타일 목록 조회 중 오류가 발생했습니다.", e);
        }

        log.info("필터링된 스타일 목록 조회 응답 완료: 결과 수={}", styles.getNumberOfElements());
        return ResponseEntity.ok(styles);
    }

    /**
     * 프로필별 스타일 목록 조회 API
     *
     * @param profileId 프로필 ID
     * @param pageable 페이징 정보
     * @return 프로필별 스타일 목록
     */
    @GetMapping("/profile/{profileId}")
    public ResponseEntity<Page<ProfileStyleResponseDto>> getStylesByProfile(
            @PathVariable("profileId") Long profileId,
            Pageable pageable
    ) {
        log.info("프로필별 스타일 목록 조회 요청: profileId={}, 페이지={}, 사이즈={}",
                profileId, pageable.getPageNumber(), pageable.getPageSize());

        // 현재 로그인한 사용자의 이메일 추출 (없는 경우 null)
        String email = SecurityUtils.extractEmailFromSecurityContext();
        log.debug("사용자 이메일 추출 결과: {}", (email != null ? email : "비로그인 사용자"));

        Page<ProfileStyleResponseDto> styles;
        try {
            styles = styleQueryService.getStylesByProfile(profileId, pageable, email);
            log.debug("프로필별 스타일 목록 조회 성공: 결과 수={}, 총 페이지={}",
                    styles.getNumberOfElements(), styles.getTotalPages());
        } catch (StyleException e) {
            log.error("프로필별 스타일 목록 조회 실패: profileId={}, 코드={}, 메시지={}",
                    profileId, e.getErrorCode().getCode(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("프로필별 스타일 목록 조회 중 예상치 못한 오류 발생: profileId={}", profileId, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "프로필별 스타일 목록 조회 중 오류가 발생했습니다.", e);
        }

        log.info("프로필별 스타일 목록 조회 응답 완료: profileId={}, 결과 수={}",
                profileId, styles.getNumberOfElements());
        return ResponseEntity.ok(styles);
    }

    /**
     * 스타일 미디어 파일 조회 API
     *
     * @param styleId 스타일 ID
     * @param fileName 파일명
     * @return 미디어 파일 바이트 배열
     * @throws IOException 파일 IO 예외 발생 시
     */
    @GetMapping("/{styleId}/media/{fileName}")
    public ResponseEntity<byte[]> getStyleMedia(
            @PathVariable("styleId") Long styleId,
            @PathVariable("fileName") String fileName
    ) throws IOException {
        log.info("스타일 미디어 파일 조회 요청: styleId={}, fileName={}", styleId, fileName);

        try {
            // /home/ubuntu/fream/styles/{styleId}/{fileName}
            String baseDir = "/home/ubuntu/fream";
//            String baseDir = "C:\\Users\\pickj\\webserver\\dockerVolums\\fream";
            String directory = "styles/" + styleId;
            String fullPath = baseDir + File.separator + directory + File.separator + fileName;
            log.debug("미디어 파일 전체 경로: {}", fullPath);

            File mediaFile = new File(fullPath);
            if (!mediaFile.exists()) {
                log.error("미디어 파일을 찾을 수 없음: styleId={}, fileName={}, fullPath={}",
                        styleId, fileName, fullPath);
                throw new StyleException(StyleErrorCode.MEDIA_FILE_NOT_FOUND,
                        "스타일 미디어 파일이 존재하지 않습니다.");
            }

            byte[] fileBytes = Files.readAllBytes(mediaFile.toPath());
            String mimeType = Files.probeContentType(mediaFile.toPath());
            if (mimeType == null) {
                log.warn("MIME 타입을 확인할 수 없음, 기본값 사용: styleId={}, fileName={}",
                        styleId, fileName);
                mimeType = "application/octet-stream";
            }
            log.debug("미디어 파일 읽기 성공: styleId={}, fileName={}, fileSize={}, mimeType={}",
                    styleId, fileName, fileBytes.length, mimeType);

            log.info("스타일 미디어 파일 조회 응답 완료: styleId={}, fileName={}", styleId, fileName);
            return ResponseEntity.ok()
                    .header("Content-Type", mimeType)
                    .body(fileBytes);
        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (IOException e) {
            log.error("미디어 파일 읽기 중 IO 예외 발생: styleId={}, fileName={}", styleId, fileName, e);
            throw new StyleException(StyleErrorCode.MEDIA_FILE_NOT_FOUND,
                    "미디어 파일을 읽는 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            log.error("미디어 파일 조회 중 예상치 못한 오류 발생: styleId={}, fileName={}",
                    styleId, fileName, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "미디어 파일 조회 중 오류가 발생했습니다.", e);
        }
    }
}