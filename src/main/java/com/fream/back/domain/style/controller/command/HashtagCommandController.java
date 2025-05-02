package com.fream.back.domain.style.controller.command;

import com.fream.back.domain.style.dto.HashtagCreateRequestDto;
import com.fream.back.domain.style.dto.HashtagResponseDto;
import com.fream.back.domain.style.dto.HashtagUpdateRequestDto;
import com.fream.back.domain.style.exception.StyleErrorCode;
import com.fream.back.domain.style.exception.StyleException;
import com.fream.back.domain.style.service.command.HashtagCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/hashtags/commands")
@RequiredArgsConstructor
public class HashtagCommandController {

    private final HashtagCommandService hashtagCommandService;

    /**
     * 해시태그 생성 API
     *
     * @param requestDto 해시태그 생성 요청 DTO
     * @return 생성된 해시태그 응답 DTO
     */
    @PostMapping
    public ResponseEntity<HashtagResponseDto> createHashtag(@RequestBody HashtagCreateRequestDto requestDto) {
        log.info("해시태그 생성 요청: name={}", requestDto.getName());

        try {
            // 요청 유효성 검사
            if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "해시태그 이름이 필요합니다.");
            }

            // 관리자 권한 확인 (필요한 경우)
            // SecurityUtils.checkAdminRole();

            // 해시태그 이름 정제
            String cleanName = requestDto.getName().trim();
            if (cleanName.startsWith("#")) {
                cleanName = cleanName.substring(1);
            }
            requestDto.setName(cleanName);

            HashtagResponseDto createdHashtag = hashtagCommandService.create(requestDto);
            log.info("해시태그 생성 완료: hashtagId={}, name={}", createdHashtag.getId(), createdHashtag.getName());

            return ResponseEntity.ok(createdHashtag);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 생성 중 예상치 못한 오류 발생: name={}", requestDto.getName(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 해시태그 수정 API
     *
     * @param id 수정할 해시태그 ID
     * @param requestDto 해시태그 수정 요청 DTO
     * @return 수정된 해시태그 응답 DTO
     */
    @PutMapping("/{id}")
    public ResponseEntity<HashtagResponseDto> updateHashtag(
            @PathVariable("id") Long id,
            @RequestBody HashtagUpdateRequestDto requestDto) {
        log.info("해시태그 수정 요청: hashtagId={}, newName={}", id, requestDto.getName());

        try {
            // 요청 유효성 검사
            if (requestDto.getName() == null || requestDto.getName().trim().isEmpty()) {
                throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST, "해시태그 이름이 필요합니다.");
            }

            // 관리자 권한 확인 (필요한 경우)
            // SecurityUtils.checkAdminRole();

            // 해시태그 이름 정제
            String cleanName = requestDto.getName().trim();
            if (cleanName.startsWith("#")) {
                cleanName = cleanName.substring(1);
            }
            requestDto.setName(cleanName);

            HashtagResponseDto updatedHashtag = hashtagCommandService.update(id, requestDto);
            log.info("해시태그 수정 완료: hashtagId={}, newName={}", id, updatedHashtag.getName());

            return ResponseEntity.ok(updatedHashtag);

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 수정 중 예상치 못한 오류 발생: hashtagId={}, newName={}",
                    id, requestDto.getName(), e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 해시태그 삭제 API
     *
     * @param id 삭제할 해시태그 ID
     * @return 빈 응답
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHashtag(@PathVariable("id") Long id) {
        log.info("해시태그 삭제 요청: hashtagId={}", id);

        try {
            // 관리자 권한 확인 (필요한 경우)
            // SecurityUtils.checkAdminRole();

            hashtagCommandService.delete(id);
            log.info("해시태그 삭제 완료: hashtagId={}", id);

            return ResponseEntity.ok().build();

        } catch (StyleException e) {
            // StyleException은 이미 로깅됨
            throw e;
        } catch (Exception e) {
            log.error("해시태그 삭제 중 예상치 못한 오류 발생: hashtagId={}", id, e);
            throw new StyleException(StyleErrorCode.STYLE_INVALID_REQUEST,
                    "해시태그 삭제 중 오류가 발생했습니다.", e);
        }
    }
}