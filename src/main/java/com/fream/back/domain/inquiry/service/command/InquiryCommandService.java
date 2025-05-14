package com.fream.back.domain.inquiry.service.command;

import com.fream.back.domain.inquiry.dto.InquiryAnswerRequestDto;
import com.fream.back.domain.inquiry.dto.InquiryCreateRequestDto;
import com.fream.back.domain.inquiry.dto.InquiryResponseDto;
import com.fream.back.domain.inquiry.dto.InquiryUpdateRequestDto;
import com.fream.back.domain.inquiry.entity.InquiryStatus;

/**
 * 1대1 문의 커맨드 서비스 인터페이스
 * 문의의 생성, 수정, 삭제, 답변 등 기능 정의
 */
public interface InquiryCommandService {

    /**
     * 1대1 문의 생성
     *
     * @param userId 사용자 ID
     * @param requestDto 문의 생성 요청 DTO
     * @return 생성된 문의 정보
     */
    InquiryResponseDto createInquiry(Long userId, InquiryCreateRequestDto requestDto);

    /**
     * 1대1 문의 수정
     *
     * @param inquiryId 문의 ID
     * @param userId 사용자 ID (본인 확인용)
     * @param requestDto 문의 수정 요청 DTO
     * @return 수정된 문의 정보
     */
    InquiryResponseDto updateInquiry(Long inquiryId, Long userId, InquiryUpdateRequestDto requestDto);

    /**
     * 1대1 문의 삭제
     *
     * @param inquiryId 문의 ID
     * @param userId 사용자 ID (본인 또는 관리자 확인용)
     * @param isAdmin 관리자 여부
     */
    void deleteInquiry(Long inquiryId, Long userId, boolean isAdmin);

    /**
     * 1대1 문의 상태 변경
     *
     * @param inquiryId 문의 ID
     * @param status 변경할 상태
     * @return 상태가 변경된 문의 정보
     */
    InquiryResponseDto updateInquiryStatus(Long inquiryId, InquiryStatus status);

    /**
     * 1대1 문의 답변 작성
     *
     * @param inquiryId 문의 ID
     * @param requestDto 답변 작성 요청 DTO
     * @return 답변이 작성된 문의 정보
     */
    InquiryResponseDto answerInquiry(Long inquiryId, InquiryAnswerRequestDto requestDto);
}