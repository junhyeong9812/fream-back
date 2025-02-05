package com.fream.back.domain.product.service.interest;

import com.fream.back.domain.product.entity.Interest;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.repository.InterestRepository;
import com.fream.back.domain.product.service.productColor.ProductColorQueryService;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InterestCommandService {

    private final InterestRepository interestRepository;
    private final UserQueryService userQueryService;
    private final ProductColorQueryService productColorQueryService;

    // 관심 상품 토글 (추가/삭제)
    public void toggleInterest(String userEmail, Long productColorId) {
        User user = userQueryService.findByEmail(userEmail); // 이메일로 유저 조회
        ProductColor productColor = productColorQueryService.findById(productColorId); // ProductColor 조회

        // 관심 상품이 이미 등록되어 있는지 확인
        interestRepository.findByUserAndProductColor(user, productColor).ifPresentOrElse(
                interest -> {
                    // 이미 등록된 경우 삭제
                    interest.unassignUser(); // 연관관계 해제
                    interest.unassignProductColor(); // 연관관계 해제
                    interestRepository.delete(interest);
                },
                () -> {
                    // 등록되지 않은 경우 추가
                    Interest newInterest = Interest.builder()
                            .user(user)
                            .productColor(productColor)
                            .build();
                    newInterest.assignUser(user); // 연관관계 설정
                    newInterest.assignProductColor(productColor); // 연관관계 설정
                    interestRepository.save(newInterest);
                }
        );
    }
    @Transactional
    public void deleteAllInterestsByProductColor(ProductColor productColor) {
        productColor.getInterests().forEach(interest -> {
            interest.unassignUser(); // 연관 관계 해제
            interest.unassignProductColor(); // 연관 관계 해제
            interestRepository.delete(interest); // 삭제
        });
        productColor.getInterests().clear();
    }
}