package com.fream.back.domain.product.service.interest;

import com.fream.back.domain.product.entity.Interest;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.exception.ProductException;
import com.fream.back.domain.product.exception.ProductErrorCode;
import com.fream.back.domain.product.repository.InterestRepository;
import com.fream.back.domain.product.service.productColor.ProductColorQueryService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관심 상품 명령(Command) 서비스
 * 관심 상품의 추가, 삭제 기능을 제공합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InterestCommandService {

    private final InterestRepository interestRepository;
    private final UserQueryService userQueryService;
    private final ProductColorQueryService productColorQueryService;

    /**
     * 관심 상품 토글 (추가/삭제)
     * 이미 등록된 관심 상품이면 삭제하고, 등록되지 않은 경우 추가합니다.
     *
     * @param userEmail 사용자 이메일
     * @param productColorId 상품 색상 ID
     * @throws ProductException 관심 상품 토글 실패 시
     */
    public void toggleInterest(String userEmail, Long productColorId) {
        log.info("관심 상품 토글 요청 - 사용자 이메일: {}, 상품 색상ID: {}", userEmail, productColorId);

        try {
            User user = userQueryService.findByEmail(userEmail); // 이메일로 유저 조회
            log.debug("사용자 조회 성공 - 사용자ID: {}, 이메일: {}", user.getId(), userEmail);

            ProductColor productColor = productColorQueryService.findById(productColorId); // ProductColor 조회
            log.debug("상품 색상 조회 성공 - 색상ID: {}, 색상명: {}", productColorId, productColor.getColorName());

            // 관심 상품이 이미 등록되어 있는지 확인
            interestRepository.findByUserAndProductColor(user, productColor).ifPresentOrElse(
                    interest -> {
                        // 이미 등록된 경우 삭제
                        log.debug("기존 관심 상품 삭제 - 관심ID: {}", interest.getId());
                        interest.unassignUser(); // 연관관계 해제
                        interest.unassignProductColor(); // 연관관계 해제
                        interestRepository.delete(interest);
                        log.info("관심 상품 삭제 성공 - 사용자ID: {}, 상품 색상ID: {}", user.getId(), productColorId);
                    },
                    () -> {
                        // 등록되지 않은 경우 추가
                        log.debug("새 관심 상품 추가");
                        Interest newInterest = Interest.builder()
                                .user(user)
                                .productColor(productColor)
                                .build();
                        newInterest.assignUser(user); // 연관관계 설정
                        newInterest.assignProductColor(productColor); // 연관관계 설정
                        Interest savedInterest = interestRepository.save(newInterest);
                        log.info("관심 상품 추가 성공 - 관심ID: {}, 사용자ID: {}, 상품 색상ID: {}",
                                savedInterest.getId(), user.getId(), productColorId);
                    }
            );
        } catch (IllegalArgumentException e) {
            log.error("관심 상품 토글 실패 - 사용자 이메일: {}, 상품 색상ID: {}, 오류: {}",
                    userEmail, productColorId, e.getMessage(), e);
            throw new ProductException(ProductErrorCode.INTEREST_TOGGLE_FAILED, e.getMessage(), e);
        } catch (Exception e) {
            log.error("관심 상품 토글 중 예상치 못한 오류 발생 - 사용자 이메일: {}, 상품 색상ID: {}",
                    userEmail, productColorId, e);
            throw new ProductException(ProductErrorCode.INTEREST_TOGGLE_FAILED,
                    "관심 상품 토글 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 상품 색상에 대한 모든 관심 상품 삭제
     *
     * @param productColor 상품 색상 엔티티
     * @throws ProductException 관심 상품 삭제 실패 시
     */
    @Transactional
    public void deleteAllInterestsByProductColor(ProductColor productColor) {
        log.info("상품 색상에 대한 모든 관심 상품 삭제 요청 - 색상ID: {}", productColor.getId());

        try {
            int count = productColor.getInterests().size();
            log.debug("삭제할 관심 상품 수: {}", count);

            productColor.getInterests().forEach(interest -> {
                log.debug("관심 상품 삭제 - 관심ID: {}, 사용자ID: {}",
                        interest.getId(), interest.getUser().getId());
                interest.unassignUser(); // 연관 관계 해제
                interest.unassignProductColor(); // 연관 관계 해제
                interestRepository.delete(interest); // 삭제
            });

            productColor.getInterests().clear();
            log.info("상품 색상에 대한 모든 관심 상품 삭제 성공 - 색상ID: {}, 삭제된 관심 상품 수: {}",
                    productColor.getId(), count);
        } catch (Exception e) {
            log.error("상품 색상에 대한 모든 관심 상품 삭제 중 예상치 못한 오류 발생 - 색상ID: {}",
                    productColor.getId(), e);
            throw new ProductException(ProductErrorCode.INTEREST_TOGGLE_FAILED,
                    "관심 상품 삭제 중 오류가 발생했습니다.", e);
        }
    }
}