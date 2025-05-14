package com.fream.back.domain.inquiry.repository;

import com.fream.back.domain.inquiry.entity.InquiryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 1대1 문의 이미지 리포지토리
 */
public interface InquiryImageRepository extends JpaRepository<InquiryImage, Long> {

    // 특정 문의의 모든 이미지 조회
    List<InquiryImage> findAllByInquiry_Id(Long inquiryId);

    // 특정 문의의 질문 이미지만 조회
    List<InquiryImage> findAllByInquiry_IdAndIsAnswerFalse(Long inquiryId);

    // 특정 문의의 답변 이미지만 조회
    List<InquiryImage> findAllByInquiry_IdAndIsAnswerTrue(Long inquiryId);

    // 특정 문의의 모든 이미지 삭제
    void deleteAllByInquiry_Id(Long inquiryId);

    // 특정 문의의 이미지 개수 조회
    long countByInquiry_Id(Long inquiryId);

    // URL로 이미지 조회
    @Query("SELECT i FROM InquiryImage i WHERE i.inquiry.id = :inquiryId AND i.imageUrl LIKE %:imageUrl%")
    List<InquiryImage> findByInquiry_IdAndImageUrlContaining(@Param("inquiryId") Long inquiryId, @Param("imageUrl") String imageUrl);
}
